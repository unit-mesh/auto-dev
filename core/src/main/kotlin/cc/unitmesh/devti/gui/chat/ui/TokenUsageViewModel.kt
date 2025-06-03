package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.llm2.TokenUsageEvent
import cc.unitmesh.devti.llm2.TokenUsageListener
import cc.unitmesh.devti.llm2.TokenUsageService
import cc.unitmesh.devti.llms.custom.Usage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection

/**
 * ViewModel for token usage data management and business logic
 * Separates business logic from UI concerns
 */
class TokenUsageViewModel(private val project: Project) {
    private val tokenUsageService = project.service<TokenUsageService>()
    private var messageBusConnection: MessageBusConnection? = null
    
    // Observable properties
    private var onTokenUsageUpdated: ((TokenUsageData) -> Unit)? = null
    
    data class TokenUsageData(
        val usage: Usage,
        val model: String?,
        val maxContextWindowTokens: Long,
        val usageRatio: Double,
        val isVisible: Boolean
    )
    
    init {
        setupTokenUsageListener()
    }
    
    private fun setupTokenUsageListener() {
        val messageBus = ApplicationManager.getApplication().messageBus
        messageBusConnection = messageBus.connect()
        messageBusConnection?.subscribe(TokenUsageListener.TOPIC, object : TokenUsageListener {
            override fun onTokenUsage(event: TokenUsageEvent) {
                updateTokenUsage(event)
            }
        })
    }
    
    private fun updateTokenUsage(event: TokenUsageEvent) {
        ApplicationManager.getApplication().invokeLater {
            val maxTokens = getMaxContextWindowTokens()
            val totalTokens = event.usage.totalTokens ?: 0
            val usageRatio = if (maxTokens > 0) {
                totalTokens.toDouble() / maxTokens
            } else {
                0.0
            }
            
            val tokenUsageData = TokenUsageData(
                usage = event.usage,
                model = event.model,
                maxContextWindowTokens = maxTokens,
                usageRatio = usageRatio,
                isVisible = !event.model.isNullOrBlank() && maxTokens > 0
            )
            
            onTokenUsageUpdated?.invoke(tokenUsageData)
        }
    }
    
    private fun getMaxContextWindowTokens(): Long {
        return try {
            tokenUsageService.getUsedMaxToken()
        } catch (e: Exception) {
            4096L // fallback value
        }
    }
    
    fun setOnTokenUsageUpdated(callback: (TokenUsageData) -> Unit) {
        onTokenUsageUpdated = callback
    }
    
    fun getCurrentUsage(): Usage = tokenUsageService.getCurrentConsumedTokens()
    
    fun getCurrentModel(): String? {
        // This would need to be tracked in TokenUsageService or passed from events
        return null // placeholder for now
    }
    
    fun reset() {
        ApplicationManager.getApplication().invokeLater {
            val emptyData = TokenUsageData(
                usage = Usage(),
                model = null,
                maxContextWindowTokens = 0,
                usageRatio = 0.0,
                isVisible = false
            )
            onTokenUsageUpdated?.invoke(emptyData)
        }
    }
    
    fun dispose() {
        messageBusConnection?.disconnect()
        messageBusConnection = null
        onTokenUsageUpdated = null
    }
    
    companion object {
        fun formatTokenCount(count: Long): String {
            return when {
                count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
                count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
                else -> count.toString()
            }
        }
    }
}