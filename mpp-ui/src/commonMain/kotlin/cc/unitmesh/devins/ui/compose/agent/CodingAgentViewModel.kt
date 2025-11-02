package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.communication.AgentChannel
import cc.unitmesh.agent.communication.AgentEvent
import cc.unitmesh.agent.communication.AgentSubmission
import cc.unitmesh.agent.core.DefaultAgentExecutor
import cc.unitmesh.agent.model.*
import cc.unitmesh.agent.subagent.ErrorRecoveryAgent
import cc.unitmesh.agent.subagent.LogSummaryAgent
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Compose ViewModel for Coding Agent
 * 
 * Manages the state and lifecycle of a coding agent, including:
 * - Agent execution
 * - Sub-agent (ErrorRecovery, LogSummary) orchestration
 * - UI state updates
 * - Event streaming
 */
class CodingAgentViewModel(
    private val llmService: KoogLLMService,
    private val projectPath: String,
    private val modelName: String = "gpt-4"  // Model name for agent definition
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val channel = AgentChannel()
    
    // Agent components
    private val agentExecutor = DefaultAgentExecutor(llmService, channel)
    private val errorRecoveryAgent = ErrorRecoveryAgent(projectPath, llmService)
    private val logSummaryAgent = LogSummaryAgent(llmService)
    
    // UI State
    var state by mutableStateOf(CodingAgentState())
        private set
    
    // Event stream for UI
    private val _events = MutableSharedFlow<AgentEvent>(replay = 0, extraBufferCapacity = 100)
    val events: SharedFlow<AgentEvent> = _events.asSharedFlow()
    
    init {
        // Listen to agent events
        scope.launch {
            channel.events().collect { event ->
                handleAgentEvent(event)
            }
        }
        
        // Listen to agent submissions
        scope.launch {
            channel.submissions().collect { submission ->
                handleAgentSubmission(submission)
            }
        }
    }
    
    /**
     * Execute a coding task
     */
    fun executeTask(task: String, sessionId: String? = null) {
        if (state.isExecuting) {
            println("Agent is already executing")
            return
        }
        
        state = state.copy(
            isExecuting = true,
            error = null,
            currentTask = task
        )
        
        scope.launch {
            try {
                val definition = createCodingAgentDefinition()
                val context = AgentContext.create(
                    agentName = "coding-agent",
                    sessionId = sessionId ?: "session-${System.currentTimeMillis()}",
                    inputs = mapOf("requirement" to task),
                    projectPath = projectPath
                )
                
                val result = agentExecutor.execute(definition, context) { activity ->
                    scope.launch {
                        handleAgentActivity(activity)
                    }
                }
                
                when (result) {
                    is AgentResult.Success -> {
                        state = state.copy(
                            isExecuting = false,
                            lastResult = result.output.toString()
                        )
                        _events.emit(AgentEvent.TaskComplete(result.output.toString()))
                    }
                    is AgentResult.Failure -> {
                        state = state.copy(
                            isExecuting = false,
                            error = result.error
                        )
                        _events.emit(AgentEvent.Error(result.error))
                    }
                }
            } catch (e: Exception) {
                state = state.copy(
                    isExecuting = false,
                    error = e.message ?: "Unknown error"
                )
                _events.emit(AgentEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }
    
    /**
     * Execute Error Recovery SubAgent
     */
    fun recoverFromError(command: String, errorMessage: String, exitCode: Int = 1) {
        if (state.isExecuting) {
            println("Agent is already executing")
            return
        }
        
        state = state.copy(
            isExecuting = true,
            error = null,
            currentTask = "Error Recovery: $command"
        )
        
        scope.launch {
            try {
                val input = mapOf(
                    "command" to command,
                    "errorMessage" to errorMessage,
                    "exitCode" to exitCode
                )
                
                val result = errorRecoveryAgent.run(input) { progress ->
                    scope.launch {
                        _events.emit(AgentEvent.Progress(0, 0, progress))
                    }
                }
                
                state = state.copy(
                    isExecuting = false,
                    lastResult = result
                )
                _events.emit(AgentEvent.TaskComplete(result))
            } catch (e: Exception) {
                state = state.copy(
                    isExecuting = false,
                    error = e.message ?: "Unknown error"
                )
                _events.emit(AgentEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }
    
    /**
     * Execute Log Summary SubAgent
     */
    fun summarizeLog(command: String, output: String, exitCode: Int = 0, executionTime: Int = 0) {
        if (state.isExecuting) {
            println("Agent is already executing")
            return
        }
        
        // Check if summarization is needed
        if (!logSummaryAgent.needsSummarization(output)) {
            println("Output is short enough, no summarization needed")
            return
        }
        
        state = state.copy(
            isExecuting = true,
            error = null,
            currentTask = "Log Summary: $command"
        )
        
        scope.launch {
            try {
                val input = mapOf(
                    "command" to command,
                    "output" to output,
                    "exitCode" to exitCode,
                    "executionTime" to executionTime
                )
                
                val result = logSummaryAgent.run(input) { progress ->
                    scope.launch {
                        _events.emit(AgentEvent.Progress(0, 0, progress))
                    }
                }
                
                state = state.copy(
                    isExecuting = false,
                    lastResult = result
                )
                _events.emit(AgentEvent.TaskComplete(result))
            } catch (e: Exception) {
                state = state.copy(
                    isExecuting = false,
                    error = e.message ?: "Unknown error"
                )
                _events.emit(AgentEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }
    
    /**
     * Cancel current task
     */
    fun cancelTask() {
        scope.launch {
            // TODO: Implement proper cancellation
            state = state.copy(
                isExecuting = false,
                error = "Task cancelled by user"
            )
            _events.emit(AgentEvent.Error("Task cancelled by user"))
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        state = state.copy(error = null)
    }
    
    /**
     * Dispose resources
     */
    fun dispose() {
        scope.cancel()
    }
    
    // Private helper methods
    
    private suspend fun handleAgentEvent(event: AgentEvent) {
        when (event) {
            is AgentEvent.StreamUpdate -> {
                state = state.copy(
                    streamingText = state.streamingText + event.text
                )
            }
            is AgentEvent.Progress -> {
                state = state.copy(
                    progress = event.message
                )
            }
            is AgentEvent.TaskComplete -> {
                // Already handled in executeTask
            }
            is AgentEvent.Error -> {
                // Already handled in executeTask
            }
            else -> {}
        }
        
        // Forward to UI
        _events.emit(event)
    }
    
    private suspend fun handleAgentActivity(activity: AgentActivity) {
        when (activity) {
            is AgentActivity.StreamUpdate -> {
                state = state.copy(
                    streamingText = state.streamingText + activity.text
                )
            }
            is AgentActivity.Progress -> {
                state = state.copy(
                    progress = activity.message
                )
            }
            is AgentActivity.ToolCallStart -> {
                state = state.copy(
                    currentToolCall = "Calling ${activity.toolName}..."
                )
            }
            is AgentActivity.ToolCallEnd -> {
                state = state.copy(
                    currentToolCall = null
                )
            }
            is AgentActivity.TaskComplete -> {
                // Already handled in executeTask
            }
            else -> {}
        }
    }
    
    private suspend fun handleAgentSubmission(submission: AgentSubmission) {
        when (submission) {
            is AgentSubmission.SendPrompt -> {
                // Handle prompt submission
            }
            is AgentSubmission.CancelTask -> {
                cancelTask()
            }
            else -> {}
        }
    }
    
    private fun createCodingAgentDefinition(): AgentDefinition {
        return AgentDefinition(
            name = "coding-agent",
            displayName = "Coding Agent",
            description = "An AI agent that helps with coding tasks",
            promptConfig = PromptConfig(
                systemPrompt = """
                    You are an expert software engineer and helpful coding assistant.
                    You help users complete coding tasks by:
                    - Understanding requirements
                    - Writing clean, maintainable code
                    - Following best practices
                    - Testing your solutions
                    
                    When you complete a task, say "TASK_COMPLETE" followed by a summary.
                """.trimIndent(),
                queryTemplate = "Requirement: \${requirement}"
            ),
            modelConfig = ModelConfig(
                modelId = modelName
            ),
            runConfig = RunConfig(
                maxTurns = 20,
                maxTimeMinutes = 10,
                terminateOnError = false
            )
        )
    }
}

/**
 * UI State for Coding Agent
 */
data class CodingAgentState(
    val isExecuting: Boolean = false,
    val currentTask: String? = null,
    val progress: String? = null,
    val streamingText: String = "",
    val currentToolCall: String? = null,
    val lastResult: String? = null,
    val error: String? = null
)

