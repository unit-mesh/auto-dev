package cc.unitmesh.devti.vcs

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.vcs.log.VcsLogDataKeys


class ReleaseNoteSuggestionAction : AnAction() {
    companion object {
        val logger = logger<ReleaseNoteSuggestionAction>()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        val vcsLog = e.getData(VcsLogDataKeys.VCS_LOG)
        val stringList = vcsLog?.let { log ->
            log.selectedShortDetails.map { it.fullMessage }
        } ?: return

        val actionType = ChatActionType.CREATE_CHANGELOG

        val toolWindowManager = ToolWindowManager.getInstance(project!!).getToolWindow(AutoDevToolWindowFactory.Util.id)
        val contentManager = toolWindowManager?.contentManager
        val chatCodingService = ChatCodingService(actionType, project)
        val contentPanel = ChatCodingComponent(chatCodingService)
        val content = contentManager?.factory?.createContent(contentPanel, chatCodingService.getLabel(), false)

        contentManager?.removeAllContents(true)
        contentManager?.addContent(content!!)

        val commitMsgs = stringList.joinToString(",")
        toolWindowManager?.activate {
            chatCodingService.handlePromptAndResponse(contentPanel, object : ContextPrompter() {
                override fun displayPrompt(): String =
                    "generate release note based on follow info: $commitMsgs"

                override fun requestPrompt(): String =
                    "generate release note based on follow info: $commitMsgs"
            }, null)
        }
    }
}
