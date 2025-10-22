package id.shabi.gwt.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for creating the Worktree tool window
 */
class WorktreeToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val worktreeToolWindow = WorktreeToolWindow(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(worktreeToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
