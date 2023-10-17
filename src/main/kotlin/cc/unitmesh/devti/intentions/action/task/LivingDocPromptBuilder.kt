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
            } ?: "Write documentation for given code. "

            instruction.append(basicInstruction)
            instruction.append("\nYou should just document the class, not the methods.\n")

            if (inOutString.isNotEmpty()) {
                instruction.append("\nCompare this snippet: \n")
                instruction.append(inOutString)
                instruction.append("\n")
            }

            instruction.append("```${target.language.displayName}\n${target.text}\n```")

            val startEndString = documentation.startEndString(type)
            instruction.append(documentation.forbiddenRules.joinToString { "\n- $it" })

            instruction.append("""You should end with: `${startEndString.second}`""")

            instruction.append("\n\nStart your documentation with ${startEndString.first}, no return code.\n")
            instruction.toString()
        }
    }

}