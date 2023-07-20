package cc.unitmesh.devti.intentions.editor

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.models.ConnectorFactory
import cc.unitmesh.devti.models.LLMCoroutineScopeService
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.jvm.internal.Ref
import kotlin.math.min


class CodeCompletionIntention : AbstractChatIntention() {
    override fun getText(): String = AutoDevBundle.message("intentions.chat.code.complete.name")
    override fun getFamilyName(): String = AutoDevBundle.message("intentions.chat.code.complete.family.name")
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        val document = editor.document
        val offset = editor.caretModel.offset
        val promptLength = 512
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

//        val element = PsiUtilBase.getElementAtCaret(editor) ?: file
//        val chunksWithPaths = SimilarChunksWithPaths().similarChunksWithPaths(element)
//        val size = chunksWithPaths.chunks?.size ?: 0
//        val similarCode = if (size > 0) {
//            chunksWithPaths.toQuery()
//        } else {
//            ""
//        }
//        prompt = "Code complete for follow code \n$similarCode\n$prompt"
        // TODO: use suffix to improve the completion
//        val suffix = document.getText(TextRange(offset, suffixEnd))

        logger.warn("Prompt: $prompt")

        updateCodeCompletion(prompt, editor, offset)

        val task = object : Task.Backgroundable(project, "Code completion", true) {
            override fun run(indicator: ProgressIndicator) {
                updateCodeCompletion(prompt, editor, offset)
            }
        }

        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))

    }

    private val writeActionGroupId = "code.complete.intention.write.action"

    /**
     * Renders an inlay with the given prompt in the specified editor at the specified offset.
     *
     * @param prompt The prompt to display in the inlay.
     * @param editor The editor in which to render the inlay.
     * @param offset The offset at which to render the inlay.
     */
    private fun updateCodeCompletion(
        prompt: @NlsSafe String,
        editor: Editor,
        offset: Int,
    ) {
        val flow: Flow<String> = connectorFactory.connector().stream(prompt)
        LLMCoroutineScopeService.scope(editor.project!!).launch {
            val currentOffset = Ref.IntRef()
            currentOffset.element = offset

            val project = editor.project!!
            val suggestion = StringBuilder()
            flow.collect {
                invokeLater {
                    WriteCommandAction.runWriteCommandAction(
                        project,
                        AutoDevBundle.message("intentions.chat.code.complete.name"),
                        writeActionGroupId,
                        {
                            insertStringAndSaveChange(project, it, editor.document, currentOffset.element, false)
                        }
                    )

                    currentOffset.element += it.length
                    editor.caretModel.moveToOffset(currentOffset.element)
                }
            }

            logger.warn("Suggestion: $suggestion")
        }
    }

    private fun insertLine(
        project: Project,
        suggestionLine: String,
        editor: Editor,
        currentOffset: Ref.IntRef
    ) {
        WriteCommandAction.runWriteCommandAction(
            project,
            AutoDevBundle.message("intentions.chat.code.complete.name"),
            writeActionGroupId,
            {
                insertStringAndSaveChange(
                    project,
                    suggestionLine,
                    editor.document,
                    currentOffset.element,
                    false
                )
            }
        )

        currentOffset.element += suggestionLine.length
        editor.caretModel.moveToOffset(currentOffset.element)
    }

    private val connectorFactory = ConnectorFactory.getInstance()

    companion object {
        val logger = Logger.getInstance(CodeCompletionIntention::class.java)

        fun updateDocument(
            project: Project,
            suggestion: String,
            document: Document,
            textRange: TextRange
        ) {
            document.replaceString(textRange.startOffset, textRange.endOffset, suggestion)
            PsiDocumentManager.getInstance(project).commitDocument(document)
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
            val reformatRange = TextRange(textRange.startOffset, textRange.startOffset + suggestion.length)
            CodeStyleManager.getInstance(project).reformatText(psiFile!!, listOf(reformatRange))
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
}
