package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.*

/**
 * Compose ViewModel for Coding Agent
 *
 * Uses the new BaseRenderer architecture with ComposeRenderer
 * for consistent rendering across CLI, TUI, and Compose UI
 */
class CodingAgentViewModel(
    private val llmService: KoogLLMService,
    private val projectPath: String,
    private val maxIterations: Int = 100
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val renderer = ComposeRenderer()

    private val codingAgent =
        CodingAgent(
            projectPath = projectPath,
            llmService = llmService,
            maxIterations = maxIterations,
            renderer = renderer
        )

    var isExecuting by mutableStateOf(false)
        private set
    private var currentExecutionJob: Job? = null

    fun executeTask(task: String) {
        if (isExecuting) {
            println("Agent is already executing")
            return
        }

        isExecuting = true
        renderer.clearError()
        renderer.addUserMessage(task)

        currentExecutionJob =
            scope.launch {
                try {
                    val agentTask =
                        AgentTask(
                            requirement = task,
                            projectPath = projectPath
                        )

                    val result = codingAgent.executeTask(agentTask)

                    // Result is already handled by the renderer
                    isExecuting = false
                    currentExecutionJob = null
                } catch (e: CancellationException) {
                    // Task was cancelled - reset all states and add cancellation message at the end
                    renderer.forceStop() // Stop all loading states

                    // Add cancellation message to timeline (will appear at the end)
                    renderer.renderError("Task cancelled by user")
                    isExecuting = false
                    currentExecutionJob = null
                } catch (e: Exception) {
                    renderer.renderError(e.message ?: "Unknown error")
                    isExecuting = false
                    currentExecutionJob = null
                }
            }
    }

    /**
     * Cancel current task
     */
    fun cancelTask() {
        if (isExecuting && currentExecutionJob != null) {
            currentExecutionJob?.cancel("Task cancelled by user")
            currentExecutionJob = null
            isExecuting = false
        }
    }

    /**
     * Clear chat history
     */
    fun clearHistory() {
        renderer.clearMessages()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        renderer.clearError()
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        scope.cancel()
    }
}
