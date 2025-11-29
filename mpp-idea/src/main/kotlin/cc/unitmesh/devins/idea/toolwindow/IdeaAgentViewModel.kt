package cc.unitmesh.devins.idea.toolwindow

import cc.unitmesh.devins.idea.model.AgentType
import cc.unitmesh.devins.idea.model.ChatMessage as ModelChatMessage
import cc.unitmesh.devins.idea.model.LLMConfig
import cc.unitmesh.devins.idea.model.MessageRole
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Agent ToolWindow.
 * Manages agent type tabs, chat messages, and LLM integration.
 */
class IdeaAgentViewModel(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) : Disposable {

    // Current agent type tab
    private val _currentAgentType = MutableStateFlow(AgentType.CODING)
    val currentAgentType: StateFlow<AgentType> = _currentAgentType.asStateFlow()

    // Chat messages
    private val _messages = MutableStateFlow<List<ModelChatMessage>>(emptyList())
    val messages: StateFlow<List<ModelChatMessage>> = _messages.asStateFlow()

    // Current streaming output
    private val _streamingOutput = MutableStateFlow("")
    val streamingOutput: StateFlow<String> = _streamingOutput.asStateFlow()

    // Is processing a request
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // LLM Configuration
    private val _llmConfig = MutableStateFlow(LLMConfig())
    val llmConfig: StateFlow<LLMConfig> = _llmConfig.asStateFlow()

    // Show config dialog
    private val _showConfigDialog = MutableStateFlow(false)
    val showConfigDialog: StateFlow<Boolean> = _showConfigDialog.asStateFlow()

    // Current streaming job (for cancellation)
    private var currentJob: Job? = null

    /**
     * Change the current agent type tab.
     */
    fun onAgentTypeChange(agentType: AgentType) {
        _currentAgentType.value = agentType
    }

    /**
     * Send a message to the LLM.
     */
    fun sendMessage(content: String) {
        if (content.isBlank() || _isProcessing.value) return

        // Add user message
        val userMessage = ModelChatMessage(
            content = content,
            role = MessageRole.USER
        )
        _messages.value = _messages.value + listOf(userMessage)

        // Start processing
        _isProcessing.value = true
        _streamingOutput.value = ""

        currentJob = coroutineScope.launch {
            try {
                // TODO: Integrate with actual LLM service
                // For now, simulate a response
                simulateResponse(content)
            } catch (e: CancellationException) {
                // Cancelled by user
            } catch (e: Exception) {
                val errorMessage = ModelChatMessage(
                    content = "Error: ${e.message}",
                    role = MessageRole.ASSISTANT
                )
                _messages.value = _messages.value + listOf(errorMessage)
            } finally {
                _isProcessing.value = false
                _streamingOutput.value = ""
            }
        }
    }

    private suspend fun simulateResponse(userMessage: String) {
        val response = """
            This is a simulated response for: "$userMessage"
            
            To enable real LLM responses, please configure your API key in the settings.
            
            Supported features:
            - **Agentic**: Full coding agent with file operations
            - **Review**: Code review and analysis
            - **Knowledge**: Document reading and Q&A
            - **Remote**: Connect to remote mpp-server
        """.trimIndent()

        // Simulate streaming
        for (char in response) {
            if (!coroutineScope.isActive) break
            _streamingOutput.value += char
            delay(10)
        }

        // Add final message
        val assistantMessage = ModelChatMessage(
            content = response,
            role = MessageRole.ASSISTANT
        )
        _messages.value = _messages.value + listOf(assistantMessage)
    }

    /**
     * Abort the current request.
     * Preserves partial streaming output if any.
     */
    fun abortRequest() {
        currentJob?.cancel()
        // Preserve partial output if any
        if (_streamingOutput.value.isNotEmpty()) {
            val partialMessage = ModelChatMessage(
                content = _streamingOutput.value + "\n\n[Interrupted]",
                role = MessageRole.ASSISTANT
            )
            _messages.value = _messages.value + listOf(partialMessage)
        }
        _isProcessing.value = false
        _streamingOutput.value = ""
    }

    /**
     * Clear chat history.
     */
    fun clearHistory() {
        _messages.value = emptyList()
        _streamingOutput.value = ""
    }

    /**
     * Update LLM configuration.
     */
    fun updateLLMConfig(config: LLMConfig) {
        _llmConfig.value = config
    }

    /**
     * Show/hide config dialog.
     */
    fun setShowConfigDialog(show: Boolean) {
        _showConfigDialog.value = show
    }

    override fun dispose() {
        currentJob?.cancel()
        coroutineScope.cancel()
    }
}

