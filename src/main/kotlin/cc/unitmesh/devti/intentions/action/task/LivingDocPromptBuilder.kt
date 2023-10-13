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
            is VariableContext -> variableInstruction(context)
            else -> null
        }
    }

    private fun variableInstruction(context: VariableContext): String? {
        if (context.name == null) return null
        return "Write documentation for given variable " + context.name + "."
    }

    private fun classInstruction(context: ClassContext): String? {
        if (context.name == null) return null
        return "Write documentation for given class " + context.name + "."
    }

    private fun methodInstruction(context: MethodContext): String? {
        if (context.name == null) return null
        var instruction = "Write documentation for given method " + context.name + "."
        if (context.paramNames.isNotEmpty()) {
            instruction = """
                $instruction
                ${documentation.parameterTagInstruction}
                """.trimIndent()
        }

        val returnType = context.returnType
        if (!returnType.isNullOrEmpty()) {
            instruction = """
                $instruction
                ${documentation.returnTagInstruction}
                """.trimIndent()
        }

        return instruction
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
            } ?: "Write documentation for given code. You should no return code, just documentation."

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

            instruction.append("Start your document here, no return code.\n")
            instruction.toString()
        }
    }

}