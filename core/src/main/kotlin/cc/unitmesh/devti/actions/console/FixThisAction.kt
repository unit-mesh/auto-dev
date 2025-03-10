package cc.unitmesh.devti.actions.console

import cc.unitmesh.devti.actions.chat.base.ChatBaseAction
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.prompting.TextTemplatePrompt
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.presentationText
import cc.unitmesh.devti.template.GENIUS_ERROR
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger

class FixThisAction : ChatBaseAction() {
    init {
        presentationText("settings.autodev.others.fixThis", templatePresentation)
    }

    override fun getActionType(): ChatActionType = ChatActionType.FIX_ISSUE
    private val logger = logger<FixThisAction>()

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val prompt: TextTemplatePrompt?

        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val selectionModel = editor.selectionModel
        val text = selectionModel.selectedText ?: return

        prompt = buildDisplayPrompt(text, text, "Fix this")

        if (prompt.displayText.isBlank() || prompt.requestText.isBlank()) {
            logger.error("Prompt is null, description: $text")
            return
        }

        sendToChatWindow(project, getActionType()) { panel, service ->
            service.handlePromptAndResponse(panel, object : ContextPrompter() {
                override fun displayPrompt(): String = prompt.displayText
                override fun requestPrompt(): String = prompt.requestText
            }, null, true)
        }
    }

    fun buildDisplayPrompt(errorTextTrimmed: String, sourceCode: String, displayText: String): TextTemplatePrompt {
        val templateRender = TemplateRender(GENIUS_ERROR)
        templateRender.context = ErrorContext(errorTextTrimmed, sourceCode)
        val template = templateRender.getTemplate("fix-error.vm")
        val prompt = templateRender.renderTemplate(template)
        return TextTemplatePrompt(displayText, prompt)
    }
}

data class ErrorContext(
    val errorText: String,
    val sourceCode: String,
) : TemplateContext

