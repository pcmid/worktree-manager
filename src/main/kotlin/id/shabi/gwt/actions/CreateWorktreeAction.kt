package id.shabi.gwt.actions

import id.shabi.gwt.services.WorktreeService
import id.shabi.gwt.ui.CreateWorktreeDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import java.nio.file.Paths

/**
 * Action to create a new Git worktree
 */
class CreateWorktreeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val worktreeService = project.getService(WorktreeService::class.java)

        val dialog = CreateWorktreeDialog(project)
        if (dialog.showAndGet()) {
            val request = dialog.getRequest()

            ApplicationManager.getApplication().executeOnPooledThread {
                worktreeService.createWorktree(request)
                    .onSuccess { created ->
                        ApplicationManager.getApplication().invokeLater {
                            // Refresh tool window if open
                            val toolWindow = ToolWindowManager.getInstance(project)
                                .getToolWindow("Git Worktrees")
                            toolWindow?.contentManager?.contents?.firstOrNull()?.component?.let {
                                // Trigger refresh - in real implementation would use proper event system
                            }

                            Messages.showInfoMessage(
                                project,
                                "Worktree '${created.displayName}' created successfully at ${created.path}",
                                "Create Worktree"
                            )

                            // Check if project is already open and focus it
                            val projectPath = Paths.get(created.path.toString())
                            val existingProject = ProjectUtil.findAndFocusExistingProjectForPath(projectPath)
                            if (existingProject != null) {
                                // Project already open, focused to it
                                return@invokeLater
                            }

                            // Project not open, ask if user wants to open it
                            val openChoice = Messages.showDialog(
                                project,
                                "Do you want to open the new worktree?",
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
                    }
                    .onFailure { error ->
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "Failed to create worktree: ${error.message}",
                                "Create Worktree Error"
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
