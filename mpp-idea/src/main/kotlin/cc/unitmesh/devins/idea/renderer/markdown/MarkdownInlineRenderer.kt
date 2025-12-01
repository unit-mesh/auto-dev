package cc.unitmesh.devins.idea.renderer.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

/**
 * Utility object for rendering inline Markdown formatting to AnnotatedString.
 * Handles bold, italic, strikethrough, code spans, links, and images.
 */
object MarkdownInlineRenderer {

    /**
     * Build annotated string with inline formatting support.
     * Recursively processes child nodes and applies appropriate styles.
     */
    fun AnnotatedString.Builder.appendMarkdownChildren(
        node: ASTNode,
        content: String,
        codeBackground: Color
    ) {
        node.children.forEach { child ->
            when (child.type) {
                MarkdownTokenTypes.TEXT -> {
                    append(child.getTextInNode(content).toString())
                }
                MarkdownTokenTypes.WHITE_SPACE -> append(" ")
                MarkdownTokenTypes.EOL -> append(" ")
                MarkdownTokenTypes.SINGLE_QUOTE -> append("'")
                MarkdownTokenTypes.DOUBLE_QUOTE -> append("\"")
                MarkdownTokenTypes.LPAREN -> append("(")
                MarkdownTokenTypes.RPAREN -> append(")")
                MarkdownTokenTypes.LBRACKET -> append("[")
                MarkdownTokenTypes.RBRACKET -> append("]")
                MarkdownTokenTypes.LT -> append("<")
                MarkdownTokenTypes.GT -> append(">")
                MarkdownTokenTypes.COLON -> append(":")
                MarkdownTokenTypes.EXCLAMATION_MARK -> append("!")
                MarkdownTokenTypes.HARD_LINE_BREAK -> append("\n")

                MarkdownElementTypes.EMPH -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendMarkdownChildren(child, content, codeBackground)
                    }
                }
                MarkdownElementTypes.STRONG -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendMarkdownChildren(child, content, codeBackground)
                    }
                }
                GFMElementTypes.STRIKETHROUGH -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        appendMarkdownChildren(child, content, codeBackground)
                    }
                }
                MarkdownElementTypes.CODE_SPAN -> {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        fontSize = 12.sp
                    )) {
                        val codeText = MarkdownTextParser.extractCodeSpanText(child, content)
                        append(" $codeText ")
                    }
                }
                MarkdownElementTypes.INLINE_LINK -> {
                    appendInlineLink(child, content)
                }
                MarkdownElementTypes.AUTOLINK -> {
                    appendAutoLink(child, content)
                }
                GFMTokenTypes.GFM_AUTOLINK -> {
                    val url = child.getTextInNode(content).toString()
                    pushStringAnnotation("URL", url)
                    withStyle(SpanStyle(
                        color = AutoDevColors.Blue.c400,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(url)
                    }
                    pop()
                }
                MarkdownElementTypes.SHORT_REFERENCE_LINK,
                MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                    appendMarkdownChildren(child, content, codeBackground)
                }
                MarkdownElementTypes.IMAGE -> {
                    val altText = MarkdownTextParser.extractImageAltText(child, content)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append("[$altText]")
                    }
                }
                else -> {
                    if (child.children.isNotEmpty()) {
                        appendMarkdownChildren(child, content, codeBackground)
                    }
                }
            }
        }
    }

    /**
     * Append an inline link with URL annotation.
     */
    fun AnnotatedString.Builder.appendInlineLink(node: ASTNode, content: String) {
        val text = MarkdownTextParser.extractLinkText(node, content)
        val url = MarkdownTextParser.extractLinkDestination(node, content)

        pushStringAnnotation("URL", url)
        withStyle(SpanStyle(
            color = AutoDevColors.Blue.c400,
            textDecoration = TextDecoration.Underline
        )) {
            append(text)
        }
        pop()
    }

    /**
     * Append an autolink with URL annotation.
     */
    fun AnnotatedString.Builder.appendAutoLink(node: ASTNode, content: String) {
        val url = MarkdownTextParser.extractAutoLinkUrl(node, content)
        pushStringAnnotation("URL", url)
        withStyle(SpanStyle(
            color = AutoDevColors.Blue.c400,
            textDecoration = TextDecoration.Underline
        )) {
            append(url)
        }
        pop()
    }
}

