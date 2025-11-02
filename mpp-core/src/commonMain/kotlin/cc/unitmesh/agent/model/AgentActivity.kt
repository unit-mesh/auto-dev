package cc.unitmesh.agent.model

/**
 * Agent 活动事件
 * 用于向外部报告 Agent 的执行状态
 */
sealed class AgentActivity {
    data class ToolCallStart(
        val toolName: String,
        val args: Map<String, Any>
    ) : AgentActivity()

    data class ToolCallEnd(
        val toolName: String,
        val output: String
    ) : AgentActivity()

    data class ThoughtChunk(
        val text: String
    ) : AgentActivity()

    data class Error(
        val context: String,
        val error: String
    ) : AgentActivity()

    data class Progress(
        val message: String
    ) : AgentActivity()

    data class StreamUpdate(
        val text: String
    ) : AgentActivity()

    data class TaskComplete(
        val result: String
    ) : AgentActivity()
}

