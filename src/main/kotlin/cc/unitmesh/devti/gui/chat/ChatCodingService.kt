package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.models.ConnectorFactory
import cc.unitmesh.devti.models.LLMCoroutineScopeService
import cc.unitmesh.devti.parser.PostCodeProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class ChatContext(
    val replaceSelectedText: ((response: String) -> Unit)? = null,
    val prefixText: String,
    val suffixText: String
)

class ChatCodingService(var actionType: ChatBotActionType, val project: Project) {
    private val connectorFactory = ConnectorFactory.getInstance()

    val action = when (actionType) {
        ChatBotActionType.EXPLAIN -> "Write down what this code does"
        ChatBotActionType.REVIEW -> "Code Review"
        ChatBotActionType.REFACTOR -> "Refactor This code"
        ChatBotActionType.CODE_COMPLETE -> "Auto Complete for this code"
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
        prompt: ContextPrompter,
        context: ChatContext? = null
    ) {
        ui.add(prompt.getUIPrompt(), true)
        ui.add(AutoDevBundle.message("devti.loading"))

        ApplicationManager.getApplication().executeOnPooledThread {
            val response = this.makeChatBotRequest(prompt.getRequestPrompt())
            LLMCoroutineScopeService.scope(project).launch {
                when {
                    actionType === ChatBotActionType.REFACTOR -> ui.updateReplaceableContent(response) {
                        context?.replaceSelectedText?.invoke(getCodeSection(it, context.prefixText, context.suffixText))
                    }

                    actionType === ChatBotActionType.CODE_COMPLETE -> ui.updateReplaceableContent(response) {
                        context?.replaceSelectedText?.invoke(getCodeSection(it, context.prefixText, context.suffixText))
                    }

                    else -> ui.updateMessage(response)
                }
            }
        }
    }

    private fun makeChatBotRequest(requestPrompt: String): Flow<String> {
        return connectorFactory.connector(project).stream(requestPrompt)
    }

    private fun getCodeSection(content: String, prefixText: String, suffixText: String): String {
        val pattern = "```(.+?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(content)

        if (match != null) return match.groupValues[1].trim()

        return PostCodeProcessor(prefixText, suffixText, content).execute()
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