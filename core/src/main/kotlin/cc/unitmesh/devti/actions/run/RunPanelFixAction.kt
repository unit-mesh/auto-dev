package cc.unitmesh.devti.actions.run

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class RunPanelFixAction : AnAction() {
    fun getActionType(): ChatActionType = ChatActionType.FIX_ISSUE

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val selectedContent: RunContentDescriptor = RunContentManager.getInstance(project).selectedContent ?: return
        val console = selectedContent?.executionConsole as ConsoleView

        val sb = StringBuilder()
        console.addMessageFilter { line, _ ->
            sb.append(line)
            null
        }

        val content = sb.toString()
        if (content.isNotEmpty()) {
            sendToChatWindow(project, getActionType()) { panel, service ->
                service.handlePromptAndResponse(panel, object : ContextPrompter() {
                    override fun displayPrompt(): String = content
                    override fun requestPrompt(): String = content
                }, null, true)
            }
        } else {
            AutoDevNotifications.error(project, "Cannot extract text from run panel.");
        }
    }
}
