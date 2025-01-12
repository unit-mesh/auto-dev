package cc.unitmesh.devti.actions.console

import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.prompting.TextTemplatePrompt
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.settings.LanguageChangedCallback.presentationText
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.temporary.error.ErrorDescription
import com.intellij.temporary.error.ErrorMessageProcessor
import com.intellij.temporary.error.ErrorPromptBuilder


class FixThisAction : ChatBaseAction() {
    init{
        presentationText("settings.autodev.others.fixThis", templatePresentation)
    }

    override fun getActionType(): ChatActionType = ChatActionType.FIX_ISSUE
    private val logger = logger<FixThisAction>()

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val prompt: TextTemplatePrompt?

        val description: ErrorDescription? = ErrorMessageProcessor.getErrorDescription(event)
        if (description == null) {
            val editor = event.getData(CommonDataKeys.EDITOR) ?: return
            val text = editor.selectionModel.selectedText ?: return

            prompt = ErrorPromptBuilder.buildDisplayPrompt(text, text, "Fix this")
        } else {
            prompt = ErrorMessageProcessor.extracted(project, description)
            if (prompt == null) {
                logger.error("Prompt is null, description: $description")
                return
            }
        }

        if (prompt.displayText.isBlank() || prompt.requestText.isBlank()) {
            logger.error("Prompt is null, description: $description")
            return
        }

        sendToChatWindow(project, getActionType()) { panel, service ->
            service.handlePromptAndResponse(panel, object : ContextPrompter() {
                override fun displayPrompt(): String = prompt.displayText ?: ""
                override fun requestPrompt(): String = prompt.requestText ?: ""
            }, null, true)
        }
    }
}
