package cc.unitmesh.agent.recovery

import cc.unitmesh.agent.subagent.ErrorRecoveryAgent
import cc.unitmesh.agent.tool.ToolType
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
            val validatedInput = errorRecoveryAgent.validateInput(input)

            val result = errorRecoveryAgent.execute(validatedInput) { progress ->
                println("   $progress")
            }

            if (result.success) {
                println("\n✓ Error Recovery completed successfully\n")
                result.content
            } else {
                println("\n✗ Error Recovery failed: ${result.content}\n")
                null
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
        // 对于 shell 命令错误，检查是否是可恢复的错误类型
        if (toolName == ToolType.Shell.name) {
            val recoverableShellErrors = listOf(
                "compilation failed", "build failed", "test failed",
                "dependency", "gradle", "maven", "npm", "yarn",
                "syntax error", "cannot find symbol", "unresolved reference",
                "permission denied", "command not found"
            )
            return recoverableShellErrors.any { errorMessage.contains(it, ignoreCase = true) }
        }

        // 对于文件操作错误，检查具体的错误类型
        if (toolName in listOf(ToolType.ReadFile.name, ToolType.WriteFile.name, ToolType.Glob.name)) {
            val recoverableFileErrors = listOf(
                "permission denied", "no such file or directory", "file not found",
                "access denied", "directory not found", "path does not exist",
                "invalid path", "encoding error", "file locked"
            )
            return recoverableFileErrors.any { errorMessage.contains(it, ignoreCase = true) }
        }

        // 对于其他工具，检查通用的可恢复错误
        val generalRecoverableErrors = listOf(
            "timeout", "connection", "network", "temporary", "retry",
            "configuration", "environment", "missing"
        )

        return generalRecoverableErrors.any { errorMessage.contains(it, ignoreCase = true) }
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
            "command" to (command ?: toolName),
            "errorMessage" to errorMessage
        )

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
