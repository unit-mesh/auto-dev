package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.InsertUtil
import cc.unitmesh.devti.LLMCoroutineScope
import cc.unitmesh.devti.intentions.action.CodeCompletionIntention
import cc.unitmesh.devti.llms.LlmFactory
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

abstract class BaseCompletionTask(private val request: CodeCompletionRequest) :
    Task.Backgroundable(request.project, AutoDevBundle.message("intentions.chat.code.complete.name")) {

    private var isCanceled: Boolean = false
    abstract fun promptText(): String

    open fun keepHistory(): Boolean = true

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        indicator.fraction = 0.1
        indicator.text = AutoDevBundle.message("intentions.chat.code.test.step.prepare-context")

        val prompt = promptText()
        val flow: Flow<String> = LlmFactory().create(request.project).stream(prompt, "", keepHistory())
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
        val project = request.project
        var currentOffset = request.offset

        indicator.isIndeterminate = true
        indicator.fraction = 0.8
        indicator.text = AutoDevBundle.message("intentions.request.background.process.title")

        LLMCoroutineScope.scope(request.project).launch {
            val suggestion = StringBuilder()

            flow.cancellable().collect { char ->
                if (isCanceled) {
                    cancel()
                    return@collect
                }

                suggestion.append(char)
                invokeLater {
                    if (!isCanceled) {
                        InsertUtil.insertStreamingToDoc(project, char, editor, currentOffset)
                        currentOffset += char.length
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

    companion object {
        val logger = logger<CodeCompletionIntention>()
    }
}
