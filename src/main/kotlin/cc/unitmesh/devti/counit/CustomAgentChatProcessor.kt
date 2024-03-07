package cc.unitmesh.devti.counit

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.counit.model.CustomAgentConfig
import cc.unitmesh.devti.counit.model.CustomAgentState
import cc.unitmesh.devti.counit.model.CustomAgentResponseAction
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.util.LLMCoroutineScope
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

@Service(Service.Level.PROJECT)
class CustomAgentChatProcessor(val project: Project) {
    private val customAgentExecutor = CustomAgentExecutor(project)

    fun handleChat(prompter: ContextPrompter, ui: ChatCodingPanel, context: ChatContext?, llmProvider: LLMProvider) {
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
            CustomAgentResponseAction.Direct -> {
                val message = ui.addMessage("loading", false, "")
                val sb = StringBuilder()
                runBlocking {
                    val result = ui.updateMessage(response)
                    sb.append(result)
                }

                llmProvider.appendLocalMessage(sb.toString(), ChatRole.Assistant)
                message.reRenderAssistantOutput()
                ui.hiddenProgressBar()
                ui.updateUI()
            }

            CustomAgentResponseAction.Stream -> {
                ui.addMessage(AutoDevBundle.message("autodev.loading"))
                LLMCoroutineScope.scope(project).launch {
                    ui.updateMessage(response)
                }
            }

            CustomAgentResponseAction.TextChunk -> {
                val sb = StringBuilder()
                runBlocking {
                    response.collect {
                        sb.append(it)
                    }
                }

                val content = sb.toString()
                llmProvider.appendLocalMessage(content, ChatRole.Assistant)
                ui.removeLastMessage()
                ui.setInput(content)
                ui.hiddenProgressBar()
            }

            CustomAgentResponseAction.Flow -> {
                TODO()
            }

            CustomAgentResponseAction.WebView -> {
                val sb = StringBuilder()
                runBlocking {
                    response.collect {
                        sb.append(it)
                    }
                }
                // TODO: add decode support
                val content = sb.toString()
                llmProvider.appendLocalMessage(content, ChatRole.Assistant)

                ui.appendWebView(content, project)
                ui.hiddenProgressBar()
            }
        }
    }

    companion object {
        private val logger = logger<CustomAgentChatProcessor>()
    }
}

