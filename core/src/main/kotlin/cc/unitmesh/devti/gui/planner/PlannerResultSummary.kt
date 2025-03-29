package cc.unitmesh.devti.gui.planner

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JPanel

class PlannerResultSummary(
    private val project: Project,
    private var changes: List<Change>
) : JPanel(BorderLayout()) {
    private val changesPanel = JPanel(GridLayout(0, 1, 0, 5))
    private val statsLabel = JBLabel("No changes")

    init {
        background = JBUI.CurrentTheme.ToolWindow.background()
        border = JBUI.Borders.empty(10)

        val titlePanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(10)
            add(JBLabel("Change list").apply {
                foreground = UIUtil.getLabelForeground()
            }, BorderLayout.WEST)
            add(statsLabel, BorderLayout.EAST)
        }

        add(titlePanel, BorderLayout.NORTH)

        changesPanel.isOpaque = false
        add(JBScrollPane(changesPanel).apply {
            border = JBUI.Borders.empty()
            background = background
        }, BorderLayout.CENTER)

        updateChanges(changes.toMutableList())
    }

    fun updateChanges(changes: MutableList<Change>) {
        this.changes = changes
        changesPanel.removeAll()

        if (changes.isEmpty()) {
            statsLabel.text = "No changes"
            changesPanel.add(JBLabel("No code changes").apply {
                foreground = UIUtil.getLabelDisabledForeground()
            })
        } else {
            statsLabel.text = "total ${changes.size} files changed"

            changes.forEach { change ->
                val filePath = change.virtualFile?.path ?: "Unknown"
                val fileName = filePath.substringAfterLast('/')

                val changeType = when {
                    change.type == Change.Type.NEW -> "Add"
                    change.type == Change.Type.DELETED -> "Delete"
                    change.type == Change.Type.MOVED -> "Move"
                    else -> "Modify"
                }

                changesPanel.add(JBLabel("$changeType: $fileName").apply {
                    toolTipText = filePath
                })
            }
        }

        changesPanel.revalidate()
        changesPanel.repaint()
        
        isVisible = true
        revalidate()
        repaint()
    }
}
