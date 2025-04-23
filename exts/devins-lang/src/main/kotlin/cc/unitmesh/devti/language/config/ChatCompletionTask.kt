package cc.unitmesh.devti.language.config

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.llms.cancelHandler
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.util.TextRange

import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAwareAction
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

open class ChatCompletionTask(private val request: ShireCodeCompletionRequest) :
    ShireInteractionTask(request.project, AutoDevBundle.message("intentions.chat.code.complete.name"), request.postExecute) {
    private val logger = logger<ChatCompletionTask>()

    private var isCanceled: Boolean = false

    private var cancelCallback: ((String) -> Unit)? = null

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = false
        indicator.fraction = 0.1
        indicator.text = AutoDevBundle.message("intentions.step.prepare-context")

        val flow: Flow<String> = LlmFactory.create(request.project)!!.stream(request.userPrompt, "", false)
        logger.info("Prompt: ${request.userPrompt}")

        val shortcutAction = DumbAwareAction.create {
            isCanceled = true
        }.apply {
            registerCustomShortcutSet(
                CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), null),
                ),
                request.editor.component
            )
        }

        val editor = request.editor
        val project = request.project

        var currentOffset = request.startOffset
        val modifyStart = request.startOffset

        indicator.isIndeterminate = false
        indicator.fraction = 0.5
        indicator.text = AutoDevBundle.message("intentions.request.background.process.title")

        AutoDevCoroutineScope.scope(request.project).launch {
            val suggestion = StringBuilder()

            flow.cancelHandler { cancelCallback = it }.cancellable().collect { char ->
                if (isCanceled) {
                    cancel()
                    return@collect
                }

                suggestion.append(char)

                invokeLater {
                    if (!isCanceled && !request.isReplacement) {
                        if (request.isInsertBefore) {
                            InsertUtil.insertStreamingToDoc(project, char, editor, currentOffset)
                            currentOffset += char.length
                        } else {
                            InsertUtil.insertStreamingToDoc(project, char, editor, currentOffset)
                            currentOffset += char.length
                        }
                    }
                }
            }

            val modifyEnd = currentOffset

            if (request.isReplacement) {
                val parsedContent = CodeFence.parse(suggestion.toString()).text
                InsertUtil.replaceText(project, editor, parsedContent)
            }

            indicator.fraction = 0.8
            logger.info("Suggestion: $suggestion")

            val textRange = TextRange(modifyStart, modifyEnd)

            request.postExecute.invoke(suggestion.toString(), textRange)
            indicator.fraction = 1.0
        }.invokeOnCompletion {
            shortcutAction.unregisterCustomShortcutSet(editor.component)
        }
    }

    override fun onThrowable(error: Throwable) {
        super.onThrowable(error)
    }

    override fun onCancel() {
        this.isCanceled = true
        this.cancelCallback?.invoke("This job is canceled")
        super.onCancel()
    }
}
