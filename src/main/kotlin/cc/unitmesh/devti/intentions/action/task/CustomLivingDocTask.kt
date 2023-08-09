package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.custom.CustomDocumentationConfig
import cc.unitmesh.devti.custom.LivingDocumentationType
import cc.unitmesh.devti.llms.LLMProviderFactory
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNameIdentifierOwner
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
            LLMProviderFactory().connector(project).stream(prompt, "")

        var result = ""

        runBlocking {
            stream.collect {
                result += it
            }
        }

        logger.warn("Result: $result")

        documentation.updateDoc(target, result, config.type, editor)
    }
}

class CustomLivingDocPromptBuilder(
    override val editor: Editor,
    override val target: PsiNameIdentifierOwner,
    val config: CustomDocumentationConfig,
    override val documentation: LivingDocumentation,
) : LivingDocPromptBuilder(editor, target, documentation, LivingDocumentationType.CUSTOM) {
    override fun buildPrompt(project: Project?, target: PsiNameIdentifierOwner, fallbackText: String): String {
        return ReadAction.compute<String, Throwable> {
            val instruction = StringBuilder(fallbackText)

            var inOutString = ""
            this.contextProviders.firstNotNullOfOrNull { contextProvider ->
                when (val llmQueryContext = contextProvider.from(target)) {
                    is MethodContext -> {
                        inOutString = llmQueryContext.inputOutputString()
                    }
                }
            }

            if (inOutString.isNotEmpty()) {
                instruction.append("\nCompare this snippet: \n")
                instruction.append(inOutString)
                instruction.append("\n")
            }

            val lang = target.language.displayName;
            if (config.example != null) {
                instruction.append("Q: ```$lang\n${config.example.question}\n```\n")
                instruction.append("A: ${config.example.answer}\n")
                instruction.append("Q: ```$lang\n${target.text}\n```\n")
                instruction.append("A: ")
            }

            return@compute instruction.toString();
        }
    }
}
