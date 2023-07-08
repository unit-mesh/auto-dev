package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.connector.ConnectorService
import com.intellij.openapi.application.ApplicationManager

class ChatCodingService(var actionType: ChatBotActionType) {
    val action = when (actionType) {
        ChatBotActionType.EXPLAIN -> "explain"
        ChatBotActionType.REVIEW -> "review"
        ChatBotActionType.REFACTOR -> "refactor"
        ChatBotActionType.CODE_COMPLETE -> "complete"
    }

    fun setActionType(actionType: ChatBotActionType) {
        this.actionType = actionType
    }

    fun getLabel(): String {
        val capitalizedAction = action.capitalize()
        return "$capitalizedAction Code"
    }

    fun handlePromptAndResponse(
        ui: ChatCodingComponent,
        prompt: PromptFormatter,
        replaceSelectedText: ((response: String) -> Unit)? = null
    ) {
        ui.add(prompt.getUIPrompt(), true)
        ui.add(AutoDevBundle.message("devti.loading"))

        ApplicationManager.getApplication().executeOnPooledThread {
            val response = this.makeChatBotRequest(prompt.getRequestPrompt())
            ApplicationManager.getApplication().invokeLater {
                when {
                    actionType === ChatBotActionType.REFACTOR -> ui.updateReplaceableContent(response) {
                        replaceSelectedText?.invoke(getCodeSection(response))
                    }

                    actionType === ChatBotActionType.CODE_COMPLETE -> ui.updateReplaceableContent(response) {
                        replaceSelectedText?.invoke(getCodeSection(response))
                    }

                    else -> ui.updateMessage(response)
                }
            }
        }
    }

    private val codeCopilot = ConnectorService.getInstance().connector()

    private fun makeChatBotRequest(requestPrompt: String): String {
        val connector = codeCopilot
        return connector.prompt(requestPrompt)
    }

    private fun getCodeSection(content: String): String {
        val pattern = "```(.+?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(content)

        if (match != null) return match.groupValues[1].trim()
        return content
    }
}

enum class ChatBotActionType {
    REFACTOR,
    EXPLAIN,
    REVIEW,
    CODE_COMPLETE
}