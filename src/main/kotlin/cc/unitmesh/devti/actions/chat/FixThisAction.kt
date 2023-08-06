package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.intentions.error.ErrorDescription
import cc.unitmesh.devti.intentions.error.ErrorMessageProcessor
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.actionSystem.AnActionEvent


class FixThisAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.FIX_ISSUE
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val description: ErrorDescription = ErrorMessageProcessor.getErrorDescription(event) ?: return

        val prompt = ErrorMessageProcessor.extracted(project, description)

        sendToChatWindow(project, getActionType()) { panel, service ->
            service.handlePromptAndResponse(panel, object : ContextPrompter() {
                override fun displayPrompt(): String {
                    return prompt?.displayText ?: ""
                }

                override fun requestPrompt(): String {
                    return prompt?.requestText ?: ""
                }
            }, null)
        }
    }
}
