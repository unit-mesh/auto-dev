package cc.unitmesh.agent.executor

import cc.unitmesh.agent.*
import cc.unitmesh.agent.conversation.ConversationManager
import cc.unitmesh.agent.tool.ToolResultFormatter
import cc.unitmesh.agent.orchestrator.ToolExecutionResult
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.parser.ToolCallParser
import cc.unitmesh.agent.recovery.ErrorRecoveryManager
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.agent.state.ToolExecutionState
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.toToolType
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.yield
import kotlinx.datetime.Clock
import cc.unitmesh.agent.orchestrator.ToolExecutionContext as OrchestratorContext

class CodingAgentExecutor(
    private val projectPath: String,
    private val llmService: KoogLLMService,
    private val toolOrchestrator: ToolOrchestrator,
    private val renderer: CodingAgentRenderer,
    private val maxIterations: Int = 100
) {
    private val toolCallParser = ToolCallParser()
    private val errorRecoveryManager = ErrorRecoveryManager(projectPath, llmService)
    private var currentIteration = 0
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
        val conversationManager = ConversationManager(llmService, systemPrompt)
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
                renderer.renderLLMResponseStart()

                val messageToSend = if (currentIteration == 1) {
                    initialUserMessage
                } else {
                    buildContinuationMessage()
                }

                conversationManager.sendMessage(messageToSend).cancellable().collect { chunk ->
                    llmResponse.append(chunk)
                    renderer.renderLLMResponseChunk(chunk)
                }

                renderer.renderLLMResponseEnd()
                conversationManager.addAssistantResponse(llmResponse.toString())

            } catch (e: Exception) {
                renderer.renderError("LLM call failed: ${e.message}")
                break
            }

            val toolCalls = toolCallParser.parseToolCalls(llmResponse.toString())
            if (toolCalls.isEmpty()) {
                renderer.renderTaskComplete()
                break
            }

            val toolResults = executeToolCalls(toolCalls)
            val toolResultsText = ToolResultFormatter.formatMultipleToolResults(toolResults)
            conversationManager.addToolResults(toolResultsText)

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

    private fun shouldContinue(): Boolean {
        return currentIteration < maxIterations
    }

    private fun buildInitialUserMessage(task: AgentTask): String {
        return "Task: ${task.requirement}\n\n" +
                "Please analyze this task and use the available DevIns tools to complete it. " +
                "Use tools like /read-file, /write-file, /shell, etc. as needed."
    }

    private fun buildContinuationMessage(): String {
        return "Please continue with the task based on the tool execution results above. " +
                "Use additional tools if needed, or summarize if the task is complete."
    }

    private suspend fun executeToolCalls(toolCalls: List<ToolCall>): List<Triple<String, Map<String, Any>, ToolExecutionResult>> {
        val results =
            mutableListOf<Triple<String, Map<String, Any>, ToolExecutionResult>>()

        for ((index, toolCall) in toolCalls.withIndex()) {
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
                    "read-file", "write-file" -> 3
                    "shell" -> 2
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
                break
            }

            renderer.renderToolCall(toolName, paramsStr)
            yield()

            // 执行工具
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

            renderer.renderToolResult(toolName, stepResult.success, stepResult.result, fullOutput)

            val currentToolType = toolName.toToolType()
            if ((currentToolType == ToolType.WriteFile) && executionResult.isSuccess) {
                recordFileEdit(params)
            }

            if (!executionResult.isSuccess) {
                val command = if (toolName == "shell") params["command"] as? String else null
                val recoveryResult = errorRecoveryManager.handleToolError(
                    toolName = toolName,
                    command = command,
                    errorMessage = executionResult.content ?: "Unknown error"
                )

                if (recoveryResult != null) {
                    // 将恢复建议添加到对话历史中
                    // 这将在下一轮迭代中被使用
                    // 注意：这里不直接修改对话历史，而是让调用者处理
                }

                if (errorRecoveryManager.isFatalError(toolName, executionResult.content ?: "")) {
                    renderer.renderError("Fatal error encountered. Stopping execution.")
                    break
                }
            }
        }

        return results
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

        renderer.renderFinalResult(success, message, currentIteration)

        return AgentResult(
            success = success,
            message = message,
            steps = steps,
            edits = edits
        )
    }
}
