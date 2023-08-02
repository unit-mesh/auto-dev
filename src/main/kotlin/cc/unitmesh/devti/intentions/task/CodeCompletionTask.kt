package cc.unitmesh.devti.intentions.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.llms.ConnectorFactory
import cc.unitmesh.devti.editor.LLMCoroutineScopeService
import cc.unitmesh.devti.intentions.CodeCompletionIntention
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.function.Consumer
import kotlin.jvm.internal.Ref

class CodeCompletionTask(private val request: CompletionTaskRequest) :
    Task.Backgroundable(request.project, AutoDevBundle.message("intentions.chat.code.complete.name")) {

    private val connectorFactory = ConnectorFactory.getInstance()

    private val writeActionGroupId = "code.complete.intention.write.action"
    private val codeMessage = AutoDevBundle.message("intentions.chat.code.complete.name")

    private val chunksString = SimilarChunksWithPaths.createQuery(request.element, 256)
    private val commenter = LanguageCommenters.INSTANCE.forLanguage(request.element.language)
    private val commentPrefix = commenter?.lineCommentPrefix

    override fun run(indicator: ProgressIndicator) {
        val prompt = promptText()

        val flow: Flow<String> = connectorFactory.connector(request.project).stream(prompt, "")
        logger.info("Prompt: $prompt")

        val editor = request.editor
        LLMCoroutineScopeService.scope(request.project).launch {
            val currentOffset = Ref.IntRef()
            currentOffset.element = request.offset

            val project = request.project
            val suggestion = StringBuilder()

            flow.collect {
                suggestion.append(it)
                invokeLater {
                    WriteCommandAction.runWriteCommandAction(project, codeMessage, writeActionGroupId, {
                        insertStringAndSaveChange(project, it, editor.document, currentOffset.element, false)
                    })

                    currentOffset.element += it.length
                    editor.caretModel.moveToOffset(currentOffset.element)
                    editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
                }
            }

            logger.info("Suggestion: $suggestion")
        }
    }

    private fun promptText(): String {
        val prefix = request.documentContent.substring(0, request.offset)
        val prompt = if (chunksString == null) {
            "complete code for given code: \n$prefix"
        } else {
            "complete code for given code: \n$commentPrefix${request.fileUri}\n$chunksString\n$prefix"
        }

        return prompt
    }

    fun execute(onFirstCompletion: Consumer<String>?) {
        val prompt = promptText()

        logger.warn("Prompt: $prompt")
        LLMCoroutineScopeService.scope(project).launch {
            val flow: Flow<String> = connectorFactory.connector(project).stream(prompt, "")
            val suggestion = StringBuilder()
            flow.collect {
                suggestion.append(it)
            }

            onFirstCompletion?.accept(suggestion.toString())
        }
    }

    companion object {
        val logger = logger<CodeCompletionIntention>()

        fun insertStringAndSaveChange(
            project: Project,
            suggestion: String,
            document: Document,
            startOffset: Int,
            withReformat: Boolean
        ) {
            document.insertString(startOffset, suggestion)
            PsiDocumentManager.getInstance(project).commitDocument(document)

            if (!withReformat) return

            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
            psiFile?.let { file ->
                val reformatRange = TextRange(startOffset, startOffset + suggestion.length)
                CodeStyleManager.getInstance(project).reformatText(file, listOf(reformatRange))
            }
        }
    }
}