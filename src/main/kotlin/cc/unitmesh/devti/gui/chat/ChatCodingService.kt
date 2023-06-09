package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.connector.ConnectorService
import cc.unitmesh.devti.parser.JavaCodePostProcessor
import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

class ChatCodingService(var actionType: ChatBotActionType) {
    private val connectorService = ConnectorService.getInstance()

    val action = when (actionType) {
        ChatBotActionType.EXPLAIN -> "explain"
        ChatBotActionType.REVIEW -> "review"
        ChatBotActionType.REFACTOR -> "refactor"
        ChatBotActionType.CODE_COMPLETE -> "complete"
        ChatBotActionType.WRITE_TEST -> "write test"
        ChatBotActionType.FIX_ISSUE -> "help me fix this"
        ChatBotActionType.GEN_COMMIT_MESSAGE -> "generate commit message"
        ChatBotActionType.CREATE_DDL -> "create ddl"
    }

    fun getLabel(): String {
        val capitalizedAction = action.capitalize()
        return "$capitalizedAction Code"
    }

    fun handlePromptAndResponse(
        ui: ChatCodingComponent,
        prompt: PromptFormatter,
        replaceSelectedText: ((response: String) -> Unit)? = null,
        prefixText: String,
        suffixText: String
    ) {
        ui.add(prompt.getUIPrompt(), true)
        ui.add(AutoDevBundle.message("devti.loading"))

        ApplicationManager.getApplication().executeOnPooledThread {
            val response = this.makeChatBotRequest(prompt.getRequestPrompt())
            runBlocking {
                when {
                    actionType === ChatBotActionType.REFACTOR -> ui.updateReplaceableContent(response) {
                        replaceSelectedText?.invoke(getCodeSection(it, prefixText, suffixText))
                    }

                    actionType === ChatBotActionType.CODE_COMPLETE -> ui.updateReplaceableContent(response) {
                        replaceSelectedText?.invoke(getCodeSection(it, prefixText, suffixText))
                    }

                    else -> ui.updateMessage(response)
                }
            }
        }
    }

    private fun makeChatBotRequest(requestPrompt: String): Flow<String> {
        return connectorService.connector().stream(requestPrompt)
    }

    private fun getCodeSection(content: String, prefixText: String, suffixText: String): String {
        val pattern = "```(.+?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(content)

        if (match != null) return match.groupValues[1].trim()

        return JavaCodePostProcessor(prefixText, suffixText, content).execute()
    }
}

enum class ChatBotActionType {
    REFACTOR,
    EXPLAIN,
    REVIEW,
    CODE_COMPLETE,
    WRITE_TEST,
    GEN_COMMIT_MESSAGE,
    FIX_ISSUE,
    CREATE_DDL;
}