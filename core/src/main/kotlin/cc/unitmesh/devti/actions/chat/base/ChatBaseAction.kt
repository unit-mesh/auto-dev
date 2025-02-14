package cc.unitmesh.devti.actions.chat.base

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import cc.unitmesh.devti.gui.chat.message.ChatContext
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.gui.sendToChatPanel
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.temporary.getElementToAction

abstract class ChatBaseAction : AnAction() {
    companion object {
        private val logger = logger<ChatBaseAction>()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    open fun chatCompletedPostAction(event: AnActionEvent, panel: ChatCodingPanel): ((response: String) -> Unit)? = null

    abstract fun getActionType(): ChatActionType

    override fun actionPerformed(event: AnActionEvent) = executeAction(event)

    open fun executeAction(event: AnActionEvent) {
        val project = event.project ?: return
        val document = event.getData(CommonDataKeys.EDITOR)?.document

        val caretModel = event.getData(CommonDataKeys.EDITOR)?.caretModel
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE)

        val lineEndOffset = document?.getLineEndOffset(document.getLineNumber(caretModel?.offset ?: 0)) ?: 0
        val prefixText = buildSelectionText(editor, caretModel)
        val suffixText = document?.text?.substring(lineEndOffset) ?: ""
        val prompter = ContextPrompter.prompter(file?.language?.displayName ?: "")

        logger.info("use prompter: ${prompter.javaClass}")

        val element = getElementToAction(project, editor) ?: return
        var prompt = element.text
        if (prefixText.isNotEmpty()) {
            prompt = prefixText
        }

        prompt += addAdditionPrompt(project, editor, element)
        prompter.initContext(getActionType(), prompt, file, project, caretModel?.offset ?: 0, element)

        sendToChatPanel(project, getActionType()) { panel: ChatCodingPanel, service ->
            val chatContext = ChatContext(
                chatCompletedPostAction(event, panel),
                prefixText,
                suffixText
            )

            service.handlePromptAndResponse(panel, prompter, chatContext, keepHistory = true)
        }
    }

    private fun buildSelectionText(editor: Editor, caretModel: CaretModel?): @NlsSafe String {
        if (caretModel?.currentCaret?.selectedText.isNullOrEmpty()) {
            return ""
        }

        return caretModel?.currentCaret?.selectedText ?: ""
    }

    /**
     * After chat completion, we can provide some suggestions to the user.
     * For example, In issue: [#129](https://github.com/unit-mesh/auto-dev/issues/129), If our user doesn't provide any
     * refactor intention, we can provide some suggestions to the user.
     *
     * @param project The current project.
     * @param editor The editor that is currently in use.
     * @param element The PsiElement that is being completed.
     * @return A string representing the completion suggestion, or `null` if no suggestion is available.
     */
    open fun chatCompletionSuggestion(project: Project, editor: Editor, element: PsiElement): String? {
        return null
    }

    /**
     * Add additional prompt to the chat context.
     * Sample case:
     *
     * - Refactor: will collection code smell for the element
     *
     */
    open fun addAdditionPrompt(project: Project, editor: Editor, element: PsiElement): String = ""

    fun selectElement(elementToExplain: PsiElement, editor: Editor) {
        val startOffset = elementToExplain.textRange.startOffset
        val endOffset = elementToExplain.textRange.endOffset

        editor.selectionModel.setSelection(startOffset, endOffset)
    }
}

fun commentPrefix(element: PsiElement): String {
    return LanguageCommenters.INSTANCE.forLanguage(element.language)?.lineCommentPrefix ?: "//"
}