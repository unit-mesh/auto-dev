package cc.unitmesh.devti.custom.document

import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.psi.PsiElement
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

class CustomLivingDocTask(
    val editor: Editor,
    val target: PsiElement,
    val config: CustomDocumentationConfig,
) :
    Task.Backgroundable(editor.project, config.title) {
    private val logger = logger<CustomLivingDocTask>()

    override fun run(indicator: ProgressIndicator) {
        val documentation = LivingDocumentation.forLanguage(target.language) ?: return
        val builder = CustomLivingDocPromptBuilder(editor, target, config, documentation)
        val prompt = builder.buildPrompt(project, target, config.prompt)

        logger.info("Prompt: $prompt")

        val stream =
            LlmFactory().create(project).stream(prompt, "", false)

        var result = ""

        runBlocking {
            stream.cancellable().collect {
                result += it
            }
        }

        logger.info("Result: $result")

        documentation.updateDoc(target, result, config.type, editor)
    }
}

