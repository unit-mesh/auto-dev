package cc.unitmesh.devti.llm2

import cc.unitmesh.devti.llms.custom.Usage
import com.intellij.util.messages.Topic
import kotlinx.serialization.Serializable

/**
 * Token usage event data that will be sent when SSE stream finishes
 */
@Serializable
data class TokenUsageEvent(
    val usage: Usage,
    val model: String? = null,
    val sessionId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Listener interface for token usage events
 */
interface TokenUsageListener {
    /**
     * Called when token usage data is available from LLM response
     * 
     * @param event The token usage event containing usage statistics
     */
    fun onTokenUsage(event: TokenUsageEvent)
    
    companion object {
        val TOPIC = Topic.create("autodev.llm.token.usage", TokenUsageListener::class.java)
        
        /**
         * Notify all subscribers about token usage
         */
        fun notify(event: TokenUsageEvent) {
            com.intellij.openapi.application.ApplicationManager.getApplication().messageBus
                .syncPublisher(TOPIC)
                .onTokenUsage(event)
        }
    }
}