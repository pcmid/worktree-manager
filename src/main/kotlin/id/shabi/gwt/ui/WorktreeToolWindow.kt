package id.shabi.gwt.ui

import id.shabi.gwt.models.OpenProjectOption
import id.shabi.gwt.models.WorktreeInfo
import id.shabi.gwt.services.WorktreeService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Paths
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.border.EmptyBorder

/**
 * Tool window for managing Git worktrees
 */
class WorktreeToolWindow(private val project: Project) {

    private val worktreeService = project.getService(WorktreeService::class.java)
    private val listModel = DefaultListModel<WorktreeInfo>()
    private val worktreeList = JBList(listModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = WorktreeListCellRenderer()
        border = EmptyBorder(5, 5, 5, 5)  // Add padding: top, left, bottom, right
    }

    private val panel = SimpleToolWindowPanel(true, true)

    init {
        setupUI()
        setupListeners()
        refreshWorktrees()
    }

    fun getContent(): JComponent = panel

    private fun setupUI() {
        val scrollPane = JBScrollPane(worktreeList)
        panel.setContent(scrollPane)

        // Create toolbar
        val actionGroup = DefaultActionGroup().apply {
            add(RefreshAction())
            add(CreateWorktreeAction())
            add(DeleteWorktreeAction())
            addSeparator()
            add(SwitchWorktreeAction())
        }

        val toolbar = ActionManager.getInstance()
            .createActionToolbar("WorktreeToolbar", actionGroup, true)
        toolbar.targetComponent = panel
        panel.toolbar = toolbar.component
    }

    private fun setupListeners() {
        worktreeList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selected = worktreeList.selectedValue
                    if (selected != null && !selected.isBare) {
                        switchToWorktree(selected)
                    }
                }
            }
        })
    }

    fun refreshWorktrees() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val worktrees = worktreeService.listWorktrees()
            ApplicationManager.getApplication().invokeLater {
                listModel.clear()
                worktrees.forEach { listModel.addElement(it) }

                // Select current worktree
                val current = worktreeService.getCurrentWorktree()
                if (current != null) {
                    val index = listModel.indexOf(current)
                    if (index >= 0) {
                        worktreeList.selectedIndex = index
                    }
                }
            }
        }
    }

    private fun switchToWorktree(worktree: WorktreeInfo) {
        // Check if project is already open and focus it
        val projectPath = Paths.get(worktree.path.toString())
        val existingProject = ProjectUtil.findAndFocusExistingProjectForPath(projectPath)
        if (existingProject != null) {
            // Project already open, focused to it
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

    // Toolbar Actions
    private inner class RefreshAction : AnAction("Refresh", "Refresh worktree list", AllIcons.Actions.Refresh) {
        override fun actionPerformed(e: AnActionEvent) {
            refreshWorktrees()
        }
    }

    private inner class CreateWorktreeAction : AnAction("Create", "Create new worktree", AllIcons.General.Add) {
        override fun actionPerformed(e: AnActionEvent) {
            val dialog = CreateWorktreeDialog(project)
            if (dialog.showAndGet()) {
                val request = dialog.getRequest()
                ApplicationManager.getApplication().executeOnPooledThread {
                    worktreeService.createWorktree(request)
                        .onSuccess { created ->
                            ApplicationManager.getApplication().invokeLater {
                                refreshWorktrees()
                                Messages.showInfoMessage(
                                    project,
                                    "Worktree '${created.displayName}' created successfully",
                                    "Create Worktree"
                                )

                                // Ask if user wants to open it
                                val openChoice = Messages.showYesNoDialog(
                                    project,
                                    "Do you want to open the new worktree?",
                                    "Open Worktree",
                                    Messages.getQuestionIcon()
                                )
                                if (openChoice == Messages.YES) {
                                    switchToWorktree(created)
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
    }

    private inner class DeleteWorktreeAction : AnAction("Delete", "Delete selected worktree", AllIcons.General.Remove) {
        override fun actionPerformed(e: AnActionEvent) {
            val selected = worktreeList.selectedValue ?: return
            if (selected.isBare) {
                Messages.showWarningDialog(
                    project,
                    "Cannot delete the bare repository",
                    "Delete Worktree"
                )
                return
            }

            val choice = Messages.showYesNoDialog(
                project,
                "Are you sure you want to delete worktree '${selected.displayName}'?",
                "Delete Worktree",
                Messages.getWarningIcon()
            )

            if (choice == Messages.YES) {
                ApplicationManager.getApplication().executeOnPooledThread {
                    worktreeService.deleteWorktree(selected)
                        .onSuccess {
                            ApplicationManager.getApplication().invokeLater {
                                refreshWorktrees()
                                Messages.showInfoMessage(
                                    project,
                                    "Worktree deleted successfully",
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
            e.presentation.isEnabled = worktreeList.selectedValue != null
        }
    }

    private inner class SwitchWorktreeAction : AnAction("Switch", "Switch to selected worktree", AllIcons.Vcs.Branch) {
        override fun actionPerformed(e: AnActionEvent) {
            val selected = worktreeList.selectedValue ?: return
            if (!selected.isBare) {
                switchToWorktree(selected)
            }
        }

        override fun update(e: AnActionEvent) {
            val selected = worktreeList.selectedValue
            e.presentation.isEnabled = selected != null && !selected.isBare
        }
    }
}
