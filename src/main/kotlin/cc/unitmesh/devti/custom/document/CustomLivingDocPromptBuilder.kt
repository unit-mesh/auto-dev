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