package cc.unitmesh.devti.language.middleware.builtin

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import cc.unitmesh.devti.devins.post.PostProcessorType
import cc.unitmesh.devti.devins.post.PostProcessorContext
import cc.unitmesh.devti.devins.post.PostProcessor
import cc.unitmesh.devti.provider.PsiElementDataBuilder
import org.jetbrains.annotations.NonNls

class ParseCommentProcessor : PostProcessor {
    override val processorName: String = PostProcessorType.ParseComment.handleName
    override val description: String = "`parseComment` will parse the comment from the llm response"

    override fun isApplicable(context: PostProcessorContext): Boolean = true

    @NonNls
    fun preHandleDoc(newDoc: String): String {
        val newDocWithoutCodeBlock = newDoc.removePrefix("```java")
            .removePrefix("```")
            .removeSuffix("```")

        val fromSuggestion = buildDocFromSuggestion(newDocWithoutCodeBlock, "/**", "*/")
        return fromSuggestion
    }

    fun buildDocFromSuggestion(suggestDoc: String, commentStart: String, commentEnd: String): String {
        val startIndex = suggestDoc.indexOf(commentStart)
        if (startIndex < 0) {
            return ""
        }

        val docComment = suggestDoc.substring(startIndex)
        val endIndex = docComment.indexOf(commentEnd, commentStart.length)
        if (endIndex < 0) {
            return docComment + commentEnd
        }

        val substring = docComment.substring(0, endIndex + commentEnd.length)
        return substring
    }

    private fun getDocFromOutput(context: PostProcessorContext) =
        preHandleDoc(context.pipeData["output"] as String? ?: context.genText ?: "")

    override fun execute(project: Project, context: PostProcessorContext, console: ConsoleView?, args: List<Any>): String {
        val defaultComment: String = getDocFromOutput(context)
        val currentFile = context.currentFile ?: return defaultComment

        val comment = PsiElementDataBuilder.forLanguage(currentFile.language)
            ?.parseComment(project, defaultComment) ?: return defaultComment

        return comment
    }
}
