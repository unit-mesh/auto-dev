package cc.unitmesh.devti.gui.chat

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.util.LLMCoroutineScope
import cc.unitmesh.devti.counit.CustomAgentChatProcessor
import cc.unitmesh.devti.counit.configurable.customRagSettings
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.util.parser.PostCodeProcessor
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ChatCodingService(var actionType: ChatActionType, val project: Project) {
    private val llmFactory = LlmFactory()
    private val counitProcessor = project.service<CustomAgentChatProcessor>()

    val action = actionType.instruction(project = project)

    fun getLabel(): String {
        val capitalizedAction = actionType
        return "$capitalizedAction Code"
    }

    fun handlePromptAndResponse(
        ui: ChatCodingPanel,
        prompter: ContextPrompter,
        context: ChatContext? = null,
        newChatContext: Boolean,
    ) {
        val requestPrompt = prompter.requestPrompt()
        val displayPrompt = prompter.displayPrompt()

        if (project.customRagSettings.enableCustomRag && ui.hasSelectedCustomAgent()) {
            counitProcessor.handleChat(prompter, ui, context)
            return
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
            val response = llmFactory.create(project).stream(requestPrompt, systemPrompt)

            LLMCoroutineScope.scope(project).launch {
                ui.updateMessage(response)
            }
        }
    }

    private fun makeChatBotRequest(requestPrompt: String, newChatContext: Boolean): Flow<String> {
        return llmFactory.create(project).stream(requestPrompt, "", keepHistory = !newChatContext)
    }

    private fun getCodeSection(content: String, prefixText: String, suffixText: String): String {
        val pattern = "```(.+?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(content)

        if (match != null) return match.groupValues[1].trim()

        return PostCodeProcessor(prefixText, suffixText, content).execute()
    }

    fun clearSession() {
        llmFactory.create(project).clearMessage()
    }
}
