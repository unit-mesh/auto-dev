package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.context.*
import cc.unitmesh.devti.context.base.LLMCodeContext
import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * The `LivingDocPromptBuilder` class is responsible for building prompts for writing documentation.
 * It provides methods to generate instructions for documenting various code elements such as classes, methods, and variables.
 * The prompts are generated based on the provided `editor`, `target`, `documentation`, and `type`.
 * The class also contains a list of `contextProviders` that determine the context in which the documentation is being written.
 *
 * To use the `LivingDocPromptBuilder`, create an instance of the class and call the `buildPrompt` method.
 * The `buildPrompt` method takes the `project`, `target`, and `fallbackText` as parameters and returns the generated prompt.
 * The generated prompt includes instructions for documenting the code element specified by the `target`.
 *
 * Note that the `LivingDocPromptBuilder` is an open class, which means it can be subclassed and overridden.
 * It provides default implementations for the `editor`, `target`, `documentation`, and `type` properties,
 * but these can be overridden in subclasses if needed.
 *
 * Example usage:
 * ```
 * val promptBuilder = LivingDocPromptBuilder(editor, target, documentation, type)
 * val prompt = promptBuilder.buildPrompt(project, target, fallbackText)
 * ```
 *
 * @property editor The editor in which the documentation is being written.
 * @property target The code element for which the documentation is being written.
 * @property documentation The living documentation instance used for generating instructions.
 * @property type The type of living documentation being written.
 * @property contextProviders The list of context providers used to determine the context in which the documentation is being written.
 */
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
        return "Write documentation for given class " + context.name + ". You should not add documentation for methods."
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

            if (inOutString.isNotEmpty()) {
                instruction.append("\nCompare this snippet: \n")
                instruction.append(inOutString)
                instruction.append("\n")
            }

            instruction.append("```${target.language.displayName}\n${target.text}\n```")

            val startEndString = documentation.startEndString(type)
            instruction.append(documentation.forbiddenRules.joinToString { "\n- $it" })

            instruction.append("\n\nStart your documentation with ${startEndString.first} here, no code, and ends with `${startEndString.second}`.\n")
            instruction.toString()
        }
    }

}