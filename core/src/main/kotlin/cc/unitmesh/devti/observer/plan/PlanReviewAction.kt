// filepath: /Volumes/source/ai/autocrud/core/src/main/kotlin/cc/unitmesh/devti/observer/plan/PlanReviewAction.kt
package cc.unitmesh.devti.observer.plan

import cc.unitmesh.devti.observer.agent.AgentStateService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.accept
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.ast.visitors.RecursiveVisitor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

class PlanReviewAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return
        val agentStateService = project.getService(AgentStateService::class.java)

        val currentPlan = agentStateService.getPlan()
        val plan = MarkdownPlanParser.formatPlanToMarkdown(currentPlan)

        /// call llm to evaluate the plan
        val allMessages = agentStateService.getAllMessages()
        val withOutCode = allMessages.map {
            removeAllMarkdownCode(it.content)
        }
    }
}

fun removeAllMarkdownCode(markdownContent: String): String {
    if (markdownContent.isEmpty()) return markdownContent

    val flavour = GFMFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownContent)

    val codeBlockReplacements = mutableListOf<Pair<IntRange, String>>()

    parsedTree.accept(object : RecursiveVisitor() {
        override fun visitNode(node: ASTNode) {
            if (node.type == MarkdownElementTypes.CODE_FENCE) {
                val language = extractCodeFenceLanguage(node, markdownContent)
                val replacement = "```$language\n// you can skip this part of the code.\n```"
                codeBlockReplacements.add(node.startOffset..node.endOffset to replacement)
            } else if (node.type == MarkdownElementTypes.CODE_BLOCK) {
                val replacement = "```\n// you can skip this part of the code.\n```"
                codeBlockReplacements.add(node.startOffset..node.endOffset to replacement)
            } else {
                super.visitNode(node)
            }
        }
    })

    codeBlockReplacements.sortByDescending { it.first.first }

    val result = StringBuilder(markdownContent)
    for ((range, replacement) in codeBlockReplacements) {
        result.replace(range.first, range.last, replacement)
    }

    return result.toString()
}

private fun extractCodeFenceLanguage(node: ASTNode, markdownContent: String): String {
    val nodeText = node.getTextInNode(markdownContent).toString()
    val firstLine = nodeText.lines().firstOrNull() ?: ""
    
    val languageMatch = Regex("^```(.*)$").find(firstLine.trim())
    return languageMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
}
