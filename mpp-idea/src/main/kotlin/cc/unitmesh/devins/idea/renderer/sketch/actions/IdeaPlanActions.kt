package cc.unitmesh.devins.idea.renderer.sketch.actions

import cc.unitmesh.devti.gui.AutoDevPlannerToolWindowFactory
import cc.unitmesh.devti.gui.planner.AutoDevPlannerToolWindow
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Business logic actions for Plan operations in mpp-idea.
 * Reuses core module's PlanToolbarFactory, MarkdownPlanParser, AgentStateService logic.
 * 
 * Related GitHub Issue: https://github.com/phodal/auto-dev/issues/25
 */
object IdeaPlanActions {
    
    /**
     * Parse plan content to AgentTaskEntry list
     */
    fun parsePlan(content: String): List<AgentTaskEntry> {
        return MarkdownPlanParser.parse(content)
    }
    
    /**
     * Format plan entries back to markdown
     */
    fun formatPlanToMarkdown(entries: List<AgentTaskEntry>): String {
        return MarkdownPlanParser.formatPlanToMarkdown(entries.toMutableList())
    }
    
    /**
     * Copy plan to clipboard
     */
    fun copyPlanToClipboard(project: Project): Boolean {
        return try {
            val agentStateService = project.getService(AgentStateService::class.java)
            val currentPlan = agentStateService.getPlan()
            val planString = formatPlanToMarkdown(currentPlan)
            
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val selection = StringSelection(planString)
            clipboard.setContents(selection, null)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Copy specific plan content to clipboard
     */
    fun copyToClipboard(content: String): Boolean {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val selection = StringSelection(content)
            clipboard.setContents(selection, null)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Pin plan to the Planner tool window
     */
    fun pinToToolWindow(project: Project, planContent: String? = null) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(AutoDevPlannerToolWindowFactory.PlANNER_ID)
            ?: return
        
        val codingPanel = toolWindow.contentManager.component.components
            ?.filterIsInstance<AutoDevPlannerToolWindow>()
            ?.firstOrNull()
        
        toolWindow.activate {
            val content = if (planContent != null) {
                planContent
            } else {
                val agentStateService = project.getService(AgentStateService::class.java)
                val currentPlan = agentStateService.getPlan()
                formatPlanToMarkdown(currentPlan)
            }
            
            codingPanel?.switchToPlanView(content)
        }
    }
    
    /**
     * Save plan to AgentStateService
     */
    fun savePlanToService(project: Project, entries: List<AgentTaskEntry>) {
        val agentStateService = project.getService(AgentStateService::class.java)
        agentStateService.updatePlan(entries.toMutableList())
    }
    
    /**
     * Get current plan from AgentStateService
     */
    fun getCurrentPlan(project: Project): List<AgentTaskEntry> {
        val agentStateService = project.getService(AgentStateService::class.java)
        return agentStateService.getPlan()
    }
}

