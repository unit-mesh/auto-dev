package cc.unitmesh.devti.gui.chat

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.util.LLMCoroutineScope
import cc.unitmesh.devti.counit.CustomAgentChatProcessor
import cc.unitmesh.devti.counit.configurable.customAgentSetting
import cc.unitmesh.devti.counit.model.CustomAgentState
import cc.unitmesh.devti.custom.compile.CustomVariable
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.util.parser.PostCodeProcessor
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ChatCodingService(var actionType: ChatActionType, val project: Project) {
    private val llmProvider = LlmFactory().create(project)
    private val counitProcessor = project.service<CustomAgentChatProcessor>()

    val action = actionType.instruction(project = project)

    fun getLabel(): String = "$actionType Code"

    fun handlePromptAndResponse(
        ui: ChatCodingPanel,
        prompter: ContextPrompter,
        context: ChatContext? = null,
        newChatContext: Boolean,
    ) {
        var requestPrompt = prompter.requestPrompt()
        var displayPrompt = prompter.displayPrompt()

        if (project.customAgentSetting.enableCustomRag && ui.hasSelectedCustomAgent()) {
            val selectedCustomAgent = ui.getSelectedCustomAgent()
            when {
                selectedCustomAgent.state === CustomAgentState.START -> {
                    counitProcessor.handleChat(prompter, ui, context, llmProvider)
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


        ui.addMessage(requestPrompt, true, displayPrompt)
        ui.addMessage(AutoDevBundle.message("autodev.loading"))

        ApplicationManager.getApplication().executeOnPooledThread {
            val response = this.makeChatBotRequest(requestPrompt, newChatContext)
            LLMCoroutineScope.scope(project).launch {
                when {
                    actionType === ChatActionType.REFACTOR -> ui.updateReplaceableContent(response) {
                        context?.replaceSelectedText?.invoke(getCodeSection(it, context.prefixText, context.suffixText))
                    }

                    actionType === ChatActionType.CODE_COMPLETE -> ui.updateReplaceableContent(response) {
                        context?.replaceSelectedText?.invoke(getCodeSection(it, context.prefixText, context.suffixText))
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

            LLMCoroutineScope.scope(project).launch {
                ui.updateMessage(response)
            }
        }
    }

    private fun makeChatBotRequest(requestPrompt: String, newChatContext: Boolean): Flow<String> {
        return llmProvider.stream(requestPrompt, "", keepHistory = !newChatContext)
    }

    private fun getCodeSection(content: String, prefixText: String, suffixText: String): String {
        val pattern = "```(.+?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(content)

        if (match != null) return match.groupValues[1].trim()

        return PostCodeProcessor(prefixText, suffixText, content).execute()
    }

    fun clearSession() {
        llmProvider.clearMessage()
    }
}
