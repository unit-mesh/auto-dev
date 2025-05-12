package cc.unitmesh.devti.language.middleware.builtin

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import cc.unitmesh.devti.devins.post.PostProcessorType
import cc.unitmesh.devti.devins.post.PostProcessorContext
import cc.unitmesh.devti.devins.post.PostProcessor
import cc.unitmesh.devti.util.parser.CodeFence

class ParseCodeProcessor : PostProcessor {
    override val processorName: String = PostProcessorType.ParseCode.handleName
    override val description: String = "`parseCode` will parse the markdown from llm response."

    override fun isApplicable(context: PostProcessorContext): Boolean = true

    override fun execute(project: Project, context: PostProcessorContext, console: ConsoleView?, args: List<Any>): String {
        val code = CodeFence.parse(context.genText ?: "")
        val codeText = code.text

        context.genTargetLanguage = code.language
        context.genTargetExtension = code.extension

        context.pipeData["output"] = codeText
        context.pipeData["code"] = codeText

        return codeText
    }
}
