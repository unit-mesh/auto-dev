package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.context.*
import cc.unitmesh.devti.context.base.LLMCodeContext
import cc.unitmesh.devti.custom.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNameIdentifierOwner

open class LivingDocPromptBuilder(
    open val editor: Editor,
    open val target: PsiNameIdentifierOwner,
    open val documentation: LivingDocumentation,
    val type: LivingDocumentationType,
) {
    private val toolName = documentation.docToolName

    protected val contextProviders = listOf(
        VariableContextProvider(false, false, false),
        ClassContextProvider(false),
        MethodContextProvider(false, false)
    )

    private fun contextInstruction(context: LLMCodeContext?): String? {
        return when (context) {
            is ClassContext -> classInstruction(context)
            is MethodContext -> methodInstruction(context)
            else -> null
        }
    }

    private fun classInstruction(context: ClassContext): String? {
        if (context.name == null) return null
        return "Write $toolName for given class " + context.name + "."
    }

    private fun methodInstruction(context: MethodContext): String? {
        if (context.name == null) return null
        return "Write $toolName for given method " + context.name + "."
    }

    open fun buildPrompt(project: Project?, target: PsiNameIdentifierOwner, fallbackText: String): String {
        return ReadAction.compute<String, Throwable> {
            val instruction = StringBuilder(fallbackText)

            var inOutString = ""
            val basicInstruction = this.contextProviders.firstNotNullOfOrNull { contextProvider ->
                val llmQueryContext = contextProvider.from(target)
                when (llmQueryContext) {
                    is MethodContext -> {
                        inOutString = llmQueryContext.inputOutputString()
                    }
                }
                contextInstruction(llmQueryContext)
            } ?: "Write documentation for given code"

            instruction.append(basicInstruction)
            instruction.append(documentation.forbiddenRules.joinToString { "\n- $it" })

            if (inOutString.isNotEmpty()) {
                instruction.append("\nCompare this snippet: \n")
                instruction.append(inOutString)
                instruction.append("\n")
            }

            instruction.append("```${target.language.displayName}\n${target.text}\n```")

            val startEndString = documentation.startEndString(type)
            instruction.append("\nYou should start with `${startEndString.first}`\nYou should end with ends with: `${startEndString.second}`\n")

            documentation.forbiddenRules.forEach {
                instruction.append("- $it\n")
            }

            instruction.append("Return your comment here, no code.\n")
            instruction.toString()
        }
    }

}