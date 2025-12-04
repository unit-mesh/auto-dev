package cc.unitmesh.agent.executor

import cc.unitmesh.agent.*
import cc.unitmesh.agent.conversation.ConversationManager
import cc.unitmesh.agent.core.SubAgentManager
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.tool.schema.ToolResultFormatter
import cc.unitmesh.agent.orchestrator.ToolExecutionResult
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.agent.state.ToolExecutionState
import cc.unitmesh.agent.plan.PlanSummaryData
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.toToolType
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.yield
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import cc.unitmesh.agent.orchestrator.ToolExecutionContext as OrchestratorContext

/**
 * Configuration for async shell execution timeout behavior
 */
data class AsyncShellConfig(
    /** Initial wait timeout in milliseconds before notifying AI that process is still running */
    val initialWaitTimeoutMs: Long = 60_000L, // 1 minute
    /** Maximum total wait time in milliseconds (2 minutes, similar to Cursor/Claude Code) */
    val maxWaitTimeoutMs: Long = 120_000L, // 2 minutes
    /** Interval for checking process status after initial timeout */
    val checkIntervalMs: Long = 30_000L // 30 seconds
)

class CodingAgentExecutor(
    projectPath: String,
    llmService: KoogLLMService,
    toolOrchestrator: ToolOrchestrator,
    renderer: CodingAgentRenderer,
    maxIterations: Int = 100,
    private val subAgentManager: SubAgentManager? = null,
    enableLLMStreaming: Boolean = true,
    private val asyncShellConfig: AsyncShellConfig = AsyncShellConfig(),
    /**
     * When true, only execute the first tool call per LLM response.
     * This enforces the "one tool per response" rule even when LLM returns multiple tool calls.
     * Default is true to prevent LLM from executing multiple tools in one iteration.
     */
    private val singleToolPerIteration: Boolean = true
) : BaseAgentExecutor(
    projectPath = projectPath,
    llmService = llmService,
    toolOrchestrator = toolOrchestrator,
    renderer = renderer,
    maxIterations = maxIterations,
    enableLLMStreaming = enableLLMStreaming
) {
    private val logger = getLogger("CodingAgentExecutor")
    private val steps = mutableListOf<AgentStep>()
    private val edits = mutableListOf<AgentEdit>()

    private val recentToolCalls = mutableListOf<String>()
    private val MAX_REPEAT_COUNT = 3

    // Track task execution time
    private var taskStartTime: Long = 0L

    /**
     * 执行 Agent 任务
     */
    suspend fun execute(
        task: AgentTask,
        systemPrompt: String,
        onProgress: (String) -> Unit = {}
    ): AgentResult {
        resetExecution()

        // Start tracking execution time
        taskStartTime = Platform.getCurrentTimestamp()

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

            val allToolCalls = toolCallParser.parseToolCalls(llmResponse.toString())
            if (allToolCalls.isEmpty()) {
                val executionTimeMs = Platform.getCurrentTimestamp() - taskStartTime
                renderer.renderTaskComplete(executionTimeMs)
                break
            }

            // When singleToolPerIteration is enabled, only execute the first tool call
            // This enforces the "one tool per response" rule even when LLM returns multiple tool calls
            val toolCalls = if (singleToolPerIteration && allToolCalls.size > 1) {
                logger.warn { "LLM returned ${allToolCalls.size} tool calls, but singleToolPerIteration is enabled. Only executing the first one: ${allToolCalls.first().toolName}" }
                renderer.renderError("Warning: LLM returned ${allToolCalls.size} tool calls, only executing the first one")
                listOf(allToolCalls.first())
            } else {
                allToolCalls
            }

            val toolResults = executeToolCalls(toolCalls)
            val toolResultsText = ToolResultFormatter.formatMultipleToolResults(toolResults)
            conversationManager!!.addToolResults(toolResultsText)

            if (isTaskComplete(llmResponse.toString())) {
                val executionTimeMs = Platform.getCurrentTimestamp() - taskStartTime
                renderer.renderTaskComplete(executionTimeMs)
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
        taskStartTime = 0L
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

            // Use renderToolCallWithParams to pass parsed params directly
            // This avoids string parsing issues with complex values like planMarkdown
            renderer.renderToolCallWithParams(toolName, params)

            val executionContext = OrchestratorContext(
                workingDirectory = projectPath,
                environment = emptyMap(),
                timeout = asyncShellConfig.maxWaitTimeoutMs  // Use max timeout for shell commands
            )

            var executionResult = toolOrchestrator.executeToolCall(
                toolName,
                params,
                executionContext
            )

            // Handle Pending result (async shell execution)
            if (executionResult.isPending) {
                executionResult = handlePendingResult(executionResult, toolName, params)
            }

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
                is ToolResult.Pending -> stepResult.result // Should not happen after handlePendingResult
                is ToolResult.Success -> stepResult.result
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

            // Render plan summary bar after plan tool execution
            if (toolName == "plan" && executionResult.isSuccess) {
                renderPlanSummaryIfAvailable()
            }

            val currentToolType = toolName.toToolType()
            if ((currentToolType == ToolType.WriteFile) && executionResult.isSuccess) {
                recordFileEdit(params)
            }

            // 错误恢复处理
            // 跳过用户取消的场景 - 用户取消是明确的意图，不需要显示额外的错误消息
            val wasCancelledByUser = executionResult.metadata["cancelled"] == "true"
            if (!executionResult.isSuccess && !executionResult.isPending && !wasCancelledByUser) {
                val errorMessage = executionResult.content ?: "Unknown error"

                renderer.renderError("Tool execution failed: $errorMessage")
            }
        }

        results
    }

    /**
     * Handle a Pending result from async shell execution.
     * Waits for the session to complete with timeout handling.
     * If the process takes longer than initialWaitTimeoutMs, returns a special result
     * indicating the process is still running (similar to Augment's behavior).
     */
    private suspend fun handlePendingResult(
        pendingResult: ToolExecutionResult,
        toolName: String,
        params: Map<String, Any>
    ): ToolExecutionResult {
        val pending = pendingResult.result as? ToolResult.Pending
            ?: return pendingResult

        val sessionId = pending.sessionId
        val command = pending.command
        val startTime = pendingResult.startTime

        // First, try to wait for the initial timeout
        val initialResult = renderer.awaitSessionResult(sessionId, asyncShellConfig.initialWaitTimeoutMs)

        return when (initialResult) {
            is ToolResult.Success -> {
                // Process completed within initial timeout
                val endTime = Clock.System.now().toEpochMilliseconds()
                ToolExecutionResult.success(
                    executionId = pendingResult.executionId,
                    toolName = toolName,
                    content = initialResult.content,
                    startTime = startTime,
                    endTime = endTime,
                    metadata = initialResult.metadata + mapOf("sessionId" to sessionId)
                )
            }
            is ToolResult.Error -> {
                // Process failed
                val endTime = Clock.System.now().toEpochMilliseconds()
                ToolExecutionResult.failure(
                    executionId = pendingResult.executionId,
                    toolName = toolName,
                    error = initialResult.message,
                    startTime = startTime,
                    endTime = endTime,
                    metadata = initialResult.metadata + mapOf("sessionId" to sessionId)
                )
            }
            is ToolResult.Pending -> {
                // Process is still running after initial timeout
                // Return a special result to inform the AI
                val elapsedSeconds = (Clock.System.now().toEpochMilliseconds() - startTime) / 1000
                val stillRunningMessage = buildString {
                    appendLine("⏳ Process is still running after ${elapsedSeconds}s")
                    appendLine("Command: $command")
                    appendLine("Session ID: $sessionId")
                    appendLine()
                    appendLine("The process is executing in the background. You can:")
                    appendLine("1. Continue with other tasks while waiting")
                    appendLine("2. Check the terminal output in the UI for real-time progress")
                    appendLine("3. The result will be available when the process completes")
                }

                // Return as a "success" with the still-running message
                // This allows the agent to continue and make decisions
                val endTime = Clock.System.now().toEpochMilliseconds()
                ToolExecutionResult(
                    executionId = pendingResult.executionId,
                    toolName = toolName,
                    result = ToolResult.Success(
                        content = stillRunningMessage,
                        metadata = mapOf(
                            "status" to "still_running",
                            "sessionId" to sessionId,
                            "command" to command,
                            "elapsedSeconds" to elapsedSeconds.toString()
                        )
                    ),
                    startTime = startTime,
                    endTime = endTime,
                    state = ToolExecutionState.Executing(pendingResult.executionId, startTime),
                    metadata = mapOf(
                        "sessionId" to sessionId,
                        "isAsync" to "true",
                        "stillRunning" to "true"
                    )
                )
            }
            is ToolResult.AgentResult -> {
                // Unexpected, but handle it
                val endTime = Clock.System.now().toEpochMilliseconds()
                ToolExecutionResult(
                    executionId = pendingResult.executionId,
                    toolName = toolName,
                    result = initialResult,
                    startTime = startTime,
                    endTime = endTime,
                    state = if (initialResult.success) {
                        ToolExecutionState.Success(pendingResult.executionId, initialResult, endTime - startTime)
                    } else {
                        ToolExecutionState.Failed(pendingResult.executionId, initialResult.content, endTime - startTime)
                    },
                    metadata = mapOf("sessionId" to sessionId)
                )
            }
        }
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

        // 对于用户取消的命令，不需要分析输出
        // 用户取消是明确的意图，不需要对取消前的输出做分析
        val wasCancelledByUser = executionResult.metadata["cancelled"] == "true"
        if (wasCancelledByUser) {
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

    /**
     * Render plan summary bar if a plan is available
     */
    private fun renderPlanSummaryIfAvailable() {
        val planStateService = toolOrchestrator.getPlanStateService() ?: return
        val currentPlan = planStateService.currentPlan.value ?: return
        val summary = PlanSummaryData.from(currentPlan)
        renderer.renderPlanSummary(summary)
    }
}
