package cc.unitmesh.devti.intentions.action.task

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.context.*
import cc.unitmesh.devti.context.base.LLMQueryContext
import cc.unitmesh.devti.llms.LLMProviderFactory
import cc.unitmesh.devti.provider.LivingDocumentation
import cc.unitmesh.devti.provider.LivingDocumentationType
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNameIdentifierOwner
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking


class LivingDocumentationTask(
    val editor: Editor,
    val target: PsiNameIdentifierOwner,
    val type: LivingDocumentationType = LivingDocumentationType.NORMAL,
) : Task.Backgroundable(editor.project, AutoDevBundle.message("intentions.request.background.process.title")) {
    override fun run(indicator: ProgressIndicator) {
        val documentation = LivingDocumentation.forLanguage(target.language) ?: return
        val builder = LivingDocumentationBuilder(editor, target, documentation, type)
        val prompt = builder.buildPrompt(project, target, "")

        val stream =
            LLMProviderFactory().connector(project).stream(prompt, "")

        var result = ""

        runBlocking {
            stream.collect {
                result += it
            }
        }

        println(result)
        documentation.updateDoc(target, result)
    }
}

class LivingDocumentationBuilder(
    val editor: Editor,
    val target: PsiNameIdentifierOwner,
    val documentation: LivingDocumentation,
    val type: LivingDocumentationType,
) {
    private val contextProviders = listOf(
        VariableContextProvider(false, false, false),
        ClassContextProvider(false),
        MethodContextProvider(false, false)
    )

    private fun contextInstruction(context: LLMQueryContext?): String? {
        return when (context) {
            is ClassContext -> classInstruction(context)
            is MethodContext -> methodInstruction(context)
            else -> null
        }
    }

    private fun classInstruction(context: ClassContext): String? {
        if (context.name == null) return null
        return "Write documentation for given class " + context.name
    }

    private fun methodInstruction(context: MethodContext): String? {
        if (context.name == null) return null
        return "Write documentation for given method " + context.name
    }

    fun buildPrompt(project: Project?, target: PsiNameIdentifierOwner, fallbackText: String): String {
        return ReadAction.compute<String, Throwable> {
            val instruction = StringBuilder(fallbackText)
            val element = this.contextProviders.firstNotNullOfOrNull { contextProvider ->
                contextInstruction(contextProvider.from(target))
            } ?: "write documentation for given code"

            instruction.append(element)
            instruction.append(" , do not return example code, do not use @author and @version tags")
            instruction.append(target.text)

            instruction.append("""${target.language.displayName}\n${target}\n```""")

            val startEndString = documentation.startEndString(type)
            instruction.append("\nstart your documentation here with ${startEndString.first} and ends with: ${startEndString.second} :\n```")

            instruction.toString()
        }
    }

}
