package cc.unitmesh.devti.llms

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.llm2.ChatSession
import cc.unitmesh.devti.llm2.LLMProvider2
import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llm2.model.ModelType
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.prompting.optimizer.PromptOptimizer
import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow

/**
 * Adapter that bridges the old LLMProvider interface with the new LLMProvider2 implementation.
 * This allows gradual migration from LLMProvider to LLMProvider2 while maintaining backward compatibility.
 */
class LLMProviderAdapter(
    private val project: Project,
    private val modelType: ModelType = ModelType.Default
) : LLMProvider {
    private val logger = logger<LLMProviderAdapter>()
    private val agentService = project.getService(AgentStateService::class.java)

    private val messages: MutableList<Message> = mutableListOf()
    private var currentSession: ChatSession<Message> = ChatSession("adapter-session")
    private var currentProvider: LLMProvider2? = null

    override val defaultTimeout: Long get() = 600

    private fun createProvider2(): LLMProvider2 {
        val llmConfig = when (modelType) {
            ModelType.Default -> LlmConfig.default()
            else -> LlmConfig.forCategory(modelType)
        }

        return LLMProvider2.fromConfig(llmConfig, project)
    }

    override fun stream(
        promptText: String,
        systemPrompt: String,
        keepHistory: Boolean,
        usePlanForFirst: Boolean
    ): Flow<String> {
        logger.info("LLMProviderAdapter.stream called with model type: $modelType")

        // Handle plan model switching logic (from old CustomLLMProvider)
        val actualLlmConfig = if (usePlanForFirst && shouldUsePlanModel()) {
            LlmConfig.forCategory(ModelType.Plan)
        } else {
            when (modelType) {
                ModelType.Default -> LlmConfig.default()
                else -> LlmConfig.forCategory(modelType)
            }
        }

        val actualProvider = LLMProvider2.fromConfig(actualLlmConfig, project)
        currentProvider = actualProvider
        if (!keepHistory || project.coderSetting.state.noChatHistory) {
            clearMessage()
            currentSession = ChatSession("adapter-session")
        }

        if (systemPrompt.isNotEmpty()) {
            if (messages.isNotEmpty() && messages[0].role != "system") {
                messages.add(0, Message("system", systemPrompt))
            } else if (messages.isEmpty()) {
                messages.add(Message("system", systemPrompt))
            } else {
                messages[0] = Message("system", systemPrompt)
            }
        }

        // Process prompt optimization
        val prompt = if (project.coderSetting.state.trimCodeBeforeSend) {
            PromptOptimizer.trimCodeSpace(promptText)
        } else {
            promptText
        }

        messages.add(Message("user", prompt))
        val finalMsgs = agentService.processMessages(messages)
        currentSession = ChatSession("adapter-session", finalMsgs)

        return try {
            kotlinx.coroutines.flow.flow {
                var fullResponse = ""
                var lastEmittedLength = 0

                actualProvider.request(
                    text = Message("user", prompt),
                    stream = true,
                    session = currentSession
                ).collect { sessionItem ->
                    val content = sessionItem.chatMessage.content
                    fullResponse = content

                    // Emit only the new part of the content (incremental)
                    if (fullResponse.length > lastEmittedLength) {
                        val newContent = fullResponse.substring(lastEmittedLength)
                        if (newContent.isNotEmpty()) {
                            emit(newContent)
                            lastEmittedLength = fullResponse.length
                        }
                    }
                }

                // Update message history with the complete response
                if (fullResponse.isNotEmpty()) {
                    messages.add(Message("assistant", fullResponse))
                }
            }
        } catch (e: Exception) {
            logger.error("Error in LLMProviderAdapter.stream", e)
            kotlinx.coroutines.flow.flowOf("Error: ${e.message}")
        }.also {
            // Update internal message history
            if (!keepHistory || project.coderSetting.state.noChatHistory) {
                clearMessage()
            }
        }
    }

    private fun shouldUsePlanModel(): Boolean {
        val canBePlanLength = 3 // System + User + Assistant
        return messages.size == canBePlanLength && LlmConfig.hasPlanModel()
    }

    override fun clearMessage() {
        messages.clear()
        currentSession = ChatSession("adapter-session")
    }

    /**
     * Cancels the current LLM request synchronously without waiting for completion.
     * This is useful for immediately stopping streaming responses from the LLM.
     */
    fun cancelCurrentRequestSync() {
        logger.info("Cancelling current LLM request synchronously")
        currentProvider?.cancelCurrentRequestSync() ?: run {
            logger.warn("No active LLM provider to cancel")
        }
    }

    override fun getAllMessages(): List<Message> {
        return messages.toList()
    }

    override fun appendLocalMessage(msg: String, role: ChatRole) {
        if (msg.isEmpty()) return
        messages.add(Message(role.roleName(), msg))

        // Update current session
        currentSession = ChatSession("adapter-session", messages)
    }
}
