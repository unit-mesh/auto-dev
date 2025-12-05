package cc.unitmesh.devti.custom.document

import cc.unitmesh.devti.context.MethodContext
import cc.unitmesh.devti.intentions.action.task.LivingDocPromptBuilder
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class CustomLivingDocPromptBuilder(
    override val editor: Editor,
    override val target: PsiElement,
    val config: CustomDocumentationConfig,
    override val documentation: LivingDocumentation,
) : LivingDocPromptBuilder(editor, target, documentation, LivingDocumentationType.CUSTOM) {
    override fun buildPrompt(project: Project?, target: PsiElement, fallbackText: String): String {
        return ReadAction.compute<String, Throwable> {
            val instruction = StringBuilder(fallbackText)

            var inOutString = ""
            val context = contextProviders.firstNotNullOfOrNull { contextProvider ->
                val llmQueryContext = contextProvider.from(target)

                if (llmQueryContext != null) {
                    return@firstNotNullOfOrNull llmQueryContext
                }

                return@firstNotNullOfOrNull null
            }

            if (context != null) {
                val related = "\nHere is related context information of the method\n\n```${target.language}\n" + context.format() + "\n```\n"
                instruction.append(related)
            }

            when (context) {
                is MethodContext -> {
                    inOutString = context.inputOutputString()
                }
            }

            if (inOutString.isNotEmpty()) {
                instruction.append("\nInput and output: \n")
                instruction.append(inOutString)
                instruction.append("\n")
            }

            val lang = target.language.displayName;
            if (config.example != null) {
                instruction.append("Examples: \n")
                instruction.append("Question: ```$lang\n${config.example.question}\n```\n")
                instruction.append("Answer: ${config.example.answer}\n")
                instruction.append("Question: ```$lang\n${target.text}\n```\n")
                instruction.append("Answer: ")
            }

            return@compute instruction.toString();
        }
    }
}