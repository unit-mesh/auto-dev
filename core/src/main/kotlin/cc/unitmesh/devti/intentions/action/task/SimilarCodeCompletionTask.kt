package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.temporary.similar.chunks.SimilarChunksWithPaths
import com.intellij.lang.LanguageCommenters

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
class SimilarCodeCompletionTask(private val request: CodeCompletionRequest) : BaseCompletionTask(request) {
    private val chunksString = SimilarChunksWithPaths.createQuery(request.element!!, 60)
    private val commenter = LanguageCommenters.INSTANCE.forLanguage(request.element!!.language)
    private val commentPrefix = commenter?.lineCommentPrefix

    override fun keepHistory(): Boolean = false;

    val start = "code complete for given code, just return rest part of code. \n"
    val end = "\nreturn rest code:"

    override fun promptText(): String {
        val documentLength = request.editor.document.textLength
        val prefix = generatePrefix(documentLength)

        return if (chunksString == null) {
            "$start$prefix\n$end"
        } else {
            "$start: \n$commentPrefix\n$chunksString\n$prefix\n$end"
        }
    }

    private fun generatePrefix(documentLength: Int): String {
        val prefix = if (request.offset > documentLength) {
            request.prefixText
        } else {
            request.editor.document.text.substring(0, request.offset)
        }

        val prefixMaxLength =
            AutoDevSettingsState.maxTokenLength - start.length - (commentPrefix?.length ?: 0) - (chunksString?.length
                ?: 0)
        if (prefix.length >= prefixMaxLength){
            return prefix.substring(prefix.length - prefixMaxLength)
        }

        return prefix
    }
}

