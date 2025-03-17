package cc.unitmesh.devti.observer.plan

import cc.unitmesh.devti.gui.AutoDevPlanerTooWindow
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import cc.unitmesh.devti.observer.agent.AgentStateService

class EditPlanAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val agentStateService = project.getService(AgentStateService::class.java)
        val currentPlan = agentStateService.getPlan()
        val planString = MarkdownPlanParser.formatPlanToMarkdown(currentPlan)

        AutoDevPlanerTooWindow.showPlanEditor(project, planString) { newPlan ->
            val newPlanItems = MarkdownPlanParser.parse(newPlan)
            if (newPlanItems.isNotEmpty()) {
                agentStateService.updatePlan(newPlanItems.toMutableList())
            }
        }
    }
}
