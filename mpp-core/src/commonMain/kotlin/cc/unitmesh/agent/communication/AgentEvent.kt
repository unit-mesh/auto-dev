package cc.unitmesh.agent.communication

/**
 * Agent 事件
 * Agent -> 用户/UI 的通信
 * 
 * 参考 Codex 的 Queue Pair 模式
 */
sealed class AgentEvent {
    /**
     * 流式文本更新
     */
    data class StreamUpdate(
        val text: String,
        val accumulated: String = ""
    ) : AgentEvent()

    /**
     * 工具调用请求（需要审批）
     */
    data class ToolCallRequest(
        val callId: String,
        val tool: String,
        val params: Map<String, Any>,
        val needsApproval: Boolean = false
    ) : AgentEvent()

    /**
     * 工具调用开始
     */
    data class ToolCallStart(
        val callId: String,
        val tool: String
    ) : AgentEvent()

    /**
     * 工具调用结束
     */
    data class ToolCallEnd(
        val callId: String,
        val tool: String,
        val output: String,
        val success: Boolean
    ) : AgentEvent()

    /**
     * 任务完成
     */
    data class TaskComplete(
        val result: String,
        val metadata: Map<String, Any> = emptyMap()
    ) : AgentEvent()

    /**
     * 错误
     */
    data class Error(
        val message: String,
        val context: String? = null,
        val recoverable: Boolean = false
    ) : AgentEvent()

    /**
     * 进度更新
     */
    data class Progress(
        val step: Int,
        val total: Int,
        val message: String
    ) : AgentEvent()

    /**
     * 思考过程
     */
    data class ThoughtChunk(
        val text: String
    ) : AgentEvent()

    /**
     * SubAgent 启动
     */
    data class SubAgentStart(
        val agentName: String,
        val purpose: String
    ) : AgentEvent()

    /**
     * SubAgent 完成
     */
    data class SubAgentComplete(
        val agentName: String,
        val result: String
    ) : AgentEvent()
}

