package cc.unitmesh.devins.idea.toolwindow

import com.intellij.openapi.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
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
 * Represents a chat message.
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ViewModel for the AutoDev Chat interface.
 *
 * Manages chat messages, user input state, and message sending.
 */
class AutoDevChatViewModel(
    private val coroutineScope: CoroutineScope
) : Disposable {

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _inputState = MutableStateFlow<MessageInputState>(MessageInputState.Disabled)
    val inputState: StateFlow<MessageInputState> = _inputState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
     * Send the current message.
     */
    fun onSendMessage() {
        val currentText = _inputState.value.inputText
        if (currentText.isBlank()) return

        // Add user message
        val userMessage = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            content = currentText,
            isUser = true
        )
        _chatMessages.value = _chatMessages.value + userMessage
        _inputState.value = MessageInputState.Sending("")

        // TODO: Integrate with mpp-core for AI responses
        // For now, add a placeholder response
        val aiResponse = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            content = "This is a placeholder response. AI integration coming soon!",
            isUser = false
        )
        _chatMessages.value = _chatMessages.value + aiResponse
        _inputState.value = MessageInputState.Disabled
    }

    /**
     * Abort the current message sending.
     */
    fun onAbortMessage() {
        _inputState.value = when (val text = _inputState.value.inputText) {
            "" -> MessageInputState.Disabled
            else -> MessageInputState.Enabled(text)
        }
    }

    /**
     * Create a new conversation.
     */
    fun onNewConversation() {
        _chatMessages.value = emptyList()
        _inputState.value = MessageInputState.Disabled
    }

    override fun dispose() {
        coroutineScope.cancel()
    }
}

