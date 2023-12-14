package cc.unitmesh.devti.actions.chat.base

import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatContext
import cc.unitmesh.devti.llms.openai.OpenAIProvider
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.gui.sendToChatPanel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.temporary.getElementToAction

abstract class ChatBaseAction : AnAction() {
    companion object {
        private val logger = logger<ChatBaseAction>()
    }

    open fun getReplaceableAction(event: AnActionEvent): ((response: String) -> Unit)? = null

    abstract fun getActionType(): ChatActionType

    override fun actionPerformed(event: AnActionEvent) = executeAction(event)

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

        val editor = event.getData(CommonDataKeys.EDITOR) ?: return

        val element = getElementToAction(project, editor) ?: return
        selectElement(element, editor)

        prompter.initContext(getActionType(), prefixText, file, project, caretModel?.offset ?: 0, element)

        sendToChatPanel(project) { panel, service ->
            val chatContext = ChatContext(
                getReplaceableAction(event),
                prefixText,
                suffixText
            )

            service.handlePromptAndResponse(panel, prompter, chatContext)
        }
    }

    private fun selectElement(elementToExplain: PsiElement, editor: Editor) {
        val startOffset = elementToExplain.textRange.startOffset
        val endOffset = elementToExplain.textRange.endOffset

        editor.selectionModel.setSelection(startOffset, endOffset)
    }
}