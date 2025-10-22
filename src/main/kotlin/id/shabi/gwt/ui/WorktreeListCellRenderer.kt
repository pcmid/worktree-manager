package id.shabi.gwt.ui

import id.shabi.gwt.models.WorktreeInfo
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import java.awt.Component
import javax.swing.*

/**
 * Custom cell renderer for worktree list
 */
class WorktreeListCellRenderer : ListCellRenderer<WorktreeInfo> {

    private val label = JBLabel()

    override fun getListCellRendererComponent(
        list: JList<out WorktreeInfo>,
        value: WorktreeInfo,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        label.text = buildString {
            append(value.displayName)
            if (value.shortCommit != null) {
                append(" (${value.shortCommit})")
            }
            if (value.isLocked) {
                append(" [Locked]")
            }
        }

        label.icon = when {
            value.isBare -> AllIcons.Nodes.Folder
            value.branch != null -> AllIcons.Vcs.Branch
            else -> AllIcons.Vcs.CommitNode
        }

        if (isSelected) {
            label.background = list.selectionBackground
            label.foreground = list.selectionForeground
            label.isOpaque = true
        } else {
            label.background = list.background
            label.foreground = list.foreground
            label.isOpaque = false
        }

        return label
    }
}
