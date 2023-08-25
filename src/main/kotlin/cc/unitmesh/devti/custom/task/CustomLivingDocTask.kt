package cc.unitmesh.devti.custom.task

import cc.unitmesh.devti.custom.CustomDocumentationConfig
import cc.unitmesh.devti.llms.LlmProviderFactory
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.psi.PsiNameIdentifierOwner
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

class CustomLivingDocTask(
    val editor: Editor,
    val target: PsiNameIdentifierOwner,
    val config: CustomDocumentationConfig,
) :
    Task.Backgroundable(editor.project, config.title) {

    companion object {
        val logger = logger<CustomLivingDocTask>()
    }

    override fun run(indicator: ProgressIndicator) {
        val documentation = LivingDocumentation.forLanguage(target.language) ?: return
        val builder = CustomLivingDocPromptBuilder(editor, target, config, documentation)
        val prompt = builder.buildPrompt(project, target, config.prompt)

        logger.warn("Prompt: $prompt")

        val stream =
            LlmProviderFactory().connector(project).stream(prompt, "")

        var result = ""

        runBlocking {
            stream.cancellable().collect {
                result += it
            }
        }

        logger.warn("Result: $result")

        documentation.updateDoc(target, result, config.type, editor)
    }
}

