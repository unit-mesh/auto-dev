package cc.unitmesh.agent.executor

import cc.unitmesh.agent.*
import cc.unitmesh.agent.conversation.ConversationManager
import cc.unitmesh.agent.core.SubAgentManager
import cc.unitmesh.agent.tool.schema.ToolResultFormatter
import cc.unitmesh.agent.orchestrator.ToolExecutionResult
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.agent.state.ToolExecutionState
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.toToolType
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.yield
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import cc.unitmesh.agent.orchestrator.ToolExecutionContext as OrchestratorContext

class CodingAgentExecutor(
    projectPath: String,
    llmService: KoogLLMService,
    toolOrchestrator: ToolOrchestrator,
    renderer: CodingAgentRenderer,
    maxIterations: Int = 100,
    private val subAgentManager: SubAgentManager? = null,
    enableLLMStreaming: Boolean = true
) : BaseAgentExecutor(
    projectPath = projectPath,
    llmService = llmService,
    toolOrchestrator = toolOrchestrator,
    renderer = renderer,
    maxIterations = maxIterations,
    enableLLMStreaming = enableLLMStreaming
) {
    private val steps = mutableListOf<AgentStep>()
    private val edits = mutableListOf<AgentEdit>()

    private val recentToolCalls = mutableListOf<String>()
    private val MAX_REPEAT_COUNT = 3

    /**
     * 执行 Agent 任务
     */
    suspend fun execute(
        task: AgentTask,
        systemPrompt: String,
        onProgress: (String) -> Unit = {}
    ): AgentResult {
        resetExecution()
        conversationManager = ConversationManager(llmService, systemPrompt)
        
        // Set up token tracking callback to update renderer
        conversationManager?.onTokenUpdate = { tokenInfo ->
            renderer.updateTokenInfo(tokenInfo)
        }
        
        val initialUserMessage = buildInitialUserMessage(task)

        onProgress("🚀 CodingAgent started")
        onProgress("Project: ${task.projectPath}")
        onProgress("Task: ${task.requirement}")

        while (shouldContinue()) {
            yield()

            currentIteration++
            renderer.renderIterationHeader(currentIteration, maxIterations)

            val llmResponse = StringBuilder()

            try {
                val message = if (currentIteration == 1) initialUserMessage else buildContinuationMessage()
                val compileDevIns = (currentIteration == 1) // Only compile DevIns on first iteration
                val response = getLLMResponse(message, compileDevIns)
                llmResponse.append(response)
            } catch (e: Exception) {
                break
            }

            val toolCalls = toolCallParser.parseToolCalls(llmResponse.toString())
            if (toolCalls.isEmpty()) {
                renderer.renderTaskComplete()
                break
            }

            val toolResults = executeToolCalls(toolCalls)
            val toolResultsText = ToolResultFormatter.formatMultipleToolResults(toolResults)
            conversationManager!!.addToolResults(toolResultsText)

            if (isTaskComplete(llmResponse.toString())) {
                renderer.renderTaskComplete()
                break
            }

            if (isStuck()) {
                renderer.renderError("Agent appears to be stuck. Stopping.")
                break
            }
        }

        return buildResult()
    }

    private fun resetExecution() {
        currentIteration = 0
        steps.clear()
        edits.clear()
        recentToolCalls.clear()
    }

    private fun buildInitialUserMessage(task: AgentTask): String {
        return "Task: ${task.requirement}"
    }

    override fun buildContinuationMessage(): String {
        return "Please continue with the task based on the tool execution results above. " +
                "Use additional tools if needed, or summarize if the task is complete."
    }

    /**
     * 并行执行多个工具调用
     *
     * 策略：
     * 1. 预先检查所有工具是否重复
     * 2. 并行启动所有工具执行
     * 3. 等待所有工具完成后统一处理结果
     * 4. 按顺序渲染和处理错误恢复
     */
    private suspend fun executeToolCalls(toolCalls: List<ToolCall>): List<Triple<String, Map<String, Any>, ToolExecutionResult>> = coroutineScope {
        val results = mutableListOf<Triple<String, Map<String, Any>, ToolExecutionResult>>()

        val toolsToExecute = mutableListOf<ToolCall>()
        var hasRepeatError = false

        for (toolCall in toolCalls) {
            if (hasRepeatError) break

            val toolName = toolCall.toolName
            val params = toolCall.params.mapValues { it.value as Any }
            val paramsStr = params.entries.joinToString(" ") { (key, value) ->
                "$key=\"$value\""
            }
            val toolSignature = "$toolName:$paramsStr"

            recentToolCalls.add(toolSignature)
            if (recentToolCalls.size > 10) {
                recentToolCalls.removeAt(0)
            }

            val exactMatches = recentToolCalls.takeLast(MAX_REPEAT_COUNT).count { it == toolSignature }
            val toolType = toolName.toToolType()
            val maxAllowedRepeats = when (toolType) {
                ToolType.ReadFile, ToolType.WriteFile -> 3
                ToolType.Shell -> 2
                else -> when (toolName) {
                    ToolType.ReadFile.name, ToolType.WriteFile.name -> 3
                    ToolType.Shell.name -> 2
                    else -> 2
                }
            }

            if (exactMatches >= maxAllowedRepeats) {
                renderer.renderRepeatWarning(toolName, exactMatches)
                val currentTime = Clock.System.now().toEpochMilliseconds()
                val errorResult = ToolExecutionResult(
                    executionId = "repeat-error-$currentTime",
                    toolName = toolName,
                    result = ToolResult.Error("Stopped due to repeated tool calls"),
                    startTime = currentTime,
                    endTime = currentTime,
                    state = ToolExecutionState.Failed(
                        "repeat-error-$currentTime",
                        "Stopped due to repeated tool calls",
                        0
                    )
                )
                results.add(Triple(toolName, params, errorResult))
                hasRepeatError = true
                break
            }

            toolsToExecute.add(toolCall)
        }

        if (hasRepeatError) {
            return@coroutineScope results
        }

        for (toolCall in toolsToExecute) {
            val toolName = toolCall.toolName
            val params = toolCall.params.mapValues { it.value as Any }
            val paramsStr = params.entries.joinToString(" ") { (key, value) ->
                "$key=\"$value\""
            }

            renderer.renderToolCall(toolName, paramsStr)

            val executionContext = OrchestratorContext(
                workingDirectory = projectPath,
                environment = emptyMap()
            )

            val executionResult = toolOrchestrator.executeToolCall(
                toolName,
                params,
                executionContext
            )

            results.add(Triple(toolName, params, executionResult))

            val stepResult = AgentStep(
                step = currentIteration,
                action = toolName,
                tool = toolName,
                params = params,
                result = executionResult.content,
                success = executionResult.isSuccess
            )
            steps.add(stepResult)

            val fullOutput = when (val result = executionResult.result) {
                is ToolResult.Error -> {
                    buildString {
                        appendLine("Error: ${result.message}")
                        appendLine("Error Type: ${result.errorType}")
                        executionResult.metadata["stderr"]?.let { stderr ->
                            if (stderr.isNotEmpty()) {
                                appendLine("\nStderr:")
                                appendLine(stderr)
                            }
                        }
                        executionResult.metadata["stdout"]?.let { stdout ->
                            if (stdout.isNotEmpty()) {
                                appendLine("\nStdout:")
                                appendLine(stdout)
                            }
                        }
                    }
                }
                is ToolResult.AgentResult -> if (!result.success) result.content else stepResult.result
                else -> stepResult.result
            }

            val contentHandlerResult = checkForLongContent(toolName, fullOutput ?: "", executionResult)
            val displayOutput = contentHandlerResult?.content ?: fullOutput

            renderer.renderToolResult(
                toolName,
                stepResult.success,
                stepResult.result,
                displayOutput,
                executionResult.metadata
            )

            val currentToolType = toolName.toToolType()
            if ((currentToolType == ToolType.WriteFile) && executionResult.isSuccess) {
                recordFileEdit(params)
            }

            // 错误恢复处理
            if (!executionResult.isSuccess) {
                val command = if (toolName == "shell") params["command"] as? String else null
                val errorMessage = executionResult.content ?: "Unknown error"

                renderer.renderError("Tool execution failed: $errorMessage")
            }
        }

        results
    }

    private fun recordFileEdit(params: Map<String, Any>) {
        val path = params["path"] as? String
        val content = params["content"] as? String
        val mode = params["mode"] as? String

        if (path != null && content != null) {
            edits.add(
                AgentEdit(
                    file = path,
                    operation = if (mode == "create") AgentEditOperation.CREATE else AgentEditOperation.UPDATE,
                    content = content
                )
            )
        }
    }

    private fun isTaskComplete(llmResponse: String): Boolean {
        val completeKeywords = listOf(
            "TASK_COMPLETE",
            "task complete",
            "Task completed",
            "implementation is complete",
            "all done",
            "finished"
        )

        return completeKeywords.any { keyword ->
            llmResponse.contains(keyword, ignoreCase = true)
        }
    }

    private fun isStuck(): Boolean {
        return currentIteration > 5 &&
                steps.takeLast(5).all { !it.success || it.result?.contains("already exists") == true }
    }

    private fun buildResult(): AgentResult {
        val success = steps.any { it.success }
        val message = if (success) {
            "Task completed after $currentIteration iterations"
        } else {
            "Task incomplete after $currentIteration iterations"
        }

        return AgentResult(
            success = success,
            message = message,
            steps = steps,
            edits = edits
        )
    }

    /**
     * 检查工具输出是否需要长内容处理
     */
    private suspend fun checkForLongContent(
        toolName: String,
        output: String,
        executionResult: ToolExecutionResult
    ): ToolResult.AgentResult? {

        if (subAgentManager == null) {
            return null
        }

        // 对于 Live Session，不要用分析结果替换原始输出
        // Live Terminal 已经在 Timeline 中显示实时输出了
        val isLiveSession = executionResult.metadata["isLiveSession"] == "true"
        if (isLiveSession) {
            return null
        }

        // 检测内容类型
        val contentType = when {
            toolName == "glob" -> "file-list"
            toolName == "shell" -> "shell-output"
            toolName == "grep" -> "search-results"
            toolName == "read-file" -> "file-content"
            output.startsWith("{") || output.startsWith("[") -> "json"
            output.contains("<?xml") -> "xml"
            else -> "text"
        }

        // 构建元数据
        val metadata = mutableMapOf<String, String>()
        metadata["toolName"] = toolName
        metadata["executionId"] = executionResult.executionId
        metadata["success"] = executionResult.isSuccess.toString()

        executionResult.metadata.forEach { (key, value) ->
            metadata["tool_$key"] = value
        }

        return subAgentManager.checkAndHandleLongContent(
            content = output,
            contentType = contentType,
            source = toolName,
            metadata = metadata
        )
    }

    /**
     * 获取对话历史
     */
    fun getConversationHistory(): List<cc.unitmesh.devins.llm.Message> {
        return conversationManager?.getHistory() ?: emptyList()
    }
}
