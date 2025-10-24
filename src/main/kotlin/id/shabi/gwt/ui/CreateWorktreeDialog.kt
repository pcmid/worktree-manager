package id.shabi.gwt.ui

import id.shabi.gwt.models.CreateWorktreeRequest
import id.shabi.gwt.services.WorktreeService
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

/**
 * Dialog for creating a new Git worktree
 */
class CreateWorktreeDialog(private val project: Project) : DialogWrapper(project) {

    private val worktreeService = project.getService(WorktreeService::class.java)

    private val branchComboBox = ComboBox<String>().apply {
        isEditable = true
    }

    private val pathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Select Worktree Location")
                .withDescription("Choose a directory for the new worktree")
        )
    }

    // Track whether user has manually modified the path
    private var isPathManuallySet = false

    // Timer for delayed path update
    private var updateTimer: javax.swing.Timer? = null

    init {
        title = "Create Git Worktree"
        init()
        loadBranches()
        // Set initial path based on first branch
        updatePathFromBranch()
        // Setup listeners after dialog is initialized
        setupListeners()
    }

    override fun createCenterPanel(): JComponent {
        val centerPanel = panel {
            row("Branch:") {
                cell(branchComboBox)
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
            row("Path:") {
                cell(pathField)
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
            row {
                comment("Path will be auto-generated from branch name. If the branch doesn't exist, a new one will be created.")
            }
        }

        // Add mouse listener to panel for clicking blank areas
        centerPanel.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent?) {
                // When clicking on blank area, update path if branch has focus
                if (branchComboBox.editor.editorComponent.hasFocus() && !isPathManuallySet) {
                    cancelUpdateTimer()
                    updatePathFromBranch()
                }
                // Request focus for the panel to remove focus from branch input
                centerPanel.requestFocusInWindow()
            }
        })

        return centerPanel
    }

    private fun setupListeners() {
        // Listen to branch combo box focus lost - update path when user leaves the field
        branchComboBox.editor.editorComponent.addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusLost(e: java.awt.event.FocusEvent?) {
                if (!isPathManuallySet) {
                    cancelUpdateTimer()
                    updatePathFromBranch()
                }
            }
        })

        // Also listen to selection changes from dropdown
        branchComboBox.addActionListener {
            if (!isPathManuallySet) {
                cancelUpdateTimer()
                updatePathFromBranch()
            }
        }

        // Listen to text changes in branch input for delayed update
        val editorComponent = branchComboBox.editor?.editorComponent
        if (editorComponent is javax.swing.text.JTextComponent) {
            editorComponent.document.addDocumentListener(
                object : javax.swing.event.DocumentListener {
                    override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = onTextChanged()
                    override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = onTextChanged()
                    override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = onTextChanged()

                    private fun onTextChanged() {
                        // Trigger validation immediately
                        initValidation()

                        // Schedule path update
                        if (!isPathManuallySet) {
                            // Cancel existing timer
                            cancelUpdateTimer()
                            // Schedule new update after 500ms
                            updateTimer = javax.swing.Timer(500) {
                                updatePathFromBranch()
                            }.apply {
                                isRepeats = false
                                start()
                            }
                        }
                    }
                }
            )
        }

        // Track manual path changes and trigger validation
        pathField.textField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = onPathChanged()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = onPathChanged()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = onPathChanged()

            private fun onPathChanged() {
                // Trigger validation immediately
                initValidation()

                // Only mark as manual if user typed in the field
                if (pathField.textField.hasFocus()) {
                    isPathManuallySet = true
                }
            }
        })
    }

    private fun cancelUpdateTimer() {
        updateTimer?.stop()
        updateTimer = null
    }

    private fun updatePathFromBranch() {
        // Get branch name from editor component (for real-time text) or selectedItem (for dropdown selection)
        val editorComponent = branchComboBox.editor?.editorComponent
        val branchName = if (editorComponent is javax.swing.text.JTextComponent) {
            editorComponent.text?.trim() ?: ""
        } else {
            (branchComboBox.selectedItem as? String)?.trim() ?: ""
        }

        if (branchName.isNotEmpty()) {
            // Get project base path and create sibling directory
            val basePath = project.basePath ?: return
            val parentPath = Paths.get(basePath).parent ?: return

            // Use branch name directly as directory name (preserves slashes for subdirectories)
            val dirName = branchName
            val newPath = parentPath.resolve(dirName).toString()

            // Update path without triggering manual flag
            pathField.text = newPath
        }
    }

    private fun loadBranches() {
        val branches = worktreeService.getBranches()
        val model = DefaultComboBoxModel<String>()
        branches.forEach { model.addElement(it) }
        branchComboBox.model = model
        if (model.size > 0) {
            branchComboBox.selectedIndex = 0
        }
    }

    override fun doValidate(): ValidationInfo? {
        val path = pathField.text.trim()
        if (path.isEmpty()) {
            return ValidationInfo("Path cannot be empty", pathField)
        }

        val pathObj = Paths.get(path)
        if (Files.exists(pathObj)) {
            return ValidationInfo("Path already exists", pathField)
        }

        // Get branch name from editor component (for real-time text) or selectedItem (for dropdown selection)
        val editorComponent = branchComboBox.editor?.editorComponent
        val branch = if (editorComponent is javax.swing.text.JTextComponent) {
            editorComponent.text
        } else {
            branchComboBox.selectedItem as? String
        }

        if (branch.isNullOrEmpty()) {
            return ValidationInfo("Please enter a branch name", branchComboBox)
        }

        val branchName = branch.trim()
        if (branchName.isEmpty()) {
            return ValidationInfo("Branch name cannot be empty", branchComboBox)
        }
        if (!branchName.matches(Regex("[a-zA-Z0-9/_-]+"))) {
            return ValidationInfo("Invalid branch name", branchComboBox)
        }

        // Check for branch name conflicts with existing branches
        val existingBranches = worktreeService.getBranches()
        val conflictError = checkBranchNameConflict(branchName, existingBranches)
        if (conflictError != null) {
            return ValidationInfo(conflictError, branchComboBox)
        }

        return null
    }

    private fun checkBranchNameConflict(newBranch: String, existingBranches: List<String>): String? {
        // Check if new branch conflicts with existing branches
        for (existing in existingBranches) {
            // Case 1: New branch is a prefix of existing branch
            // e.g., trying to create "test" when "test/unit" exists
            if (existing.startsWith("$newBranch/")) {
                return "Cannot create branch '$newBranch' because branch '$existing' already exists"
            }

            // Case 2: Existing branch is a prefix of new branch
            // e.g., trying to create "test/unit" when "test" exists
            if (newBranch.startsWith("$existing/")) {
                return "Cannot create branch '$newBranch' because branch '$existing' already exists"
            }
        }

        return null
    }

    fun getRequest(): CreateWorktreeRequest {
        val path = Paths.get(pathField.text.trim())

        // Get branch name from editor component (for real-time text) or selectedItem (for dropdown selection)
        val editorComponent = branchComboBox.editor?.editorComponent
        val branch = if (editorComponent is javax.swing.text.JTextComponent) {
            editorComponent.text?.trim() ?: ""
        } else {
            (branchComboBox.selectedItem as? String)?.trim() ?: ""
        }

        // Check if branch exists in the list (which means it's an existing branch)
        val existingBranches = worktreeService.getBranches()
        val isNewBranch = !existingBranches.contains(branch)

        return CreateWorktreeRequest(
            path = path,
            branch = branch,
            createNewBranch = isNewBranch,
            checkout = true,
            force = false
        )
    }
}
