package cc.unitmesh.devti.intentions.editor

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.models.ConnectorFactory
import cc.unitmesh.devti.presentation.LLMTextPresentation
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlin.math.min

class CodeCompletionIntention : AbstractChatIntention() {
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return (editor != null) && (file != null)
    }

    override fun getText(): String = AutoDevBundle.message("intentions.chat.code.complete.name")

    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.code.complete.family.name")

    override fun getPrompt(project: Project, elementToExplain: PsiElement?): String {
        return "Complete code for "
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

        val text = ""
        val flow = connectorFactory.connector().prompt(prompt)
        val presentation = LLMTextPresentation(editor, flow, false)
        val editorCustomElementRenderer: EditorCustomElementRenderer = PresentationRenderer(presentation)
        editor.inlayModel.addAfterLineEndElement(
            offset,
            true,
            editorCustomElementRenderer
        )
    }

    // for future
    private suspend fun renderInlay(
        prompt: @NlsSafe String,
        editor: Editor,
        offset: Int
    ) {
        val flow: Flow<String> = connectorFactory.connector().stream(prompt)
        var text = ""
        flow.collect {
            text += it
            if (text.isNotEmpty()) {
                val presentation = LLMTextPresentation(editor, it, false)
                val editorCustomElementRenderer: EditorCustomElementRenderer = PresentationRenderer(presentation)
                editor.inlayModel.addAfterLineEndElement(
                    offset,
                    true,
                    editorCustomElementRenderer
                )
            }
        }
    }

    private val connectorFactory = ConnectorFactory.getInstance()

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
