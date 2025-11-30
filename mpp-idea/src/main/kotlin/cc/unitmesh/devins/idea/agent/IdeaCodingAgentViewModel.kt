package cc.unitmesh.devins.idea.agent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the state of the message input.
 */
sealed class MessageInputState {
    abstract val inputText: String

    data object Disabled : MessageInputState() {
        override val inputText: String = ""
    }

    data class Enabled(override val inputText: String) : MessageInputState()
    data class Sending(override val inputText: String) : MessageInputState()
    data class SendFailed(override val inputText: String, val error: Throwable) : MessageInputState()
}

/**
 * ViewModel for the IDEA CodingAgent interface.
 *
 * Manages the agent execution, timeline rendering, and UI state.
 * This is the IntelliJ IDEA version of CodingAgentViewModel from mpp-ui.
 */
class IdeaCodingAgentViewModel(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
    private val maxIterations: Int = 100
) : Disposable {

    val renderer = IdeaAgentRenderer()

    private val _inputState = MutableStateFlow<MessageInputState>(MessageInputState.Disabled)
    val inputState: StateFlow<MessageInputState> = _inputState.asStateFlow()

    var isExecuting by mutableStateOf(false)
        private set

    private var currentExecutionJob: Job? = null

    // Configuration state - TODO: integrate with actual LLM service
    private var isConfigured = false

    /**
     * Update the input text.
     */
    fun onInputChanged(text: String) {
        _inputState.value = when {
            _inputState.value is MessageInputState.Sending -> MessageInputState.Sending(text)
            text.isEmpty() -> MessageInputState.Disabled
            else -> MessageInputState.Enabled(text)
        }
    }

    /**
     * Execute a task with the given prompt.
     */
    fun executeTask(task: String, onConfigRequired: (() -> Unit)? = null) {
        if (isExecuting) return

        if (task.isBlank()) return

        // Handle built-in commands
        if (task.trim().startsWith("/")) {
            handleBuiltinCommand(task.trim())
            return
        }

        isExecuting = true
        renderer.addUserMessage(task)

        currentExecutionJob = coroutineScope.launch {
            try {
                // TODO: Integrate with mpp-core CodingAgent
                // For now, simulate agent execution
                simulateAgentExecution(task)
            } catch (e: CancellationException) {
                renderer.forceStop()
                renderer.addError("Task cancelled by user")
            } catch (e: Exception) {
                renderer.addError(e.message ?: "Unknown error")
            } finally {
                isExecuting = false
                currentExecutionJob = null
            }
        }
    }

    /**
     * Cancel the current task execution.
     */
    fun cancelTask() {
        currentExecutionJob?.cancel()
        currentExecutionJob = null
        isExecuting = false
        renderer.forceStop()
    }

    /**
     * Handle built-in commands like /clear, /help.
     */
    private fun handleBuiltinCommand(command: String) {
        when {
            command == "/clear" -> {
                renderer.clear()
            }
            command == "/help" -> {
                renderer.addUserMessage(command)
                renderer.addAssistantMessage("""
                    |**Available Commands:**
                    |
                    |- `/clear` - Clear the chat history
                    |- `/help` - Show this help message
                    |
                    |**Tips:**
                    |- Describe your coding task in natural language
                    |- The agent will analyze, plan, and execute the task
                """.trimMargin())
            }
            else -> {
                renderer.addUserMessage(command)
                renderer.addError("Unknown command: $command")
            }
        }
    }

    /**
     * Simulate agent execution for testing.
     * TODO: Replace with actual mpp-core CodingAgent integration.
     */
    private suspend fun simulateAgentExecution(task: String) {
        renderer.startStreaming()
        
        // Simulate thinking
        val thinking = "I'll analyze your request and create a plan..."
        for (char in thinking) {
            renderer.appendStreamingContent(char.toString())
            delay(20)
        }
        renderer.endStreaming()

        // Simulate tool call
        renderer.startToolCall("ReadFile", "Reading project files", "src/main/kotlin/...")
        delay(500)
        renderer.completeToolCall(true, "Read 3 files", "File contents...", 450)

        // Simulate response
        renderer.startStreaming()
        val response = "\n\nBased on my analysis, I've completed the following:\n\n1. Analyzed the codebase\n2. Identified the relevant files\n3. Made the necessary changes\n\nThe task has been completed successfully."
        for (char in response) {
            renderer.appendStreamingContent(char.toString())
            delay(15)
        }
        renderer.endStreaming()

        renderer.addTaskComplete(true, "Task completed in 2 iterations")
    }

    /**
     * Create a new conversation.
     */
    fun onNewConversation() {
        cancelTask()
        renderer.clear()
        _inputState.value = MessageInputState.Disabled
    }

    override fun dispose() {
        currentExecutionJob?.cancel()
        coroutineScope.cancel()
    }
}

