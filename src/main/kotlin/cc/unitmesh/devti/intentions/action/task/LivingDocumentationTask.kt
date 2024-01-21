package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.LivingDocumentation
import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.statusbar.AutoDevStatus
import cc.unitmesh.devti.statusbar.AutoDevStatusService
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.*

/**
 * The `LivingDocumentationTask` class represents a background task for generating living documentation.
 *
 * @property editor The editor in which the task is performed.
 * @property target The target element for which the living documentation is generated.
 * @property type The type of living documentation to be generated, defaulting to `LivingDocumentationType.COMMENT`.
 */
class LivingDocumentationTask(
    val editor: Editor,
    val target: PsiElement,
    val type: LivingDocumentationType = LivingDocumentationType.COMMENT,
    val documentation: LivingDocumentation,
) : Task.Backgroundable(editor.project, AutoDevBundle.message("intentions.request.background.process.title")) {
    override fun run(indicator: ProgressIndicator) {
        val builder = LivingDocPromptBuilder(editor, target, documentation, type)
        val prompt = builder.buildPrompt(project, target, "")
        AutoDevStatusService.notifyApplication(AutoDevStatus.InProgress)

        logger.info("Prompt: $prompt")

        val stream = LlmFactory().create(project).stream(prompt, "")

        var result = ""

        runBlocking {
            stream.collect {
                result += it
            }
        }

        logger.info("Result: $result")

        documentation.updateDoc(target, result, type, editor)
        AutoDevStatusService.notifyApplication(AutoDevStatus.Ready)
    }

    companion object {
        val logger = logger<LivingDocumentationTask>()
    }
}

