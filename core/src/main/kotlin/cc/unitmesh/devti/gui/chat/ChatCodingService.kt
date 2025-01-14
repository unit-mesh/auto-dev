package cc.unitmesh.devti.gui.chat

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.agent.CustomAgentChatProcessor
import cc.unitmesh.devti.agent.configurable.customAgentSetting
import cc.unitmesh.devti.agent.model.CustomAgentState
import cc.unitmesh.devti.custom.compile.CustomVariable
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.message.ChatContext
import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ChatCodingService(var actionType: ChatActionType, val project: Project) {
    private val llmProvider = LlmFactory().create(project)
    private val counitProcessor = project.service<CustomAgentChatProcessor>()
    private var currentJob: Job? = null

    fun getLabel(): String = "$actionType Code"

    fun stop() {
        currentJob?.cancel()
    }

    private var isLastAgent: Boolean = false

    fun handlePromptAndResponse(
        ui: ChatCodingPanel,
        prompter: ContextPrompter,
        context: ChatContext? = null,
        keepHistory: Boolean
    ) {
        var chatHistory: List<LlmMsg.ChatMessage> = emptyList()
        currentJob?.cancel()
        var requestPrompt = prompter.requestPrompt()
        var displayPrompt = prompter.displayPrompt()

        if (project.customAgentSetting.enableCustomRag && ui.hasSelectedCustomAgent()) {
            val selectedCustomAgent = ui.getSelectedCustomAgent()
            when {
                selectedCustomAgent.state === CustomAgentState.START -> {
                    isLastAgent = true
                    val response = ApplicationManager.getApplication().executeOnPooledThread<String?> {
                        val output = counitProcessor.handleChat(prompter, ui, llmProvider)

                        runInEdt {
                            ui.resetAgent()
                            ui.moveCursorToStart()
                        }

                        output
                    }

                    return
                }

                selectedCustomAgent.state === CustomAgentState.FINISHED -> {
                    if (CustomVariable.hasVariable(requestPrompt)) {
                        val compiler = prompter.toTemplateCompiler()
                        compiler?.also {
                            requestPrompt = CustomVariable.compile(requestPrompt, it)
                            displayPrompt = CustomVariable.compile(displayPrompt, it)
                        }
                    }
                }
            }
        }

        if (isLastAgent) {
            isLastAgent = false
            chatHistory = ui.getHistoryMessages()
        }

        ui.addMessage(requestPrompt, true, displayPrompt)
        ui.addMessage(AutoDevBundle.message("autodev.loading"))

        ApplicationManager.getApplication().executeOnPooledThread {
            val response = this.makeChatBotRequest(requestPrompt, keepHistory, chatHistory)
            currentJob = AutoDevCoroutineScope.scope(project).launch {
                when {
                    actionType === ChatActionType.REFACTOR -> ui.updateReplaceableContent(response) {
                        context?.postAction?.invoke(it)
                    }

                    else -> ui.updateMessage(response)
                }
            }
        }
    }

    fun handleMsgsAndResponse(
        ui: ChatCodingPanel,
        messages: List<LlmMsg.ChatMessage>,
    ) {
        val requestPrompt = messages.filter { it.role == LlmMsg.ChatRole.User }.joinToString("\n") { it.content }
        val systemPrompt = messages.filter { it.role == LlmMsg.ChatRole.System }.joinToString("\n") { it.content }

        ui.addMessage(requestPrompt, true, requestPrompt)
        ui.addMessage(AutoDevBundle.message("autodev.loading"))

        ApplicationManager.getApplication().executeOnPooledThread {
            val response = llmProvider.stream(requestPrompt, systemPrompt)

            currentJob = AutoDevCoroutineScope.scope(project).launch {
                ui.updateMessage(response)
            }
        }
    }

    fun makeChatBotRequest(
        requestPrompt: String,
        newChatContext: Boolean,
        chatHistory: List<LlmMsg.ChatMessage>
    ): Flow<String> {
        if (chatHistory.isNotEmpty()) {
            chatHistory.forEach {
                llmProvider.appendLocalMessage(it.content, ChatRole.valueOf(it.role.name))
            }
        }

        return llmProvider.stream(requestPrompt, "", keepHistory = !newChatContext)
    }

    fun clearSession() {
        llmProvider.clearMessage()
    }

    fun request(systemPrompt: String, userPrompt: String): Flow<String> {
        return llmProvider.stream(userPrompt, systemPrompt, keepHistory = true)
    }
}
