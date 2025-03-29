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
    private val statsLabel = JBLabel("暂无变更")
    
    init {
        background = JBUI.CurrentTheme.ToolWindow.background()
        border = JBUI.Borders.empty(10)
        
        val titlePanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(10)
            add(JBLabel("执行结果").apply { 
                font = font.deriveFont(font.size + 2f)
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
            statsLabel.text = "暂无变更"
            changesPanel.add(JBLabel("暂无代码变更").apply {
                foreground = UIUtil.getLabelDisabledForeground()
            })
        } else {
            statsLabel.text = "共 ${changes.size} 个文件变更"
            
            // 简单显示变更的文件
            changes.forEach { change ->
                val filePath = change.virtualFile?.path ?: "未知文件"
                val fileName = filePath.substringAfterLast('/')
                
                val changeType = when {
                    change.type == Change.Type.NEW -> "新增"
                    change.type == Change.Type.DELETED -> "删除"
                    change.type == Change.Type.MOVED -> "移动"
                    else -> "修改"
                }
                
                changesPanel.add(JBLabel("$changeType: $fileName").apply {
                    toolTipText = filePath
                })
            }
        }
        
        revalidate()
        repaint()
    }
}
