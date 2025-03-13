package cc.unitmesh.devti.observer.agent

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.llms.custom.Message
import com.intellij.util.diff.Diff.Change
import java.util.UUID

data class AgentState(
    var conversationId: String = UUID.randomUUID().toString(),
    var changeList: List<Change> = emptyList(),
    var messages: List<Message> = emptyList(),
    var usedTools: List<AgentTool> = emptyList(),
)

