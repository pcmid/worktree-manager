package id.shabi.gwt.actions

import id.shabi.gwt.services.WorktreeService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBList
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import java.nio.file.Paths
import javax.swing.DefaultListModel

/**
 * Action to switch to another Git worktree
 */
class SwitchWorktreeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val worktreeService = project.getService(WorktreeService::class.java)

        val worktrees = worktreeService.listWorktrees()
            .filter { !it.isBare } // Can't switch to bare repository

        if (worktrees.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No worktrees available to switch to",
                "Switch Worktree"
            )
            return
        }

        // Show selection dialog
        val listModel = DefaultListModel<String>()
        worktrees.forEach { listModel.addElement(it.displayName) }
        val list = JBList(listModel)

        // Pre-select current worktree
        val current = worktreeService.getCurrentWorktree()
        if (current != null) {
            val currentIndex = worktrees.indexOf(current)
            if (currentIndex >= 0) {
                list.selectedIndex = currentIndex
            }
        }

        val choice = Messages.showDialog(
            project,
            "Select Worktree to Switch To",
            "Select Worktree",
            arrayOf("OK", "Cancel"),
            0,
            Messages.getQuestionIcon(),
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

        // Check if it's the current worktree
        if (selectedWorktree == current) {
            Messages.showInfoMessage(
                project,
                "Already in worktree '${selectedWorktree.displayName}'",
                "Switch Worktree"
            )
            return
        }

        // Check if project is already open and focus it
        val projectPath = Paths.get(selectedWorktree.path.toString())
        val existingProject = ProjectUtil.findAndFocusExistingProjectForPath(projectPath)
        if (existingProject != null) {
            // Project already open, focused to it
            return
        }

        // Project not open, ask how to open
        val openChoice = Messages.showDialog(
            project,
            "How do you want to open worktree '${selectedWorktree.displayName}'?",
            "Open Worktree",
            arrayOf("New Window", "Current Window", "Cancel"),
            0,
            Messages.getQuestionIcon()
        )

        when (openChoice) {
            0 -> {
                ApplicationManager.getApplication().executeOnPooledThread {
                    ProjectUtil.openOrImport(projectPath, OpenProjectTask {
                        forceOpenInNewFrame = true
                        projectToClose = null
                    })
                }
            }
            1 -> {
                ApplicationManager.getApplication().executeOnPooledThread {
                    ProjectUtil.openOrImport(projectPath, OpenProjectTask {
                        forceOpenInNewFrame = false
                        projectToClose = project
                    })
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
