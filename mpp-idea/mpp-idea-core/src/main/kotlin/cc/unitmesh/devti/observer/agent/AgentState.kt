package cc.unitmesh.devti.observer.agent

import cc.unitmesh.devti.agent.tool.AgentTool
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import com.intellij.openapi.vcs.changes.Change
import java.util.UUID

data class AgentState(
    /**
     * First question of user
     */
    var originIntention: String = "",

    var conversationId: String = UUID.randomUUID().toString(),

    var changes: MutableList<Change> = mutableListOf(),

    var messages: List<Message> = emptyList(),

    var usedTools: List<AgentTool> = emptyList(),

    /**
     * Logging environment variables, maybe related to  [cc.unitmesh.devti.provider.context.ChatContextProvider]
     */
    var environment: Map<String, String> = emptyMap(),

    var plan: MutableList<AgentTaskEntry> = mutableListOf()
)

