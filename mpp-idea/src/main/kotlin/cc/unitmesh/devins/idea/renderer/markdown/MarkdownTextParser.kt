package cc.unitmesh.devins.idea.renderer.markdown

import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode

/**
 * Utility functions for parsing and extracting text from Markdown AST nodes.
 * These functions are pure and can be easily tested without Compose dependencies.
 */
object MarkdownTextParser {

    /**
     * Extract header text, removing the # prefix.
     * Supports both ATX headers (# Header) and SETEXT headers (underlined).
     */
    fun extractHeaderText(node: ASTNode, content: String): String {
        // For ATX headers, find the ATX_CONTENT child
        val contentNode = node.findChildOfType(MarkdownTokenTypes.ATX_CONTENT)
        if (contentNode != null) {
            return contentNode.getTextInNode(content).toString().trim()
        }

        // For SETEXT headers, find the SETEXT_CONTENT child
        val setextContent = node.findChildOfType(MarkdownTokenTypes.SETEXT_CONTENT)
        if (setextContent != null) {
            return setextContent.getTextInNode(content).toString().trim()
        }

        // Fallback: remove # prefix manually
        val fullText = node.getTextInNode(content).toString()
        return fullText.trimStart('#').trim()
    }

    /**
     * Extract code fence content, removing the ``` markers and language identifier.
     */
    fun extractCodeFenceContent(node: ASTNode, content: String): String {
        val children = node.children
        if (children.size < 3) return ""

        // Find the start of actual code content (after FENCE_LANG and EOL)
        var startIndex = 0
        for (i in children.indices) {
            if (children[i].type == MarkdownTokenTypes.EOL) {
                startIndex = i + 1
                break
            }
        }

        // Find the end (before CODE_FENCE_END)
        var endIndex = children.size - 1
        for (i in children.indices.reversed()) {
            if (children[i].type == MarkdownTokenTypes.CODE_FENCE_END) {
                endIndex = i - 1
                break
            }
        }

        if (startIndex > endIndex) return ""

        // Collect code content
        val codeBuilder = StringBuilder()
        for (i in startIndex..endIndex) {
            codeBuilder.append(children[i].getTextInNode(content))
        }

        return codeBuilder.toString().trimEnd()
    }

    /**
     * Extract clean text from a table cell node.
     * Strips markdown formatting characters.
     */
    fun extractCellText(cell: ASTNode, content: String): String {
        return cell.getTextInNode(content).toString()
            .replace("|", "")
            .replace("`", "")
            .replace("**", "")
            .replace("*", "")
            .trim()
    }

    /**
     * Extract text from inline code span, removing backticks.
     */
    fun extractCodeSpanText(node: ASTNode, content: String): String {
        return node.children
            .filter { it.type != MarkdownTokenTypes.BACKTICK }
            .joinToString("") { it.getTextInNode(content).toString() }
            .trim()
    }

    /**
     * Extract language identifier from a code fence node.
     */
    fun extractCodeFenceLanguage(node: ASTNode, content: String): String? {
        return node.findChildOfType(MarkdownTokenTypes.FENCE_LANG)
            ?.getTextInNode(content)?.toString()?.trim()
    }

    /**
     * Extract link text from an inline link node.
     */
    fun extractLinkText(node: ASTNode, content: String): String {
        val linkText = node.findChildOfType(org.intellij.markdown.MarkdownElementTypes.LINK_TEXT)
        return linkText?.children?.filter { it.type == MarkdownTokenTypes.TEXT }
            ?.joinToString("") { it.getTextInNode(content).toString() }
            ?: node.getTextInNode(content).toString()
    }

    /**
     * Extract link destination URL from an inline link node.
     */
    fun extractLinkDestination(node: ASTNode, content: String): String {
        val linkDest = node.findChildOfType(org.intellij.markdown.MarkdownElementTypes.LINK_DESTINATION)
        return linkDest?.getTextInNode(content)?.toString() ?: ""
    }

    /**
     * Extract image alt text from an image node.
     */
    fun extractImageAltText(node: ASTNode, content: String): String {
        return node.findChildOfType(org.intellij.markdown.MarkdownElementTypes.LINK_TEXT)
            ?.getTextInNode(content)?.toString()?.trim('[', ']') ?: "image"
    }

    /**
     * Extract autolink URL, removing angle brackets.
     */
    fun extractAutoLinkUrl(node: ASTNode, content: String): String {
        return node.getTextInNode(content).toString().trim('<', '>')
    }
}

