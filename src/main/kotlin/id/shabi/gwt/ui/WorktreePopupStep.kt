package id.shabi.gwt.ui

import id.shabi.gwt.models.WorktreeInfo
import id.shabi.gwt.services.WorktreeService
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import java.nio.file.Paths
import javax.swing.Icon

/**
 * Popup step for selecting a worktree from the status bar widget
 */
class WorktreePopupStep(
    private val project: Project,
    private val worktreeService: WorktreeService,
    private val onWorktreeChanged: () -> Unit
) : BaseListPopupStep<WorktreeInfo>("Switch Worktree", worktreeService.listWorktrees()) {

    override fun getTextFor(value: WorktreeInfo): String {
        return value.displayName
    }

    override fun getIconFor(value: WorktreeInfo): Icon? {
        return when {
            value.isBare -> AllIcons.Nodes.Folder
            value.branch != null -> AllIcons.Vcs.Branch
            else -> AllIcons.Vcs.CommitNode
        }
    }

    override fun isSelectable(value: WorktreeInfo): Boolean {
        return !value.isBare
    }

    override fun onChosen(selectedValue: WorktreeInfo?, finalChoice: Boolean): PopupStep<*>? {
        if (selectedValue == null || selectedValue.isBare) {
            return PopupStep.FINAL_CHOICE
        }

        if (finalChoice) {
            switchToWorktree(selectedValue)
        }

        return PopupStep.FINAL_CHOICE
    }

    private fun switchToWorktree(worktree: WorktreeInfo) {
        // Check if project is already open and focus it
        val projectPath = Paths.get(worktree.path.toString())
        val existingProject = ProjectUtil.findAndFocusExistingProjectForPath(projectPath)
        if (existingProject != null) {
            // Project already open, focused to it
            onWorktreeChanged()
            return
        }

        // Project not open, ask how to open
        val choice = Messages.showDialog(
            project,
            "How do you want to open worktree '${worktree.displayName}'?",
            "Open Worktree",
            arrayOf("New Window", "Current Window", "Cancel"),
            0,
            Messages.getQuestionIcon()
        )

        when (choice) {
            0 -> openWorktreeInNewWindow(worktree)
            1 -> openWorktreeInCurrentWindow(worktree)
            else -> return
        }

        onWorktreeChanged()
    }

    private fun openWorktreeInNewWindow(worktree: WorktreeInfo) {
        val projectPath = Paths.get(worktree.path.toString())
        ApplicationManager.getApplication().executeOnPooledThread {
            ProjectUtil.openOrImport(projectPath, OpenProjectTask {
                forceOpenInNewFrame = true
                projectToClose = null
            })
        }
    }

    private fun openWorktreeInCurrentWindow(worktree: WorktreeInfo) {
        val projectPath = Paths.get(worktree.path.toString())
        ApplicationManager.getApplication().executeOnPooledThread {
            ProjectUtil.openOrImport(projectPath, OpenProjectTask {
                forceOpenInNewFrame = false
                projectToClose = project
            })
        }
    }
}
