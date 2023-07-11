package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.connector.ConnectorService
import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.flow.Flow

class ChatCodingService(var actionType: ChatBotActionType) {
    val action = when (actionType) {
        ChatBotActionType.EXPLAIN -> "explain"
        ChatBotActionType.REVIEW -> "review"
        ChatBotActionType.REFACTOR -> "refactor"
        ChatBotActionType.CODE_COMPLETE -> "complete"
        ChatBotActionType.WRITE_TEST -> "write test"
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
                        replaceSelectedText?.invoke(getCodeSection(it))
                    }

                    actionType === ChatBotActionType.CODE_COMPLETE -> ui.updateReplaceableContent(response) {
                        replaceSelectedText?.invoke(getCodeSection(it))
                    }

                    else -> ui.updateMessage(response)
                }
            }
        }
    }

    private val codeCopilot = ConnectorService.getInstance().connector()

    private fun makeChatBotRequest(requestPrompt: String): Flow<String> {
        val connector = codeCopilot
        return connector.stream(requestPrompt)
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
    CODE_COMPLETE,
    WRITE_TEST
}