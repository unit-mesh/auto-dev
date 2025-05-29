package cc.unitmesh.devti.gui.toolbar

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.mcp.ui.McpConfigPopup
import cc.unitmesh.devti.mcp.ui.McpConfigService
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import javax.swing.JButton
import javax.swing.JComponent

class McpConfigAction : AnAction("MCP Config", "Configure MCP tools", AutoDevIcons.MCP),
    CustomComponentAction {
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        showConfigPopup(e.inputEvent?.component as? JComponent, project)
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button: JButton = object : JButton() {
            init {
                putClientProperty("ActionToolbar.smallVariant", true)
                putClientProperty("customButtonInsets", JBInsets(1, 1, 1, 1).asUIResource())
                
                icon = AutoDevIcons.MCP
                toolTipText = "Configure MCP Tools"
                setOpaque(false)
                
                addActionListener {
                    val project = ActionToolbar.getDataContextFor(this).getData(CommonDataKeys.PROJECT)
                    if (project != null) {
                        showConfigPopup(this, project)
                    }
                }
            }
        }

        return Wrapper(button).also {
            it.setBorder(JBUI.Borders.empty(0, 10))
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null
        
        if (project != null) {
            val configService = project.getService(McpConfigService::class.java)
            val selectedCount = configService.getSelectedToolsCount()
            
            e.presentation.text = if (selectedCount > 0) {
                "MCP Config ($selectedCount tools selected)"
            } else {
                "MCP Config"
            }
        }
    }
    
    private fun showConfigPopup(component: JComponent?, project: Project) {
        McpConfigPopup.show(component, project)
    }
}
