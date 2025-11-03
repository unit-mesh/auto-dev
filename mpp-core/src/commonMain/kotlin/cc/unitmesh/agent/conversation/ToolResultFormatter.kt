package cc.unitmesh.agent.conversation

import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.orchestrator.ToolExecutionResult

/**
 * 工具执行结果格式化器
 * 
 * 负责将工具执行结果格式化为适合 LLM 理解的文本格式
 */
object ToolResultFormatter {
    
    /**
     * 格式化单个工具执行结果
     *
     * @param toolName 工具名称
     * @param params 工具参数
     * @param result 执行结果
     * @return 格式化后的文本
     */
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
        
        // 格式化结果
        sb.append("Result: ${if (result.isSuccess) "SUCCESS" else "FAILED"}\n")
        sb.append("Output:\n")
        sb.append(result.content)

        if (!result.isSuccess) {
            val errorMsg = result.errorMessage
            if (!errorMsg.isNullOrEmpty()) {
                sb.append("\nError: $errorMsg")
            }
        }
        
        return sb.toString()
    }
    
    /**
     * 格式化多个工具执行结果
     *
     * @param toolResults 工具执行结果列表
     * @return 格式化后的文本
     */
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
    
    /**
     * 格式化工具执行摘要
     *
     * @param toolResults 工具执行结果列表
     * @return 摘要文本
     */
    fun formatToolExecutionSummary(
        toolResults: List<Triple<String, Map<String, Any>, ToolExecutionResult>>
    ): String {
        if (toolResults.isEmpty()) {
            return "No tools executed in this iteration."
        }
        
        val successCount = toolResults.count { it.third.isSuccess }
        val failureCount = toolResults.size - successCount
        
        val sb = StringBuilder()
        sb.append("Executed ${toolResults.size} tool(s): ")
        sb.append("$successCount successful, $failureCount failed.\n\n")
        
        // 列出执行的工具
        toolResults.forEach { (toolName, _, result) ->
            val status = if (result.isSuccess) "✓" else "✗"
            sb.append("$status $toolName\n")
        }
        
        return sb.toString()
    }
    
    /**
     * 创建继续对话的提示
     *
     * @param toolResults 工具执行结果
     * @param context 额外上下文信息
     * @return 继续对话的提示文本
     */
    fun createContinuationPrompt(
        toolResults: List<Triple<String, Map<String, Any>, ToolExecutionResult>>,
        context: String? = null
    ): String {
        val sb = StringBuilder()
        
        // 添加工具执行结果
        sb.append(formatMultipleToolResults(toolResults))
        sb.append("\n\n")
        
        // 添加额外上下文
        if (!context.isNullOrBlank()) {
            sb.append("Additional context:\n")
            sb.append(context)
            sb.append("\n\n")
        }
        
        // 添加继续指令
        sb.append("Based on the tool execution results above, please continue with the task. ")
        sb.append("If you need to use more tools, provide them in DevIns format. ")
        sb.append("If the task is complete, please summarize what was accomplished.")
        
        return sb.toString()
    }
}
