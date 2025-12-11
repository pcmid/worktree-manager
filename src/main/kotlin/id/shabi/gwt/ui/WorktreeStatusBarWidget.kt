package id.shabi.gwt.ui

import id.shabi.gwt.models.WorktreeInfo
import id.shabi.gwt.services.WorktreeService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import java.awt.event.MouseEvent

/**
 * Status bar widget showing current worktree
 */
class WorktreeStatusBarWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.TextPresentation {

    companion object {
        const val ID = "WorktreeStatusBarWidget"
    }

    private val worktreeService = project.getService(WorktreeService::class.java)
    private var currentWorktree: WorktreeInfo? = null

    init {
        updateCurrentWorktree()
    }

    override fun ID(): String = ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getTooltipText(): String {
        return currentWorktree?.let {
            buildString {
                append("Worktree: ${it.displayName}")
                append("\nPath: ${it.path}")
                if (it.shortCommit != null) {
                    append("\nCommit: ${it.shortCommit}")
                }
                append("\n\nClick to switch worktree")
            }
        } ?: "No worktree"
    }

    override fun getText(): String {
        return currentWorktree?.let {
            when {
                it.isBare -> "bare"
                it.branch != null -> it.branch
                else -> "detached"
            }
        } ?: "N/A"
    }

    override fun getAlignment(): Float = 0f

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer { event ->
            showWorktreePopup(event)
        }
    }

    private fun showWorktreePopup(event: MouseEvent) {
        val popup = createWorktreePopup()
        val component = event.component
        popup.showUnderneathOf(component)
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
                myStatusBar?.updateWidget(ID())
            }
        }
    }

    override fun install(statusBar: StatusBar) {
        super.install(statusBar)
        updateCurrentWorktree()
    }
}
