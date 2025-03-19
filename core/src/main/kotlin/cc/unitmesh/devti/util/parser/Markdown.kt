package cc.unitmesh.devti.util.parser

import org.commonmark.Extension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.accept
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.ast.visitors.RecursiveVisitor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

object MarkdownCodeHelper {
    fun parseCodeFromString(markdown: String): List<String> {
        val extensions: List<Extension> = listOf(TablesExtension.create())
        val parser: Parser = Parser.builder()
            .extensions(extensions)
            .build()

        val node: Node = parser.parse(markdown)
        val visitor = CodeVisitor()
        node.accept(visitor)

        if (visitor.code.isEmpty()) {
            return listOf(markdown)
        }

        return visitor.code
    }

    /**
     * Removes all Markdown code blocks from the provided Markdown content and replaces them with a placeholder.
     * This function is useful when you want to strip out code blocks from a Markdown string while preserving the structure.
     *
     * The function identifies both fenced code blocks (e.g., ```kotlin```) and inline code blocks (e.g., `code`),
     * and replaces them with a placeholder text indicating that the code can be skipped.
     *
     * @param markdownContent The input Markdown string from which code blocks should be removed.
     * @return A new string where all Markdown code blocks have been replaced with placeholders.
     *
     * Example:
     * ```
     * Input:
     * "Here is some text with a code block:\n```kotlin\nfun main() {}\n```"
     *
     * Output:
     * "Here is some text with a code block:\n```kotlin\n// you can skip this part of the code.\n```"
     * ```
     */
    fun removeAllMarkdownCode(markdownContent: String): String {
        if (markdownContent.isEmpty()) return markdownContent

        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownContent)

        val codeBlockReplacements = mutableListOf<Pair<IntRange, String>>()

        parsedTree.accept(object : RecursiveVisitor() {
            override fun visitNode(node: ASTNode) {
                when (node.type) {
                    MarkdownElementTypes.CODE_FENCE -> {
                        val language = extractCodeFenceLanguage(node, markdownContent)
                        val replacement = "```$language\n// you can skip this part of the code.\n```"
                        codeBlockReplacements.add(node.startOffset..node.endOffset to replacement)
                    }

                    MarkdownElementTypes.CODE_BLOCK -> {
                        val replacement = "```\n// you can skip this part of the code.\n```"
                        codeBlockReplacements.add(node.startOffset..node.endOffset to replacement)
                    }

                    else -> {
                        super.visitNode(node)
                    }
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

    fun extractCodeFenceLanguage(node: ASTNode, markdownContent: String): String {
        val nodeText = node.getTextInNode(markdownContent).toString()
        val firstLine = nodeText.lines().firstOrNull() ?: ""

        val languageMatch = Regex("^```(.*)$").find(firstLine.trim())
        return languageMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

}

internal class CodeVisitor : AbstractVisitor() {
    var code = listOf<String>()

    override fun visit(fencedCodeBlock: FencedCodeBlock?) {
        if (fencedCodeBlock?.literal != null) {
            this.code += fencedCodeBlock.literal
        }
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock?) {
        super.visit(indentedCodeBlock)
    }
}