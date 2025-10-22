package id.shabi.gwt.models

import java.nio.file.Path

/**
 * Represents a Git worktree with its associated information
 */
data class WorktreeInfo(
    val path: Path,
    val branch: String?,
    val commit: String?,
    val isBare: Boolean = false,
    val isLocked: Boolean = false,
    val isPrunable: Boolean = false
) {
    /**
     * Returns the display name for this worktree
     */
    val displayName: String
        get() = when {
            isBare -> "${path.fileName} (bare)"
            branch != null -> "$branch - ${path.fileName}"
            else -> "${path.fileName} (detached)"
        }

    /**
     * Returns whether this is the main/bare repository
     */
    val isMain: Boolean
        get() = isBare

    /**
     * Returns a short commit hash
     */
    val shortCommit: String?
        get() = commit?.take(8)

    override fun toString(): String {
        return displayName
    }
}

/**
 * Request to create a new worktree
 */
data class CreateWorktreeRequest(
    val path: Path,
    val branch: String,
    val createNewBranch: Boolean = false,
    val checkout: Boolean = true,
    val force: Boolean = false
)

/**
 * Options for opening a worktree project
 */
enum class OpenProjectOption {
    /**
     * Open in a new window
     */
    NEW_WINDOW,

    /**
     * Open in current window, replacing the current project
     */
    CURRENT_WINDOW,

    /**
     * Ask the user each time
     */
    ASK
}
