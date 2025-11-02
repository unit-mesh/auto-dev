package cc.unitmesh.agent.model


/**
 * Agent 执行上下文
 * 
 * 注意：inputs 使用 Any 类型，因此不可序列化
 * 如需序列化，请手动转换为 JSON
 */
data class AgentContext(
    val agentId: String,
    val sessionId: String,
    val inputs: Map<String, Any>,
    val projectPath: String,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        fun create(
            agentName: String,
            sessionId: String,
            inputs: Map<String, Any>,
            projectPath: String
        ): AgentContext {
            val agentId = "${agentName}-${generateRandomId()}"
            return AgentContext(
                agentId = agentId,
                sessionId = sessionId,
                inputs = inputs,
                projectPath = projectPath
            )
        }

        private fun generateRandomId(): String {
            return (0..5).map { ('a'..'z').random() }.joinToString("")
        }
    }
}

/**
 * Agent 执行结果
 */
sealed class AgentResult {
    data class Success(
        val output: Map<String, Any>,
        val terminateReason: TerminateReason,
        val steps: List<AgentStep>,
        val metadata: Map<String, Any> = emptyMap()
    ) : AgentResult()

    data class Failure(
        val error: String,
        val terminateReason: TerminateReason,
        val steps: List<AgentStep>
    ) : AgentResult()
}

/**
 * Agent 执行步骤
 */
data class AgentStep(
    val step: Int,
    val action: String,
    val tool: String? = null,
    val params: Map<String, Any>? = null,
    val result: String? = null,
    val success: Boolean,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)

/**
 * 终止原因
 */
enum class TerminateReason {
    GOAL,           // 目标完成
    MAX_TURNS,      // 达到最大轮次
    TIMEOUT,        // 超时
    ERROR,          // 错误
    ABORTED         // 用户取消
}

