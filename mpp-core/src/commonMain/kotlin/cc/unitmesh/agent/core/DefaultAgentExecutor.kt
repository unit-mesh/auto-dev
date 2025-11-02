package cc.unitmesh.agent.core

import cc.unitmesh.agent.communication.AgentChannel
import cc.unitmesh.agent.communication.AgentEvent
import cc.unitmesh.agent.model.*
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock

/**
 * 默认的 Agent 执行器实现
 * 
 * 负责执行 Agent 的主循环：
 * 1. 调用 LLM
 * 2. 解析和执行工具调用
 * 3. 检查终止条件
 * 4. 发送活动事件
 * 
 * 参考 Gemini CLI 的 AgentExecutor 设计
 */
class DefaultAgentExecutor(
    private val llmService: KoogLLMService,
    private val channel: AgentChannel? = null
) : AgentExecutor {
    
    private val activeAgents = mutableMapOf<String, Boolean>()

    override suspend fun execute(
        definition: AgentDefinition,
        context: AgentContext,
        onActivity: (AgentActivity) -> Unit
    ): AgentResult {
        val startTime = Clock.System.now().toEpochMilliseconds()
        var turnCount = 0
        val maxTurns = definition.runConfig.maxTurns
        val steps = mutableListOf<AgentStep>()
        
        activeAgents[context.agentId] = true
        
        try {
            // Build initial prompt
            var currentPrompt = buildInitialPrompt(definition, context)
            
            while (turnCount < maxTurns && activeAgents[context.agentId] == true) {
                turnCount++
                
                // Check timeout
                val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
                val maxTimeMs = definition.runConfig.maxTimeMinutes * 60 * 1000
                if (elapsed > maxTimeMs) {
                    return AgentResult.Failure(
                        error = "Agent execution timeout after ${definition.runConfig.maxTimeMinutes} minutes",
                        terminateReason = TerminateReason.TIMEOUT,
                        steps = steps
                    )
                }
                
                // Emit progress
                onActivity(AgentActivity.Progress("Turn $turnCount/$maxTurns"))
                channel?.emit(AgentEvent.Progress(turnCount, maxTurns, "Processing..."))
                
                // Call LLM
                val response = try {
                    val responseText = StringBuilder()
                    llmService.streamPrompt(
                        userPrompt = currentPrompt,
                        compileDevIns = false  // Agent 自己处理 DevIns
                    ).catch { e ->
                        throw Exception("LLM call failed: ${e.message}")
                    }.collect { chunk ->
                        responseText.append(chunk)
                        onActivity(AgentActivity.StreamUpdate(chunk))
                        channel?.emit(AgentEvent.StreamUpdate(chunk, responseText.toString()))
                    }
                    responseText.toString()
                } catch (e: Exception) {
                    steps.add(AgentStep(
                        step = turnCount,
                        action = "llm_call",
                        result = "Error: ${e.message}",
                        success = false
                    ))
                    
                    if (definition.runConfig.terminateOnError) {
                        return AgentResult.Failure(
                            error = "LLM call failed: ${e.message}",
                            terminateReason = TerminateReason.ERROR,
                            steps = steps
                        )
                    }
                    continue
                }
                
                // Check for completion signal
                if (isTaskComplete(response)) {
                    val result = extractFinalResult(response)
                    steps.add(AgentStep(
                        step = turnCount,
                        action = "complete_task",
                        result = result,
                        success = true
                    ))
                    
                    onActivity(AgentActivity.TaskComplete(result))
                    channel?.emit(AgentEvent.TaskComplete(result))
                    
                    return AgentResult.Success(
                        output = mapOf("result" to result),
                        terminateReason = TerminateReason.GOAL,
                        steps = steps
                    )
                }
                
                // Extract tool calls (simplified - real implementation would parse DevIns blocks)
                val toolCalls = extractToolCalls(response)
                
                if (toolCalls.isEmpty()) {
                    // No tool calls and no completion - treat as reasoning step
                    steps.add(AgentStep(
                        step = turnCount,
                        action = "reasoning",
                        result = response.take(200),
                        success = true
                    ))
                    
                    // Prepare next prompt with previous response
                    currentPrompt = "Continue with the task. Previous response: ${response.take(500)}"
                } else {
                    // Process tool calls (simplified for now)
                    steps.add(AgentStep(
                        step = turnCount,
                        action = "tool_calls",
                        result = "Executed ${toolCalls.size} tool(s)",
                        success = true
                    ))
                    
                    currentPrompt = "Tool results received. Continue with the task."
                }
            }
            
            // Max turns reached
            return AgentResult.Failure(
                error = "Max turns ($maxTurns) reached without completion",
                terminateReason = TerminateReason.MAX_TURNS,
                steps = steps
            )
            
        } catch (e: Exception) {
            onActivity(AgentActivity.Error("execution", e.message ?: "Unknown error"))
            channel?.emit(AgentEvent.Error(e.message ?: "Unknown error", "execution"))
            
            return AgentResult.Failure(
                error = e.message ?: "Unknown error",
                terminateReason = TerminateReason.ERROR,
                steps = steps
            )
        } finally {
            activeAgents.remove(context.agentId)
        }
    }

    override suspend fun cancel(agentId: String) {
        activeAgents[agentId] = false
        channel?.emit(AgentEvent.Error("Agent cancelled by user", "cancellation"))
    }

    /**
     * 构建初始提示词
     */
    private fun buildInitialPrompt(definition: AgentDefinition, context: AgentContext): String {
        val systemPrompt = definition.promptConfig.systemPrompt
        val query = definition.promptConfig.queryTemplate?.let { template ->
            // Simple template substitution
            var result = template
            for ((key, value) in context.inputs) {
                result = result.replace("\${$key}", value.toString())
            }
            result
        } ?: "Start working on the task"
        
        return buildString {
            appendLine("# System Instructions")
            appendLine(systemPrompt)
            appendLine()
            appendLine("# Task")
            appendLine(query)
            appendLine()
            appendLine("# Important Rules")
            appendLine("- You MUST call the 'complete_task' tool when you finish")
            appendLine("- Respond with 'TASK_COMPLETE' when done")
            appendLine("- Use available tools to complete the task")
            appendLine()
            if (definition.toolConfig != null) {
                appendLine("# Available Tools")
                definition.toolConfig.allowedTools.forEach { tool ->
                    appendLine("- $tool")
                }
                appendLine()
            }
        }
    }

    /**
     * 检查任务是否完成
     */
    private fun isTaskComplete(response: String): Boolean {
        val lowerResponse = response.lowercase()
        return lowerResponse.contains("task_complete") ||
                lowerResponse.contains("task complete") ||
                lowerResponse.contains("mission complete") ||
                lowerResponse.contains("completed successfully")
    }

    /**
     * 提取最终结果
     */
    private fun extractFinalResult(response: String): String {
        // Simple extraction - could be more sophisticated
        return response.take(500)
    }

    /**
     * 提取工具调用（简化版本）
     */
    private fun extractToolCalls(response: String): List<String> {
        val toolCalls = mutableListOf<String>()
        
        // Extract DevIns blocks
        val devinRegex = Regex("<devin>([\\s\\S]*?)</devin>")
        devinRegex.findAll(response).forEach { match ->
            toolCalls.add(match.groupValues[1].trim())
        }
        
        // Extract tool mentions (simple pattern)
        val toolPattern = Regex("/([a-z-]+)")
        toolPattern.findAll(response).forEach { match ->
            toolCalls.add(match.groupValues[1])
        }
        
        return toolCalls.distinct()
    }
}

