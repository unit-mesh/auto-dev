package cc.unitmesh.devti.intentions.editor

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatBotActionType
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlin.math.min

class CodeCompletionIntention : AbstractChatIntention() {
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return (editor != null) && (file != null)
    }

    override fun getText(): String = AutoDevBundle.message("intentions.chat.code.complete.name")

    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.code.complete.family.name")

    override fun getPrompt(project: Project, elementToExplain: PsiElement?): String {
        return "Complete code from prompt"
    }

    /**
     * Invokes the method and performs auto-completion based on the current caret position in the editor.
     *
     * @param project the current project
     * @param editor the editor in which the completion is performed
     * @param file the PSI file in the editor
     */
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        val document = editor.document
        val offset = editor.caretModel.offset
        val promptLength = 256
        var promptStart = (offset - promptLength).coerceAtLeast(0)
        val isOutBoundary = !EditorActionUtil.isWordBoundary(editor.document.text, promptStart, false, false)
        while (promptStart < offset && isOutBoundary) {
            promptStart++
        }
        if (promptStart == offset) {
            promptStart = (offset - promptLength).coerceAtLeast(0)
        }

        val prompt = document.getText(TextRange.create(promptStart, offset))

        val suffixLength = 256
        var suffixEnd = min((offset + suffixLength).toDouble(), document.textLength.toDouble()).toInt()
        while (suffixEnd > offset && !EditorActionUtil.isWordBoundary(editor.document.text, suffixEnd, false, false)) {
            suffixEnd--
        }

        val suffix = document.getText(TextRange(offset, suffixEnd))
        complete(project, prompt, suffix, editor, offset)
    }

    fun complete(project: Project, prompt: String, suffix: String, editor: Editor, offset: Int) {
        val actionType = ChatBotActionType.CODE_COMPLETE

        val prompter = object : ContextPrompter() {
            override fun getUIPrompt(): String {
                return "Complete code for follow code: \n$prompt"
            }

            override fun getRequestPrompt(): String {
                return "Complete code for follow code: \n$prompt"
            }
        }

        sendToChat(project, actionType, prompter)
    }

    fun insertStringAndSaveChange(
        project: Project,
        suggestion: String,
        document: Document,
        startOffset: Int,
        withReformat: Boolean
    ) {
        document.insertString(startOffset, suggestion)
        PsiDocumentManager.getInstance(project).commitDocument(document)

        if (withReformat) {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
            psiFile?.let { file ->
                val reformatRange = TextRange(startOffset, startOffset + suggestion.length)
                CodeStyleManager.getInstance(project).reformatText(file, listOf(reformatRange))
            }
        }
    }
}
