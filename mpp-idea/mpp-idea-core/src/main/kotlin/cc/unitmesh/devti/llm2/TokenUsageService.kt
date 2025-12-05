package cc.unitmesh.devti.llm2

import cc.unitmesh.devti.llms.custom.Usage
import cc.unitmesh.devti.settings.model.LLMModelManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Service for managing token usage statistics and calculations
 */
@Service(Service.Level.PROJECT)
class TokenUsageService(private val project: Project) : TokenUsageListener {
    private val sessionTokenUsage = ConcurrentHashMap<String, Usage>()
    private val totalTokenUsage = AtomicLong(0)
    private val totalPromptTokens = AtomicLong(0)
    private val totalCompletionTokens = AtomicLong(0)
    
    private var currentSessionId: String? = null
    private var currentModel: String? = null
    private var messageBusConnection: MessageBusConnection? = null
    
    init {
        setupTokenUsageListener()
    }
    
    private fun setupTokenUsageListener() {
        messageBusConnection = ApplicationManager.getApplication().messageBus.connect()
        messageBusConnection?.subscribe(TokenUsageListener.TOPIC, this)
    }
    
    override fun onTokenUsage(event: TokenUsageEvent) {
        event.sessionId?.let { sessionId ->
            sessionTokenUsage[sessionId] = event.usage
            currentSessionId = sessionId
        }
        
        // Update current model
        currentModel = event.model
        
        // Update total usage statistics
        totalTokenUsage.addAndGet(event.usage.totalTokens)
        totalPromptTokens.addAndGet(event.usage.promptTokens)
        totalCompletionTokens.addAndGet(event.usage.completionTokens)
    }
    
    /**
     * Get the current consumed tokens for the active session
     */
    fun getCurrentConsumedTokens(): Usage {
        return currentSessionId?.let { sessionId ->
            sessionTokenUsage[sessionId]
        } ?: Usage()
    }
    
    /**
     * Get total consumed tokens across all sessions
     */
    fun getTotalConsumedTokens(): Usage {
        return Usage(
            promptTokens = totalPromptTokens.get(),
            completionTokens = totalCompletionTokens.get(),
            totalTokens = totalTokenUsage.get()
        )
    }
    
    /**
     * Get the maximum tokens allowed for the current model
     */
    fun getUsedMaxToken(): Long {
        return LLMModelManager.getInstance().getUsedMaxToken().maxContextWindowTokens?.toLong() ?: 0L
    }
    
    /**
     * Calculate token availability ratio (0.0 to 1.0)
     * Returns the percentage of tokens still available
     */
    fun calculateTokenAvailability(): Double {
        val maxTokens = getUsedMaxToken()
        if (maxTokens <= 0) return 1.0
        
        val currentUsage = getCurrentConsumedTokens()
        val usedTokens = currentUsage.totalTokens
        
        return if (usedTokens >= maxTokens) {
            0.0
        } else {
            1.0 - (usedTokens.toDouble() / maxTokens.toDouble())
        }
    }
    
    /**
     * Calculate token usage ratio (0.0 to 1.0)
     * Returns the percentage of tokens already used
     */
    fun calculateTokenUsageRatio(): Double {
        return 1.0 - calculateTokenAvailability()
    }
    
    /**
     * Check if token usage is approaching the limit
     * @param threshold Warning threshold (default 0.8 = 80%)
     */
    fun isApproachingTokenLimit(threshold: Double = 0.8): Boolean {
        return calculateTokenUsageRatio() >= threshold
    }
    
    /**
     * Get remaining tokens for current session
     */
    fun getRemainingTokens(): Long {
        val maxTokens = getUsedMaxToken()
        val currentUsage = getCurrentConsumedTokens()
        val remaining = maxTokens - currentUsage.totalTokens
        return maxOf(0L, remaining)
    }

    fun dispose() {
        messageBusConnection?.disconnect()
        messageBusConnection = null
    }
    
    companion object {
        fun getInstance(project: Project): TokenUsageService {
            return project.getService(TokenUsageService::class.java)
        }
    }
}