package id.shabi.gwt.actions

import id.shabi.gwt.services.WorktreeService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBList
import javax.swing.DefaultListModel

/**
 * Action to delete a Git worktree
 */
class DeleteWorktreeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val worktreeService = project.getService(WorktreeService::class.java)

        val worktrees = worktreeService.listWorktrees()
            .filter { !it.isBare } // Can't delete bare repository

        if (worktrees.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No worktrees available to delete",
                "Delete Worktree"
            )
            return
        }

        // Show selection dialog
        val listModel = DefaultListModel<String>()
        worktrees.forEach { listModel.addElement(it.displayName) }
        val list = JBList(listModel)

        val choice = Messages.showDialog(
            project,
            "Select Worktree to Delete",
            "Delete Worktree",
            arrayOf("OK", "Cancel"),
            0,
            Messages.getWarningIcon(),
            null
        )

        if (choice != 0) { // OK button
            return
        }

        val selectedIndex = list.selectedIndex
        if (selectedIndex < 0) {
            return
        }

        val selectedWorktree = worktrees[selectedIndex]

        val confirmChoice = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete worktree '${selectedWorktree.displayName}'?\n\n" +
                    "Path: ${selectedWorktree.path}\n" +
                    "Branch: ${selectedWorktree.branch ?: "N/A"}\n\n" +
                    "This action cannot be undone.",
            "Confirm Delete Worktree",
            Messages.getWarningIcon()
        )

        if (confirmChoice == Messages.YES) {
            ApplicationManager.getApplication().executeOnPooledThread {
                worktreeService.deleteWorktree(selectedWorktree)
                    .onSuccess {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showInfoMessage(
                                project,
                                "Worktree '${selectedWorktree.displayName}' deleted successfully",
                                "Delete Worktree"
                            )
                        }
                    }
                    .onFailure { error ->
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "Failed to delete worktree: ${error.message}",
                                "Delete Worktree Error"
                            )
                        }
                    }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
