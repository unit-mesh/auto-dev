package cc.unitmesh.devti.counit

import cc.unitmesh.devti.counit.model.CustomAgentConfig
import cc.unitmesh.devti.counit.model.ResponseAction
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.util.LLMCoroutineScope
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class CustomAgentChatProcessor(val project: Project) {
    private val llmFactory = LlmFactory()

    private val customAgentExecutor = CustomAgentExecutor(project)
    private val llmProvider = llmFactory.create(project)

    fun handleChat(prompter: ContextPrompter, ui: ChatCodingPanel, context: ChatContext?) {
        val originPrompt = prompter.requestPrompt()
        ui.addMessage(originPrompt, true, originPrompt)

        val request = originPrompt.trim()
        val selectedAgent: CustomAgentConfig = ui.getSelectedCustomAgent()

        val response = customAgentExecutor.execute(request, selectedAgent)
        if (response == null) {
            logger.error("error for custom agent: $selectedAgent with request: $request")
            return
        }

        when (selectedAgent.responseAction) {
            ResponseAction.Direct -> {
                ui.addMessage(response, true, response)
            }

            ResponseAction.TextChunk -> {
                ui.setInput(response)
            }

            ResponseAction.Flow -> {
                ui.addMessage(response, true, response)

                // loading
                LLMCoroutineScope.scope(project).launch {
                    llmProvider.appendLocalMessage(response, ChatRole.User)

                    val intentionFlow = llmProvider.stream(response, "")
                    val result = ui.updateMessage(intentionFlow)

                    llmProvider.appendLocalMessage(result, ChatRole.Assistant)
                }
            }

            ResponseAction.WebView -> TODO()
        }
    }

    companion object {
        private val logger = logger<CustomAgentChatProcessor>()
    }
}

