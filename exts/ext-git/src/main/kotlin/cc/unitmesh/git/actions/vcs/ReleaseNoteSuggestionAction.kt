package cc.unitmesh.git.actions.vcs

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.settings.LanguageChangedCallback.presentationText
import cc.unitmesh.devti.template.GENIUS_PRACTISES
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.vcs.log.VcsLogDataKeys


class ReleaseNoteSuggestionAction : AnAction() {
    init{
        presentationText("settings.autodev.others.generateReleaseNote", templatePresentation)
    }
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vcsLog = e.getData(VcsLogDataKeys.VCS_LOG)
        val stringList = vcsLog?.let { log ->
            log.selectedShortDetails.map { it.fullMessage }
        } ?: return

        val actionType = ChatActionType.CREATE_CHANGELOG

        val toolWindowManager = AutoDevToolWindowFactory.getToolWindow(project)
        val contentManager = toolWindowManager?.contentManager
        val chatCodingService = ChatCodingService(actionType, project)
        val contentPanel = ChatCodingPanel(chatCodingService, toolWindowManager?.disposable)
        val content = contentManager?.factory?.createContent(contentPanel, chatCodingService.getLabel(), false)

        contentManager?.removeAllContents(true)
        contentManager?.addContent(content!!)

        val templateRender = TemplateRender(GENIUS_PRACTISES)
        val template = templateRender.getTemplate("release-note.vm")
        val commitMsgs = stringList.joinToString(",")

        templateRender.context = ReleaseNoteSuggestionContext(
            commitMsgs = commitMsgs,
        )

        val prompt = templateRender.renderTemplate(template)

        toolWindowManager?.activate {
            chatCodingService.handlePromptAndResponse(contentPanel, object : ContextPrompter() {
                override fun displayPrompt(): String = prompt
                override fun requestPrompt(): String = prompt
            }, null, true)
        }
    }
}

data class ReleaseNoteSuggestionContext(
    val commitMsgs: String = "",
) : TemplateContext