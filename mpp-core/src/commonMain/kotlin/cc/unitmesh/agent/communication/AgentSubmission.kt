package cc.unitmesh.agent.communication

/**
 * Agent 提交指令
 * 用户/UI -> Agent 的通信
 * 
 * 参考 Codex 的 Queue Pair 模式
 */
sealed class AgentSubmission {
    /**
     * 发送提示词
     */
    data class SendPrompt(
        val text: String,
        val context: Map<String, Any> = emptyMap()
    ) : AgentSubmission()

    /**
     * 取消任务
     */
    data class CancelTask(
        val taskId: String
    ) : AgentSubmission()

    /**
     * 批准工具调用
     */
    data class ApproveToolCall(
        val callId: String,
        val approved: Boolean
    ) : AgentSubmission()

    /**
     * 重试失败的操作
     */
    data class RetryAction(
        val actionId: String
    ) : AgentSubmission()

    /**
     * 更新配置
     */
    data class UpdateConfig(
        val config: Map<String, Any>
    ) : AgentSubmission()
}

