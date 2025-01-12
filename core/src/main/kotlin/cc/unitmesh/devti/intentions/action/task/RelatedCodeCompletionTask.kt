package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.application.runReadAction

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
class RelatedCodeCompletionTask(private val request: CodeCompletionRequest) : BaseCompletionTask(request) {
    override fun keepHistory(): Boolean = false;
    override fun promptText(): String {
        val lang = request.element?.language ?: throw Exception("element language is null")
        val prompter = ContextPrompter.prompter(lang.displayName)
        prompter
            .initContext(
                ChatActionType.CODE_COMPLETE,
                request.prefixText,
                runReadAction { request.element.containingFile },
                project,
                request.offset,
                request.element
            )

        return prompter.requestPrompt()
    }
}

