package cc.unitmesh.devti.observer.agent

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.llms.custom.Message
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vcs.changes.Change

@Service(Service.Level.PROJECT)
class AgentStateService {
    var state: AgentState = AgentState()

    fun resetState() {
        state = AgentState()
    }

    fun addTools(tools: List<BuiltinCommand>) {
        state.usedTools = tools.map {
            AgentTool(it.commandName, it.description, "")
        }

        logger<AgentStateService>().info("Called agent tools:\n ${state.usedTools.joinToString("\n")}")
    }

    fun updateChanges(changes: Collection<Change?>?) {
        val allChanges = changes?.filterNotNull()?.map { it } ?: emptyList()
        state.changes = allChanges.toMutableList()
    }

    /**
     * Call some LLM to compress it or use some other method to compress the history
     */
    fun processMessages(messages: List<Message>): List<Message> {
        state.messages = messages
        return messages
    }

    fun buildOriginIntention(): String? {
        val intention = state.messages
            .firstOrNull { it.role.lowercase() == "user" }
            ?.content

        if (intention != null) {
            state.originIntention = intention
        }

        return intention
    }

    fun allMessages(): List<Message> {
        return state.messages
    }

    fun updatePlan(items: MutableList<PlanList>) {
        this.state.planLists = items
        ApplicationManager.getApplication().messageBus
            .syncPublisher(PlanUpdateListener.TOPIC)
            .onPlanUpdate(items)
    }
}