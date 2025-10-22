package id.shabi.gwt.ui

import id.shabi.gwt.models.WorktreeInfo
import id.shabi.gwt.services.WorktreeService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.event.MouseEvent

/**
 * Status bar widget showing current worktree
 */
class WorktreeStatusBarWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.MultipleTextValuesPresentation {

    private val worktreeService = project.getService(WorktreeService::class.java)
    private var currentWorktree: WorktreeInfo? = null

    init {
        updateCurrentWorktree()
    }

    override fun ID(): String = "WorktreeStatusBarWidget"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getTooltipText(): String {
        return currentWorktree?.let {
            buildString {
                append("Worktree: ${it.displayName}")
                append("\nPath: ${it.path}")
                if (it.shortCommit != null) {
                    append("\nCommit: ${it.shortCommit}")
                }
            }
        } ?: "No worktree"
    }

    override fun getSelectedValue(): String {
        return currentWorktree?.let {
            when {
                it.isBare -> "bare"
                it.branch != null -> it.branch
                else -> "detached"
            }
        } ?: "N/A"
    }

    override fun getClickConsumer(): Consumer<MouseEvent>? {
        return Consumer { event ->
            showWorktreePopup(event.component, event)
        }
    }

    private fun showWorktreePopup(component: Component, event: MouseEvent) {
        val popup = createWorktreePopup()
        popup.show(RelativePoint(component, event.point))
    }

    private fun createWorktreePopup(): ListPopup {
        return JBPopupFactory.getInstance().createListPopup(
            WorktreePopupStep(project, worktreeService) {
                updateCurrentWorktree()
            }
        )
    }

    private fun updateCurrentWorktree() {
        ApplicationManager.getApplication().executeOnPooledThread {
            currentWorktree = worktreeService.getCurrentWorktree()
            ApplicationManager.getApplication().invokeLater {
                val statusBar = myStatusBar
                if (statusBar != null) {
                    statusBar.updateWidget(ID())
                }
            }
        }
    }

    override fun dispose() {
        super.dispose()
        Disposer.dispose(this)
    }
}
