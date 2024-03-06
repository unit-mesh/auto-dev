package cc.unitmesh.devti.counit

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.counit.model.CustomAgentConfig
import cc.unitmesh.devti.counit.model.CustomAgentState
import cc.unitmesh.devti.counit.model.ResponseAction
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.util.LLMCoroutineScope
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class CustomAgentChatProcessor(val project: Project) {
    private val customAgentExecutor = CustomAgentExecutor(project)

    fun handleChat(prompter: ContextPrompter, ui: ChatCodingPanel, context: ChatContext?) {
        val originPrompt = prompter.requestPrompt()
        ui.addMessage(originPrompt, true, originPrompt)

        val request = originPrompt.trim()
        val selectedAgent: CustomAgentConfig = ui.getSelectedCustomAgent()

        selectedAgent.state = CustomAgentState.HANDLING

        val response = customAgentExecutor.execute(request, selectedAgent)
        if (response == null) {
            logger.error("error for custom agent: $selectedAgent with request: $request")
            return
        }

        selectedAgent.state = CustomAgentState.FINISHED
        when (selectedAgent.responseAction) {
            ResponseAction.Direct -> {
                ui.addMessage(response, false, response).also {
                    it.updateContent(response)
                    it.reRenderAssistantOutput()
                }
                ui.hiddenProgressBar()
                ui.updateUI()
            }

            ResponseAction.Stream -> {
                ui.addMessage(AutoDevBundle.message("autodev.loading"))
                LLMCoroutineScope.scope(project).launch {
//                    ui.updateMessage(response)
                }
            }

            ResponseAction.TextChunk -> {
                ui.setInput(response)
                ui.hiddenProgressBar()
            }

            ResponseAction.Flow -> {
                ui.addMessage(response, false, response).also {
                    it.updateContent(response)
                    it.reRenderAssistantOutput()
                }
                ui.hiddenProgressBar()
                ui.updateUI()
            }

            ResponseAction.WebView -> {
                //
            }
        }
    }

    companion object {
        private val logger = logger<CustomAgentChatProcessor>()
    }
}

