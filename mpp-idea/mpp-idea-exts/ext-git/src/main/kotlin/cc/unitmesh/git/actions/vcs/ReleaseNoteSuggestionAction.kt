package cc.unitmesh.git.actions.vcs

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory.AutoDevToolUtil
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.presentationText
import cc.unitmesh.devti.template.GENIUS_PRACTISES
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.vcs.log.VcsLogDataKeys


class ReleaseNoteSuggestionAction : AnAction() {
    init {
        presentationText("settings.autodev.others.generateReleaseNote", templatePresentation)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vcsLog = e.getData(VcsLogDataKeys.VCS_LOG)
        val stringList = vcsLog?.let { log ->
            log.selectedShortDetails.map { it.fullMessage }
        } ?: return

        val actionType = ChatActionType.CREATE_CHANGELOG

        val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolUtil.ID) ?: run {
            logger<ReleaseNoteSuggestionAction>().error("toolWindowManager is null")
            return
        }

        val chatCodingService = ChatCodingService(actionType, project)
        val contentPanel = AutoDevToolWindowFactory.labelNormalChat(chatCodingService) ?: run {
            logger<ReleaseNoteSuggestionAction>().error("contentPanel is null")
            return
        }

        val templateRender = TemplateRender(GENIUS_PRACTISES)
        val template = templateRender.getTemplate("release-note.vm")
        val commitMsgs = stringList.joinToString(",")

        templateRender.context = ReleaseNoteSuggestionContext(
            commitMsgs = commitMsgs,
        )

        val prompt = templateRender.renderTemplate(template)

        toolWindowManager.activate {
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