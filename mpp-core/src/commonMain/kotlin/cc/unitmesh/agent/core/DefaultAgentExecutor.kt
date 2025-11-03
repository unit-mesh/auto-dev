package cc.unitmesh.agent.core

import cc.unitmesh.agent.communication.AgentChannel
import cc.unitmesh.agent.communication.AgentEvent
import cc.unitmesh.agent.model.*
import cc.unitmesh.agent.parser.ToolCallParser
import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.flow.catch
import kotlinx.datetime.Clock

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
            var currentPrompt = buildInitialPrompt(definition, context)

            while (turnCount < maxTurns && activeAgents[context.agentId] == true) {
                turnCount++

                val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
                val maxTimeMs = definition.runConfig.maxTimeMinutes * 60 * 1000
                if (elapsed > maxTimeMs) {
                    return AgentResult.Failure(
                        error = "Agent execution timeout after ${definition.runConfig.maxTimeMinutes} minutes",
                        terminateReason = TerminateReason.TIMEOUT,
                        steps = steps
                    )
                }

                onActivity(AgentActivity.Progress("Turn $turnCount/$maxTurns"))
                channel?.emit(AgentEvent.Progress(turnCount, maxTurns, "Processing..."))

                val response = try {
                    val responseText = StringBuilder()
                    llmService.streamPrompt(
                        userPrompt = currentPrompt,
                        fileSystem = cc.unitmesh.devins.filesystem.EmptyFileSystem(),
                        historyMessages = emptyList(),
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
                    steps.add(
                        AgentStep(
                            step = turnCount,
                            action = "llm_call",
                            result = "Error: ${e.message}",
                            success = false
                        )
                    )

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
                    steps.add(
                        AgentStep(
                            step = turnCount,
                            action = "complete_task",
                            result = result,
                            success = true
                        )
                    )

                    onActivity(AgentActivity.TaskComplete(result))
                    channel?.emit(AgentEvent.TaskComplete(result))

                    return AgentResult.Success(
                        output = mapOf("result" to result),
                        terminateReason = TerminateReason.GOAL,
                        steps = steps
                    )
                }

                val toolCalls = extractToolCalls(response)

                if (toolCalls.isEmpty()) {
                    steps.add(
                        AgentStep(
                            step = turnCount,
                            action = "reasoning",
                            result = response.take(200),
                            success = true
                        )
                    )

                    currentPrompt = "Continue with the task. Previous response: ${response.take(500)}"
                } else {
                    steps.add(
                        AgentStep(
                            step = turnCount,
                            action = "tool_calls",
                            result = "Executed ${toolCalls.size} tool(s)",
                            success = true
                        )
                    )

                    currentPrompt = "Tool results received. Continue with the task."
                }
            }

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

    private fun buildInitialPrompt(definition: AgentDefinition, context: AgentContext): String {
        return ""
    }

    private fun isTaskComplete(response: String): Boolean {
        TODO()
    }

    private fun extractFinalResult(response: String): String {
        TODO()
    }

    private fun extractToolCalls(response: String): List<ToolCall> {
        return ToolCallParser().parseToolCalls(response)
    }
}

