package cc.unitmesh.devti.observer.agent

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.llms.custom.Message
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger

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

    fun addChanges(fileName: String) {
        // todo changeList.add()
    }

    /**
     * Call some LLM to compress it or use some other method to compress the history
     */
    fun processMessages(messages: List<Message>): List<Message> {
        state.messages = messages
        /// todo compress message in here
        return messages
    }

    fun resolveIssue() {
        // todo resolve issue
    }
}