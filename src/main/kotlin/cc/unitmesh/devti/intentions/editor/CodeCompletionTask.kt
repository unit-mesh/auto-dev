package cc.unitmesh.devti.intentions.editor

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.context.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.llms.ConnectorFactory
import cc.unitmesh.devti.llms.LLMCoroutineScopeService
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.Flow.*
import java.util.function.Consumer
import kotlin.jvm.internal.Ref


class CompletionTaskRequest(
    val project: Project,
    val useTabIndents: Boolean,
    val tabWidth: Int,
    val fileUri: VirtualFile,
    val documentContent: String,
    val offset: Int,
    val documentVersion: Long,
    val element: PsiElement,
    val editor: Editor
) : Disposable {
    companion object {
        fun create(editor: Editor, offset: Int, element: PsiElement): CompletionTaskRequest? {
            val project = editor.project ?: return null

            val document = editor.document
            val file = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return null

            val useTabs = editor.settings.isUseTabCharacter(project)
            val tabWidth = editor.settings.getTabSize(project)
            val uri = file.virtualFile
            val documentVersion = if (document is DocumentEx) {
                document.modificationSequence.toLong()
            } else {
                document.modificationStamp
            }


            return CompletionTaskRequest(
                project,
                useTabs,
                tabWidth,
                uri,
                document.text,
                offset,
                documentVersion,
                element,
                editor
            )

        }
    }

    @Volatile
    var isCancelled = false

    fun cancel() {
        if (isCancelled) {
            return
        }
        isCancelled = true
        Disposer.dispose(this)
    }

    override fun dispose() {
        isCancelled = true
    }
}

class CodeCompletionTask(
    private val request: CompletionTaskRequest,
) : Task.Backgroundable(request.project, AutoDevBundle.message("intentions.chat.code.complete.name")) {

    private val connectorFactory = ConnectorFactory.getInstance()

    private val writeActionGroupId = "code.complete.intention.write.action"
    private val codeMessage = AutoDevBundle.message("intentions.chat.code.complete.name")

    private val chunksString = SimilarChunksWithPaths.createQuery(request.element, 256)
    private val commenter = LanguageCommenters.INSTANCE.forLanguage(request.element.language)
    private val commentPrefix = commenter?.lineCommentPrefix

    override fun run(indicator: ProgressIndicator) {
        val prefix = request.documentContent.substring(0, request.offset)
        val prompt = if (chunksString == null) {
            prefix
        } else {
            "code complete for follow code: \n$commentPrefix${request.fileUri}\n$chunksString\n$prefix"
        }

        val flow: Flow<String> = connectorFactory.connector(request.project).stream(prompt)
        logger.warn("Prompt: $prompt")

        LLMCoroutineScopeService.scope(request.project).launch {
            val currentOffset = Ref.IntRef()
            currentOffset.element = request.offset

            val project = request.project
            val suggestion = StringBuilder()
            flow.collect {
                suggestion.append(it)
                invokeLater {
                    WriteCommandAction.runWriteCommandAction(
                        project,
                        codeMessage,
                        writeActionGroupId,
                        {
                            insertStringAndSaveChange(project, it, request.editor.document, currentOffset.element, false)
                        }
                    )

                    currentOffset.element += it.length
                    request.editor.caretModel.moveToOffset(currentOffset.element)
                }
            }

            logger.warn("Suggestion: $suggestion")
        }
    }

    fun execute(onFirstCompletion: Consumer<String>?) {
        LLMCoroutineScopeService.scope(project).launch {
            val flow: Flow<String> = connectorFactory.connector(project).stream("code complete")
            val suggestion = StringBuilder()
            flow.collect {
                suggestion.append(it)
            }

            onFirstCompletion?.accept(suggestion.toString())
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