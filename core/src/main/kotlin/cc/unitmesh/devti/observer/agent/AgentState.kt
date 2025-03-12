package cc.unitmesh.devti.observer.agent

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.llms.custom.Message
import com.intellij.openapi.components.Service
import com.intellij.util.diff.Diff.Change
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

data class AgentState(
    val conversationId: String = UUID.randomUUID().toString(),
    val changeList: List<Change> = emptyList(),
    val messages: List<Message> = emptyList(),
    val usedTools: List<AgentTool> = emptyList(),
)


@Service
class AgentStateService {
    var state: AgentState = AgentState()

    /**
     * Call some LLM to compress it or use some other method to compress the history
     */
    fun compressHistory(messages: List<Message>): List<Message> {
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