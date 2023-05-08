package cc.unitmesh.devti.gui

import cc.unitmesh.devti.connector.ConnectorService
import com.intellij.openapi.application.ApplicationManager

class ChatCodingService {
    var actionType: ChatBotActionType = ChatBotActionType.EXPLAIN

    fun handlePromptAndResponse(
        ui: ChatCodingComponent,
        prompt: PromptFormatter,
        replaceSelectedText: ((response: String) -> Unit)? = null
    ) {
        ui.add(prompt.getUIPrompt(), true)
        ui.add("Loading...")

        ApplicationManager.getApplication().executeOnPooledThread {
            val response = this.makeChatBotRequest(prompt.getRequestPrompt())
            ApplicationManager.getApplication().invokeLater {
                when {
                    actionType === ChatBotActionType.REFACTOR -> ui.updateReplaceableContent(response) {
                        replaceSelectedText?.invoke(getCodeSection(response))
                    }

                    else -> ui.updateMessage(response)
                }
            }
        }
    }

    private fun makeChatBotRequest(requestPrompt: String): String {
        val connector = ConnectorService.getInstance().connector()
        return connector.prompt(requestPrompt)
    }

    private fun getCodeSection(content: String): String {
        val pattern = "```(.+?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(content)

        if (match != null) return match.groupValues[1].trim()
        return ""
    }
}

enum class ChatBotActionType {
    REFACTOR,
    EXPLAIN
}