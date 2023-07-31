package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.DevtiFlowToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.gui.chat.ChatCodingService
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager

class ChatWithThisAction: ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.CHAT
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val caretModel = event.getData(CommonDataKeys.EDITOR)?.caretModel
        val prefixText = caretModel?.currentCaret?.selectedText ?: ""

        val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(DevtiFlowToolWindowFactory.Util.id)
        val contentManager = toolWindowManager?.contentManager

        val chatCodingService = ChatCodingService(getActionType(), project)
        val contentPanel = ChatCodingComponent(chatCodingService)
        val content = contentManager?.factory?.createContent(contentPanel, "Chat with this", false)
        val language = event.getData(CommonDataKeys.PSI_FILE)?.language?.displayName ?: ""

        contentManager?.removeAllContents(true)
        contentManager?.addContent(content!!)

        toolWindowManager?.activate {
            contentPanel.setContent("""
                |```$language
                |$prefixText
                |```""".trimMargin())
        }

    }
}