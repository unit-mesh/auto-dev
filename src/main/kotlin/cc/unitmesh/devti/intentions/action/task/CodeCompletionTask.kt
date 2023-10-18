package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.temporary.similar.chunks.SimilarChunksWithPaths
import cc.unitmesh.devti.llms.LlmProviderFactory
import cc.unitmesh.devti.LLMCoroutineScope
import cc.unitmesh.devti.InsertUtil
import cc.unitmesh.devti.intentions.action.CodeCompletionIntention
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import kotlin.jvm.internal.Ref

/**
 * The `CodeCompletionTask` class is responsible for performing code completion tasks in the background.
 * It extends the `Task.Backgroundable` class and is used to complete code based on a given code completion request.
 *
 * The class has the following properties:
 * - `request`: A private property of type `CodeCompletionRequest` that represents the code completion request.
 * - `LLMProviderFactory`: A private property of type `LlmProviderFactory` that is used to create an instance of the LLM provider factory.
 * - `chunksString`: A private property of type `String` that represents the query for similar chunks with paths.
 * - `commenter`: A private property of type `Commenter` that represents the commenter for the language of the code.
 * - `commentPrefix`: A private property of type `String` that represents the line comment prefix for the language of the code.
 * - `isCanceled`: A private property of type `Boolean` that indicates whether the code completion task has been canceled.
 * - `logger`: A companion object property of type `Logger` that is used for logging purposes.
 *
 * The class has the following methods:
 * - `run`: An overridden method that performs the code completion task. It takes a `ProgressIndicator` as a parameter.
 * - `onCancel`: An overridden method that is called when the code completion task is canceled.
 * - `promptText`: A private method that returns the prompt text for the code completion task.
 *
 * Note: This class does not have any public methods.
 */
class CodeCompletionTask(private val request: CodeCompletionRequest) :
    Task.Backgroundable(request.project, AutoDevBundle.message("intentions.chat.code.complete.name")) {

    private val providerFactory = LlmProviderFactory()

    private val chunksString = SimilarChunksWithPaths.createQuery(request.element, 60)
    private val commenter = LanguageCommenters.INSTANCE.forLanguage(request.element.language)
    private val commentPrefix = commenter?.lineCommentPrefix
    private var isCanceled: Boolean = false

    override fun run(indicator: ProgressIndicator) {
        val prompt = promptText()

        val flow: Flow<String> = providerFactory.connector(request.project).stream(prompt, "")
        logger.info("Prompt: $prompt")

        DumbAwareAction.create {
            isCanceled = true
        }.registerCustomShortcutSet(
            CustomShortcutSet(
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), null),
            ),
            request.editor.component
        )

        val editor = request.editor
        LLMCoroutineScope.scope(request.project).launch {
            val currentOffset = Ref.IntRef()
            currentOffset.element = request.offset

            val project = request.project
            val suggestion = StringBuilder()

            flow.cancellable().collect {
                if (isCanceled) {
                    cancel()
                    return@collect
                }

                suggestion.append(it)
                invokeLater {
                    if (!isCanceled) {
                        InsertUtil.insertStreamingToDoc(project, it, editor, currentOffset.element)
                        currentOffset.element += it.length
                    }
                }
            }

            logger.info("Suggestion: $suggestion")
        }
    }

    override fun onCancel() {
        this.isCanceled = true
        super.onCancel()
    }

    private fun promptText(): String {
        val documentLength = request.editor.document.textLength
        val prefix = if (request.offset > documentLength) {
            request.prefixText
        } else {
            val text = request.editor.document.text
            text.substring(0, request.offset)
        }

        return if (chunksString == null) {
            "complete code for given code: \n$prefix"
        } else {
            "complete code for given code: \n$commentPrefix\n$chunksString\n$prefix"
        }
    }

    companion object {
        val logger = logger<CodeCompletionIntention>()
    }
}

