package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingComponent
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.llms.openai.OpenAIProvider
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase

abstract class ChatBaseAction : AnAction() {
    companion object {
        private val logger = logger<OpenAIProvider>()
    }

    override fun actionPerformed(event: AnActionEvent) {
        executeAction(event)
    }

    open fun executeAction(event: AnActionEvent) {
        val project = event.project ?: return
        val document = event.getData(CommonDataKeys.EDITOR)?.document

        val caretModel = event.getData(CommonDataKeys.EDITOR)?.caretModel
        var prefixText = caretModel?.currentCaret?.selectedText ?: ""

        val file = event.getData(CommonDataKeys.PSI_FILE)

        val lineEndOffset = document?.getLineEndOffset(document.getLineNumber(caretModel?.offset ?: 0)) ?: 0
        // if selectedText is empty, then we use the cursor position to get the text
        if (prefixText.isEmpty()) {
            prefixText = document?.text?.substring(0, lineEndOffset) ?: ""
        }
        val suffixText = document?.text?.substring(lineEndOffset) ?: ""

        val prompter = ContextPrompter.prompter(file?.language?.displayName ?: "")
        logger.info("use prompter: ${prompter.javaClass}")
        prompter.initContext(getActionType(), prefixText, file, project, caretModel?.offset ?: 0)

        sendToToolWindow(project) { service, panel ->
            val chatContext = ChatContext(
                getReplaceableAction(event),
                prefixText,
                suffixText
            )

            service.handlePromptAndResponse(panel, prompter, chatContext)
        }
    }

    open fun sendToToolWindow(
        project: Project,
        activeAction: (service: ChatCodingService, panel: ChatCodingComponent) -> Unit
    ) {
        val chatCodingService = ChatCodingService(getActionType(), project)
        val contentPanel = ChatCodingComponent(chatCodingService)

        val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolWindowFactory.Util.id)
        val contentManager = toolWindowManager?.contentManager

        val content = contentManager?.factory?.createContent(contentPanel, chatCodingService.getLabel(), false)

        contentManager?.removeAllContents(true)
        contentManager?.addContent(content!!)

        toolWindowManager?.activate {
            activeAction(chatCodingService, contentPanel)
        }
    }

    open fun getReplaceableAction(event: AnActionEvent): ((response: String) -> Unit)? {
        return null
    }

    abstract fun getActionType(): ChatActionType
}