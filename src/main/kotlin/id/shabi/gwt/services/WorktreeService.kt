package id.shabi.gwt.services

import id.shabi.gwt.models.CreateWorktreeRequest
import id.shabi.gwt.models.WorktreeInfo
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Service for managing Git worktrees
 */
@Service(Service.Level.PROJECT)
class WorktreeService(private val project: Project) {

    private val log = logger<WorktreeService>()

    /**
     * Lists all worktrees in the repository
     */
    fun listWorktrees(): List<WorktreeInfo> {
        val repoRoot = project.basePath ?: return emptyList()

        val output = executeGitCommand(repoRoot, "worktree", "list", "--porcelain")
        if (!output.isSuccess) {
            log.warn("Failed to list worktrees: ${output.stderr}")
            return emptyList()
        }

        return parseWorktreeList(output.stdout)
    }

    /**
     * Creates a new worktree
     */
    fun createWorktree(request: CreateWorktreeRequest): Result<WorktreeInfo> {
        val repoRoot = project.basePath ?: return Result.failure(
            IllegalStateException("No project path found")
        )

        val args = buildList {
            add("worktree")
            add("add")

            if (request.createNewBranch) {
                add("-b")
                add(request.branch)

                // If remote branch is specified, create local branch tracking remote
                if (request.remoteBranch != null) {
                    add("--track")
                }
            }

            if (request.force) {
                add("--force")
            }

            add(request.path.toString())

            if (!request.createNewBranch) {
                add(request.branch)
            } else if (request.remoteBranch != null) {
                // Specify the remote branch as the base for the new local branch
                add(request.remoteBranch)
            }
        }

        val output = executeGitCommand(repoRoot, *args.toTypedArray())
        if (!output.isSuccess) {
            return Result.failure(RuntimeException(output.stderr))
        }

        // Refresh VFS to detect new worktree
        VirtualFileManager.getInstance().asyncRefresh(null)

        // Re-list to get the created worktree info
        val worktrees = listWorktrees()
        val created = worktrees.find { it.path == request.path }
            ?: return Result.failure(RuntimeException("Worktree created but not found in list"))

        return Result.success(created)
    }

    /**
     * Deletes a worktree
     */
    fun deleteWorktree(worktree: WorktreeInfo, force: Boolean = false): Result<Unit> {
        val repoRoot = project.basePath ?: return Result.failure(
            IllegalStateException("No project path found")
        )

        val args = buildList {
            add("worktree")
            add("remove")
            if (force) {
                add("--force")
            }
            add(worktree.path.toString())
        }

        val output = executeGitCommand(repoRoot, *args.toTypedArray())
        if (!output.isSuccess) {
            return Result.failure(RuntimeException(output.stderr))
        }

        // Refresh VFS
        VirtualFileManager.getInstance().asyncRefresh(null)

        return Result.success(Unit)
    }

    /**
     * Gets the current worktree (based on current project path)
     */
    fun getCurrentWorktree(): WorktreeInfo? {
        val currentPath = Paths.get(project.basePath ?: return null)
        return listWorktrees().find { it.path == currentPath }
    }

    /**
     * Prunes worktree administrative files
     */
    fun pruneWorktrees(): Result<Unit> {
        val repoRoot = project.basePath ?: return Result.failure(
            IllegalStateException("No project path found")
        )

        val output = executeGitCommand(repoRoot, "worktree", "prune")
        if (!output.isSuccess) {
            return Result.failure(RuntimeException(output.stderr))
        }

        return Result.success(Unit)
    }

    /**
     * Gets local branches only
     */
    fun getBranches(): List<String> {
        val repoRoot = project.basePath ?: return emptyList()

        val output = executeGitCommand(repoRoot, "branch", "--format=%(refname:short)")
        if (!output.isSuccess) {
            log.warn("Failed to list branches: ${output.stderr}")
            return emptyList()
        }

        return output.stdout.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Gets remote branches only
     */
    fun getRemoteBranches(): List<String> {
        val repoRoot = project.basePath ?: return emptyList()

        val output = executeGitCommand(repoRoot, "branch", "-r", "--format=%(refname:short)")
        if (!output.isSuccess) {
            log.warn("Failed to list remote branches: ${output.stderr}")
            return emptyList()
        }

        return output.stdout.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.contains("HEAD") }
    }

    /**
     * Gets all branches (local + remote) for auto-completion
     */
    fun getAllBranches(): List<String> {
        return getBranches() + getRemoteBranches()
    }

    private fun executeGitCommand(workingDir: String, vararg args: String): ProcessOutput {
        val commandLine = GeneralCommandLine("git")
            .withParameters(*args)
            .withWorkDirectory(workingDir)
            .withCharset(Charsets.UTF_8)

        return try {
            ExecUtil.execAndGetOutput(commandLine, 30000)
        } catch (e: Exception) {
            log.error("Failed to execute git command: ${args.joinToString(" ")}", e)
            ProcessOutput("", e.message ?: "Unknown error", -1, false, false)
        }
    }

    private fun parseWorktreeList(output: String): List<WorktreeInfo> {
        val worktrees = mutableListOf<WorktreeInfo>()
        val lines = output.lines().filter { it.isNotEmpty() }

        var currentPath: Path? = null
        var currentBranch: String? = null
        var currentCommit: String? = null
        var isBare = false
        var isLocked = false
        var isPrunable = false

        for (line in lines) {
            when {
                line.startsWith("worktree ") -> {
                    // Save previous worktree if exists
                    currentPath?.let { path ->
                        worktrees.add(
                            WorktreeInfo(
                                path = path,
                                branch = currentBranch,
                                commit = currentCommit,
                                isBare = isBare,
                                isLocked = isLocked,
                                isPrunable = isPrunable
                            )
                        )
                    }

                    // Start new worktree
                    currentPath = Paths.get(line.substring(9))
                    currentBranch = null
                    currentCommit = null
                    isBare = false
                    isLocked = false
                    isPrunable = false
                }
                line.startsWith("HEAD ") -> {
                    currentCommit = line.substring(5)
                }
                line.startsWith("branch ") -> {
                    currentBranch = line.substring(7).substringAfterLast('/')
                }
                line == "bare" -> {
                    isBare = true
                }
                line.startsWith("locked") -> {
                    isLocked = true
                }
                line.startsWith("prunable") -> {
                    isPrunable = true
                }
            }
        }

        // Add last worktree
        currentPath?.let { path ->
            worktrees.add(
                WorktreeInfo(
                    path = path,
                    branch = currentBranch,
                    commit = currentCommit,
                    isBare = isBare,
                    isLocked = isLocked,
                    isPrunable = isPrunable
                )
            )
        }

        return worktrees
    }

    private val ProcessOutput.isSuccess: Boolean
        get() = exitCode == 0
}
