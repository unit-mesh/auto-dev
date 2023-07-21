package cc.unitmesh.devti.intentions.editor

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.models.ConnectorFactory
import cc.unitmesh.devti.models.LLMCoroutineScopeService
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.jvm.internal.Ref

class CodeCompletionTask(
    private val editor: Editor,
    private val prefix: String,
    private val suffix: String,
    private val element: PsiElement,
    private val offset: Int,
) : Task.Backgroundable(editor.project, AutoDevBundle.message("intentions.chat.code.complete.name")) {

    private val connectorFactory = ConnectorFactory.getInstance()

    private val writeActionGroupId = "code.complete.intention.write.action"
    private val codeMessage = AutoDevBundle.message("intentions.chat.code.complete.name")

    private val chunksString = SimilarChunksWithPaths.createQuery(element, 256)
    private val commenter = LanguageCommenters.INSTANCE.forLanguage(element.language)
    private val commentPrefix = commenter?.lineCommentPrefix

    override fun run(indicator: ProgressIndicator) {
        val prompt = if (chunksString == null) {
            prefix
        } else {
            val filePath = element.containingFile.virtualFile.path
            "code complete for follow code: \n$commentPrefix$filePath\n$chunksString\n$prefix"
        }

        val flow: Flow<String> = connectorFactory.connector(editor.project!!).stream(prompt)
        logger.warn("Prompt: $prompt")

        LLMCoroutineScopeService.scope(editor.project!!).launch {
            val currentOffset = Ref.IntRef()
            currentOffset.element = offset

            val project = editor.project!!
            val suggestion = StringBuilder()
            flow.collect {
                suggestion.append(it)
                invokeLater {
                    WriteCommandAction.runWriteCommandAction(
                        project,
                        codeMessage,
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

    companion object {
        val logger = Logger.getInstance(CodeCompletionIntention::class.java)

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