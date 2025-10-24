package id.shabi.gwt.ui

import id.shabi.gwt.models.CreateWorktreeRequest
import id.shabi.gwt.services.WorktreeService
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBColor
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JComponent

/**
 * Dialog for creating a new Git worktree
 */
class CreateWorktreeDialog(private val project: Project) : DialogWrapper(project) {

    private val worktreeService = project.getService(WorktreeService::class.java)

    // Store all branches for auto-completion
    private var allBranches: List<String> = emptyList()

    // Branch text field with auto-completion
    private lateinit var branchTextField: TextFieldWithAutoCompletion<String>

    private val pathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Select Worktree Location")
                .withDescription("Choose a directory for the new worktree")
        )
    }

    // Error label for displaying validation errors
    private val errorLabel = JBLabel().apply {
        foreground = JBColor.RED
        isVisible = false
    }

    // Track whether user has manually modified the path
    private var isPathManuallySet = false

    // Timer for delayed path update
    private var updateTimer: javax.swing.Timer? = null

    init {
        title = "Create Git Worktree"
        // Load branches first
        allBranches = worktreeService.getBranches()
        // Create text field with auto-completion
        branchTextField = TextFieldWithAutoCompletion.create(
            project,
            allBranches,
            true,  // show auto-popup
            ""     // initial text
        )
        init()
        // Setup listeners after dialog is initialized
        setupListeners()
    }

    override fun createCenterPanel(): JComponent {
        val centerPanel = panel {
            row("Branch:") {
                cell(branchTextField)
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
            row("Path:") {
                cell(pathField)
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
            row {
                cell(errorLabel)
            }
            row {
                comment("Path will be auto-generated from branch name. If the branch doesn't exist, a new one will be created.")
            }
        }

        // Add mouse listener to panel for clicking blank areas
        centerPanel.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent?) {
                // When clicking on blank area, update path if branch has focus
                if (branchTextField.hasFocus() && !isPathManuallySet) {
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
        // Listen to branch text field focus lost - update path when user leaves the field
        branchTextField.addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusLost(e: java.awt.event.FocusEvent?) {
                if (!isPathManuallySet) {
                    cancelUpdateTimer()
                    updatePathFromBranch()
                }
            }
        })

        // Listen to text changes in branch input for delayed path update
        branchTextField.addDocumentListener(
            object : com.intellij.openapi.editor.event.DocumentListener {
                override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
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
        val branchName = branchTextField.text.trim()

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

    override fun doValidate(): ValidationInfo? {
        // Clear previous error
        errorLabel.isVisible = false
        errorLabel.text = ""

        val path = pathField.text.trim()
        if (path.isEmpty()) {
            val error = "Path cannot be empty"
            errorLabel.text = error
            errorLabel.isVisible = true
            return ValidationInfo(error, pathField)
        }

        val pathObj = Paths.get(path)
        if (Files.exists(pathObj)) {
            val error = "Path already exists"
            errorLabel.text = error
            errorLabel.isVisible = true
            return ValidationInfo(error, pathField)
        }

        val branch = branchTextField.text

        if (branch.isNullOrEmpty()) {
            val error = "Please enter a branch name"
            errorLabel.text = error
            errorLabel.isVisible = true
            return ValidationInfo(error, branchTextField)
        }

        val branchName = branch.trim()
        if (branchName.isEmpty()) {
            val error = "Branch name cannot be empty"
            errorLabel.text = error
            errorLabel.isVisible = true
            return ValidationInfo(error, branchTextField)
        }
        if (!branchName.matches(Regex("[a-zA-Z0-9/_-]+"))) {
            val error = "Invalid branch name"
            errorLabel.text = error
            errorLabel.isVisible = true
            return ValidationInfo(error, branchTextField)
        }

        // Check for branch name conflicts with existing branches
        val existingBranches = worktreeService.getBranches()
        val conflictError = checkBranchNameConflict(branchName, existingBranches)
        if (conflictError != null) {
            errorLabel.text = conflictError
            errorLabel.isVisible = true
            return ValidationInfo(conflictError, branchTextField)
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
        val branch = branchTextField.text.trim()

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
