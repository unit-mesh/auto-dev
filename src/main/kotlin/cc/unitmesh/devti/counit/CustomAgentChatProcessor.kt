package cc.unitmesh.devti.counit

import cc.unitmesh.devti.counit.model.CustomAgentConfig
import cc.unitmesh.devti.counit.model.ResponseAction
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.util.LLMCoroutineScope
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class CustomAgentChatProcessor(val project: Project) {
    private val llmFactory = LlmFactory()

    private val customAgentHandler = CustomAgentHandler(project)
    private val llmProvider = llmFactory.create(project)

    fun handleChat(prompter: ContextPrompter, ui: ChatCodingPanel, context: ChatContext?) {
        val originPrompt = prompter.requestPrompt()
        ui.addMessage(originPrompt, true, originPrompt)

        val request = originPrompt.trim()
        val selectedAgent: CustomAgentConfig = ui.getSelectedCustomAgent()

        val response = customAgentHandler.executeQuery(request, selectedAgent)
        if (response == null) {
            logger.error("can not find intention for request: $request")
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
                ui.addMessage("start to identify intention", false, "start to identify intention")
                LLMCoroutineScope.scope(project).launch {
                    llmProvider.appendLocalMessage(response, ChatRole.User)

                    val intentionFlow = llmProvider.stream(response, "")
                    val result = ui.updateMessage(intentionFlow)

                    llmProvider.appendLocalMessage(result, ChatRole.Assistant)

                    val searchTip = "search API by query and hypothetical document"
                    llmProvider.appendLocalMessage(searchTip, ChatRole.User)
                    ui.addMessage(searchTip, true, searchTip)

                    val related = customAgentHandler.semanticQuery("") ?: ""
                    if (related.isEmpty()) {
                        val noResultTip = "no related API found"
                        llmProvider.appendLocalMessage(noResultTip, ChatRole.Assistant)
                        ui.addMessage(noResultTip, false, noResultTip)
                        return@launch
                    }

                    llmProvider.appendLocalMessage(related, ChatRole.User)

                    ApplicationManager.getApplication().invokeLater {
                        ui.addMessage(related, true, related)
                    }
                }
            }
        }
    }

    /**
     * This method is used to extract JSON response from a given string.
     * It removes the leading and trailing ````json` tags from the string,
     * which are used to denote JSON code blocks in markdown files.
     *
     * @param result The string containing the JSON response surrounded by ````json` tags.
     * @return The extracted JSON response string without the ````json` tags.
     */
    private fun extractJsonResponse(result: String): String {
        return result
            .removePrefix("```json")
            .removeSuffix("```")
    }

    companion object {
        private val logger = logger<CustomAgentChatProcessor>()
    }
}

