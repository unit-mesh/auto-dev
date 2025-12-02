package cc.unitmesh.agent.tool.schema

import cc.unitmesh.agent.orchestrator.ToolExecutionResult
import cc.unitmesh.agent.tool.ToolResult

/**
 * 工具执行结果格式化器
 *
 * 负责将工具执行结果格式化为适合 LLM 理解的文本格式
 */
object ToolResultFormatter {
    fun formatToolResult(
        toolName: String,
        params: Map<String, Any>,
        result: ToolExecutionResult
    ): String {
        val sb = StringBuilder()

        sb.append("Tool execution result:\n")
        sb.append("Tool: $toolName\n")

        // 格式化参数
        if (params.isNotEmpty()) {
            sb.append("Parameters:\n")
            params.forEach { (key, value) ->
                sb.append("  $key: $value\n")
            }
        }

        // 格式化结果状态
        val statusText = when {
            result.isPending -> "PENDING"
            result.isSuccess -> "SUCCESS"
            else -> "FAILED"
        }
        sb.append("Result: $statusText\n")

        // 处理 Pending 状态的特殊格式化
        if (result.isPending) {
            val pending = result.result as? ToolResult.Pending
            if (pending != null) {
                sb.append("Status: Process is executing asynchronously\n")
                sb.append("Session ID: ${pending.sessionId}\n")
                sb.append("Command: ${pending.command}\n")
                sb.append("Message: ${pending.message}\n")
            }
        } else {
            sb.append("Output:\n")
            sb.append(result.content)

            if (!result.isSuccess) {
                val errorMsg = result.errorMessage
                if (!errorMsg.isNullOrEmpty()) {
                    sb.append("\nError: $errorMsg")
                }
            }
        }

        return sb.toString()
    }

    fun formatMultipleToolResults(
        toolResults: List<Triple<String, Map<String, Any>, ToolExecutionResult>>
    ): String {
        if (toolResults.isEmpty()) {
            return "No tools were executed."
        }

        val sb = StringBuilder()
        sb.append("Tool execution results:\n\n")

        toolResults.forEachIndexed { index, (toolName, params, result) ->
            sb.append("${index + 1}. ")
            sb.append(formatToolResult(toolName, params, result))
            if (index < toolResults.size - 1) {
                sb.append("\n---\n\n")
            }
        }

        return sb.toString()
    }
}