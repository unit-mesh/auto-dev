package cc.unitmesh.devti.agent.custom

import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.agent.custom.model.CustomAgentResponseAction
import cc.unitmesh.devti.agent.custom.model.CustomAgentState
import cc.unitmesh.devti.gui.chat.NormalChatCodingPanel
import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.devins.CustomAgentContext
import cc.unitmesh.devti.provider.devins.LanguageProcessor
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture

@Service(Service.Level.PROJECT)
class CustomAgentChatProcessor(val project: Project) {
    private val customAgentExecutor = project.service<CustomAgentExecutor>()
    private val logger = logger<CustomAgentChatProcessor>()

    fun handleChat(prompter: ContextPrompter, ui: NormalChatCodingPanel, llmProvider: LLMProvider): String? {
        val originPrompt = prompter.requestPrompt()
        val displayMessage = originPrompt

        val request = originPrompt.trim()
        val selectedAgent: CustomAgentConfig = ui.getSelectedCustomAgent()

        selectedAgent.state = CustomAgentState.HANDLING

        val response: Flow<String>? = customAgentExecutor.execute(request, selectedAgent)
        if (response == null) {
            logger.error("error for custom agent: $selectedAgent with request: $request")
            return null
        }

        ui.addMessage(originPrompt, true, displayMessage)

        var llmResponse = ""
        selectedAgent.state = CustomAgentState.FINISHED

        var devInCode: String? = ""
        when (selectedAgent.responseAction) {
            CustomAgentResponseAction.Direct -> {
                val sb = StringBuilder()
                runBlocking {
                    val result = ui.updateMessage(response)
                    sb.append(result)
                }

                val content = sb.toString().removeSurrounding("\"")
                llmProvider.appendLocalMessage(content, ChatRole.Assistant)

                val code = CodeFence.parse(content)
                if (code.language.displayName == "DevIn") {
                    devInCode = code.text
                }

                ui.hiddenProgressBar()
                ui.updateUI()

                llmResponse = content
            }

            CustomAgentResponseAction.Stream -> {
                val future: CompletableFuture<String> = CompletableFuture()
                val sb = StringBuilder()
                runBlocking {
                    val result = ui.updateMessage(response)
                    sb.append(result)
                }

                val content = sb.toString()
                llmProvider.appendLocalMessage(content, ChatRole.Assistant)
                ui.hiddenProgressBar()
                ui.updateUI()
                ui.moveCursorToStart()

                val code = CodeFence.parse(content)
                if (code.language.displayName == "DevIn") {
                    devInCode = code.text
                }

                future.complete(content)
                llmResponse = future.get()
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
                ui.moveCursorToStart()
                ui.setInput(content)
                ui.hiddenProgressBar()

                llmResponse = content
            }

            CustomAgentResponseAction.Flow -> {
                logger.error("will not support flow response for now")
            }

            CustomAgentResponseAction.WebView -> {
                val sb = StringBuilder()
                runBlocking {
                    response.collect {
                        sb.append(it)
                    }
                }

                val content = sb.toString()
                llmProvider.appendLocalMessage(content, ChatRole.Assistant)

                ui.appendWebView(content, project)
                ui.hiddenProgressBar()

                llmResponse = content
            }

            CustomAgentResponseAction.DevIns -> {
                val msg: String = runBlocking {
                    ui.updateMessage(response)
                }

                llmProvider.appendLocalMessage(msg, ChatRole.Assistant)
                ui.hiddenProgressBar()
                ui.updateUI()

                devInCode = msg

                llmResponse = msg
            }
        }

        if (!devInCode.isNullOrEmpty()) {
            val task = object : Task.Backgroundable(project, "Compile context", false) {
                override fun run(indicator: ProgressIndicator) {
                    val devin = LanguageProcessor.devin()
                    runBlocking {
                        devin?.execute(project, CustomAgentContext(selectedAgent, devInCode!!))
                    }
                }
            }
            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        }

        return llmResponse
    }
}
