package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.inlay.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.statusbar.AutoDevStatusService
import cc.unitmesh.devti.util.InsertUtil
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.function.Consumer
import kotlin.jvm.internal.Ref

class CodeCompletionTask(private val request: CodeCompletionRequest) :
    Task.Backgroundable(request.project, AutoDevBundle.message("intentions.chat.code.complete.name")) {

    private val writeActionGroupId = "code.complete.intention.write.action"
    private val codeMessage = AutoDevBundle.message("intentions.chat.code.complete.name")

    private val chunksString = request.element?.let { SimilarChunksWithPaths.createQuery(it, 60) }
    private val commenter = request.element?.let { LanguageCommenters.INSTANCE.forLanguage(it.language) }
    private val commentPrefix = commenter?.lineCommentPrefix

    override fun run(indicator: ProgressIndicator) {
        val prompt = promptText()

        val flow: Flow<String> = LlmFactory.create(request.project).stream(prompt, "", false)
        logger.info("Prompt: $prompt")

        val editor = request.editor
        AutoDevCoroutineScope.scope(request.project).launch {
            val currentOffset = Ref.IntRef()
            currentOffset.element = request.offset

            val project = request.project
            val suggestion = StringBuilder()

            flow.collect {
                suggestion.append(it)
                invokeLater {
                    WriteCommandAction.runWriteCommandAction(project, codeMessage, writeActionGroupId, {
                        InsertUtil.insertStringAndSaveChange(project, it, editor.document, currentOffset.element, false)
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
        val documentLength = request.editor.document.textLength
        val prefix = if (request.offset > documentLength) {
            request.prefix
        } else {
            val text = request.editor.document.text
            text.substring(0, request.offset)
        }

        val prompt = if (chunksString == null) {
            prefix
        } else {
            "$commentPrefix\n$chunksString\n$prefix"
        }

        return prompt
    }

    fun execute(onFirstCompletion: Consumer<String>?) {
        AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)
        val prompt = promptText()

        AutoDevCoroutineScope.scope(project).launch {
            try {
                val flow: Flow<String> = LlmFactory.createCompletion(project).stream(prompt, "", false)
                val suggestion = StringBuilder()
                flow.collect {
                    AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)
                    suggestion.append(it)
                }

                AutoDevStatusService.notifyApplication(AutoDevStatus.Done)
                onFirstCompletion?.accept(suggestion.toString())
            } catch (e: Exception) {
                logger.error("Failed to generate code completion suggestion: ${e.message}")
                AutoDevStatusService.notifyApplication(AutoDevStatus.Error)
            }
        }
    }

    companion object {
        private val logger = logger<CodeCompletionTask>()
    }
}