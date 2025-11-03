package cc.unitmesh.agent.recovery

import cc.unitmesh.agent.subagent.ErrorRecoveryAgent
import cc.unitmesh.llm.KoogLLMService

/**
 * 错误恢复管理器
 *
 * 负责处理工具执行失败时的错误恢复逻辑
 */
class ErrorRecoveryManager(private val projectPath: String, private val llmService: KoogLLMService) {
    private val errorRecoveryAgent = ErrorRecoveryAgent(projectPath, llmService)

    /**
     * 处理工具执行错误
     *
     * @param toolName 失败的工具名称
     * @param command 执行的命令（如果适用）
     * @param errorMessage 错误消息
     * @param exitCode 退出码（如果适用）
     * @return 恢复建议，如果恢复失败则返回 null
     */
    suspend fun handleToolError(
        toolName: String,
        command: String? = null,
        errorMessage: String,
        exitCode: Int? = null
    ): String? {
        if (!shouldAttemptRecovery(toolName, errorMessage)) {
            return null
        }

        println("\n════════════════════════════════════════════════════════")
        println("   🔧 ACTIVATING ERROR RECOVERY SUBAGENT")
        println("   Tool: $toolName")
        println("   Error: ${errorMessage.take(100)}${if (errorMessage.length > 100) "..." else ""}")
        println("════════════════════════════════════════════════════════\n")

        return try {
            val input = buildRecoveryInput(toolName, command, errorMessage, exitCode)

            val result = errorRecoveryAgent.run(input) { progress ->
                println("   $progress")
            }

            when (result) {
                else -> {
                    println("\n✗ Unexpected result type from ErrorRecoveryAgent\n")
                    null
                }
            }
        } catch (e: Exception) {
            println("\n✗ Error Recovery failed: ${e.message}\n")
            null
        }
    }

    /**
     * 判断是否应该尝试错误恢复
     */
    private fun shouldAttemptRecovery(toolName: String, errorMessage: String): Boolean {
        // 对于 shell 命令错误，总是尝试恢复
        if (toolName == "shell") {
            return true
        }

        // 对于文件操作错误，如果是权限或路径问题，尝试恢复
        if (toolName in listOf("write-file", "read-file")) {
            val recoverableErrors = listOf(
                "permission denied",
                "no such file or directory",
                "file not found",
                "access denied"
            )
            return recoverableErrors.any { errorMessage.contains(it, ignoreCase = true) }
        }

        return false
    }

    /**
     * 构建恢复输入参数
     */
    private fun buildRecoveryInput(
        toolName: String,
        command: String?,
        errorMessage: String,
        exitCode: Int?
    ): Map<String, Any> {
        val input = mutableMapOf<String, Any>(
            "toolName" to toolName,
            "errorMessage" to errorMessage
        )

        command?.let { input["command"] = it }
        exitCode?.let { input["exitCode"] = it }

        return input
    }

    /**
     * 检查错误是否是致命的（不应该继续执行）
     */
    fun isFatalError(toolName: String, errorMessage: String): Boolean {
        val fatalErrors = listOf(
            "out of memory",
            "disk full",
            "network unreachable",
            "authentication failed",
            "permission permanently denied"
        )

        return fatalErrors.any { errorMessage.contains(it, ignoreCase = true) }
    }
}
