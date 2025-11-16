package cc.unitmesh.agent.executor

import cc.unitmesh.agent.conversation.ConversationManager
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.parser.ToolCallParser
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.flow.cancellable

/**
 * Base class for agent executors
 * Provides common functionality for executing agent tasks with tool calling support
 */
abstract class BaseAgentExecutor(
    protected val projectPath: String,
    protected val llmService: KoogLLMService,
    protected val toolOrchestrator: ToolOrchestrator,
    protected val renderer: CodingAgentRenderer,
    protected val maxIterations: Int,
    protected val enableLLMStreaming: Boolean = true
) {
    protected val toolCallParser = ToolCallParser()
    protected var currentIteration = 0
    protected var conversationManager: ConversationManager? = null

    /**
     * Check if executor should continue iterations
     */
    protected fun shouldContinue(): Boolean {
        return currentIteration < maxIterations
    }

    /**
     * Build a continuation message for the agent
     */
    protected open fun buildContinuationMessage(): String {
        return "Please continue based on the tool execution results above. " +
                "Use additional tools if needed, or provide your final response if you have all the information."
    }

    /**
     * Get LLM response with streaming support
     * 
     * @param userMessage The message to send to LLM
     * @param compileDevIns Whether to compile DevIns commands
     * @param onChunk Callback for each chunk of streamed response
     * @return Complete LLM response
     */
    protected suspend fun getLLMResponse(
        userMessage: String,
        compileDevIns: Boolean = true,
        onChunk: (String) -> Unit = {}
    ): String {
        val llmResponse = StringBuilder()
        
        renderer.renderLLMResponseStart()
        
        try {
            if (enableLLMStreaming) {
                conversationManager!!.sendMessage(userMessage, compileDevIns).cancellable().collect { chunk ->
                    llmResponse.append(chunk)
                    renderer.renderLLMResponseChunk(chunk)
                    onChunk(chunk)
                }
            } else {
                val response = llmService.sendPrompt(userMessage)
                llmResponse.append(response)
                // Simulate streaming for consistent rendering
                response.split(Regex("(?<=[.!?。！？]\\s)")).forEach { sentence ->
                    if (sentence.isNotBlank()) {
                        renderer.renderLLMResponseChunk(sentence)
                        onChunk(sentence)
                    }
                }
            }
            
            renderer.renderLLMResponseEnd()
            conversationManager!!.addAssistantResponse(llmResponse.toString())
            
            return llmResponse.toString()
        } catch (e: Exception) {
            renderer.renderError("LLM call failed: ${e.message}")
            throw e
        }
    }

    /**
     * Check if a completion indicator is present in the response
     */
    protected fun hasCompletionIndicator(response: String, indicators: List<String>): Boolean {
        val lowerResponse = response.lowercase()
        return indicators.any { lowerResponse.contains(it) }
    }
}

