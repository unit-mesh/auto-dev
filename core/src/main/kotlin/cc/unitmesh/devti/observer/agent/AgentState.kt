package cc.unitmesh.devti.observer.agent

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.llms.custom.Message
import com.intellij.openapi.components.Service
import com.intellij.util.diff.Diff.Change
import java.util.UUID

data class AgentState(
    var conversationId: String = UUID.randomUUID().toString(),
    var changeList: List<Change> = emptyList(),
    var messages: List<Message> = emptyList(),
    var usedTools: List<AgentTool> = emptyList(),
)

@Service(Service.Level.PROJECT)
class AgentStateService {
    var state: AgentState = AgentState()

    fun resetState() {
        state = AgentState()
    }

    fun addTools(tools: List<AgentTool>) {

    }

    fun addChanges(val fileName: String) {
        // todo changeList.add()
    }

    /**
     * Call some LLM to compress it or use some other method to compress the history
     */
    fun preprocessMessages(messages: List<Message>): List<Message> {
        state.messages = messages
        return messages
    }
}

interface AgentProcessor {
    fun process()
}

class HistoryMessageProcessor : AgentProcessor {
    override fun process() {
        TODO("Not yet implemented")
    }
}