package cc.unitmesh.devins.idea.renderer.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.renderer.markdown.MarkdownInlineRenderer.appendMarkdownChildren
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import java.awt.Desktop
import java.net.URI

/**
 * Full-featured Jewel-themed Markdown renderer using JetBrains' intellij-markdown parser.
 * Supports:
 * - Headers (H1-H6)
 * - Paragraphs with inline formatting (bold, italic, strikethrough, code)
 * - Code blocks and fenced code with language detection
 * - Block quotes
 * - Ordered and unordered lists (with nesting)
 * - Links (inline and auto-detected)
 * - Tables (GFM)
 * - Horizontal rules
 * - Checkboxes (GFM task lists)
 */
@Composable
fun JewelMarkdownRenderer(
    content: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onLinkClick: ((String) -> Unit)? = null
) {
    val flavour = remember { GFMFlavourDescriptor() }
    val parser = remember { MarkdownParser(flavour) }
    val tree = remember(content) { parser.buildMarkdownTreeFromString(content) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RenderNode(node = tree, content = content, onLinkClick = onLinkClick)
    }
}

@Composable
private fun RenderNode(
    node: ASTNode,
    content: String,
    listDepth: Int = 0,
    onLinkClick: ((String) -> Unit)? = null
) {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE -> {
            node.children.forEach { child ->
                RenderNode(node = child, content = content, listDepth = listDepth, onLinkClick = onLinkClick)
            }
        }
        MarkdownElementTypes.PARAGRAPH -> {
            MarkdownParagraph(node = node, content = content, onLinkClick = onLinkClick)
        }
        MarkdownElementTypes.ATX_1 -> {
            MarkdownHeader(node = node, content = content, level = 1)
        }
        MarkdownElementTypes.ATX_2 -> {
            MarkdownHeader(node = node, content = content, level = 2)
        }
        MarkdownElementTypes.ATX_3 -> {
            MarkdownHeader(node = node, content = content, level = 3)
        }
        MarkdownElementTypes.ATX_4 -> {
            MarkdownHeader(node = node, content = content, level = 4)
        }
        MarkdownElementTypes.ATX_5 -> {
            MarkdownHeader(node = node, content = content, level = 5)
        }
        MarkdownElementTypes.ATX_6 -> {
            MarkdownHeader(node = node, content = content, level = 6)
        }
        MarkdownElementTypes.SETEXT_1 -> {
            MarkdownHeader(node = node, content = content, level = 1)
        }
        MarkdownElementTypes.SETEXT_2 -> {
            MarkdownHeader(node = node, content = content, level = 2)
        }
        MarkdownElementTypes.CODE_FENCE -> {
            MarkdownCodeFence(node = node, content = content)
        }
        MarkdownElementTypes.CODE_BLOCK -> {
            MarkdownCodeBlock(node = node, content = content)
        }
        MarkdownElementTypes.BLOCK_QUOTE -> {
            MarkdownBlockQuote(node = node, content = content, onLinkClick = onLinkClick)
        }
        MarkdownElementTypes.UNORDERED_LIST -> {
            MarkdownUnorderedList(node = node, content = content, depth = listDepth, onLinkClick = onLinkClick)
        }
        MarkdownElementTypes.ORDERED_LIST -> {
            MarkdownOrderedList(node = node, content = content, depth = listDepth, onLinkClick = onLinkClick)
        }
        MarkdownTokenTypes.HORIZONTAL_RULE -> {
            MarkdownHorizontalRule()
        }
        GFMElementTypes.TABLE -> {
            MarkdownTable(node = node, content = content)
        }
        GFMElementTypes.STRIKETHROUGH -> {
            // Handled inline
        }
        else -> {
            // For other node types, try to render children
            if (node.children.isNotEmpty()) {
                node.children.forEach { child ->
                    RenderNode(node = child, content = content, listDepth = listDepth, onLinkClick = onLinkClick)
                }
            }
        }
    }
}

@Composable
private fun MarkdownHeader(node: ASTNode, content: String, level: Int) {
    val text = MarkdownTextParser.extractHeaderText(node, content)
    val (fontSize, fontWeight) = when (level) {
        1 -> 24.sp to FontWeight.Bold
        2 -> 20.sp to FontWeight.Bold
        3 -> 18.sp to FontWeight.SemiBold
        4 -> 16.sp to FontWeight.SemiBold
        5 -> 14.sp to FontWeight.Medium
        else -> 13.sp to FontWeight.Medium
    }
    val verticalPadding = when (level) {
        1 -> 8.dp
        2 -> 6.dp
        else -> 4.dp
    }

    Text(
        text = text,
        style = JewelTheme.defaultTextStyle.copy(
            fontSize = fontSize,
            fontWeight = fontWeight
        ),
        modifier = Modifier.padding(vertical = verticalPadding)
    )
}

@Composable
private fun MarkdownParagraph(
    node: ASTNode,
    content: String,
    onLinkClick: ((String) -> Unit)? = null
) {
    // Capture the color in composable context
    val codeBackground = JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f)

    val annotatedString = buildAnnotatedString {
        appendMarkdownChildren(node, content, codeBackground)
    }

    if (annotatedString.getStringAnnotations("URL", 0, annotatedString.length).isNotEmpty()) {
        @Suppress("DEPRECATION")
        ClickableText(
            text = annotatedString,
            style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp),
            modifier = Modifier.padding(vertical = 2.dp),
            onClick = { offset ->
                annotatedString.getStringAnnotations("URL", offset, offset)
                    .firstOrNull()?.let { annotation ->
                        if (onLinkClick != null) {
                            onLinkClick(annotation.item)
                        } else {
                            try {
                                Desktop.getDesktop().browse(URI(annotation.item))
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
            }
        )
    } else {
        Text(
            text = annotatedString,
            style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp),
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}

// ============ Code Block Components ============

@Composable
private fun MarkdownCodeFence(node: ASTNode, content: String) {
    val language = MarkdownTextParser.extractCodeFenceLanguage(node, content)
    val codeText = MarkdownTextParser.extractCodeFenceContent(node, content)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                JewelTheme.globalColors.panelBackground.copy(alpha = 0.6f),
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                JewelTheme.globalColors.borders.normal,
                RoundedCornerShape(6.dp)
            )
    ) {
        // Language header if present
        if (!language.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = JewelTheme.globalColors.text.info
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.5f))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = codeText,
                style = JewelTheme.defaultTextStyle.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
private fun MarkdownCodeBlock(node: ASTNode, content: String) {
    val codeText = node.getTextInNode(content).toString()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                JewelTheme.globalColors.panelBackground.copy(alpha = 0.6f),
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                JewelTheme.globalColors.borders.normal,
                RoundedCornerShape(6.dp)
            )
            .padding(12.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = codeText,
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        )
    }
}

@Composable
private fun MarkdownBlockQuote(
    node: ASTNode,
    content: String,
    onLinkClick: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f),
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(AutoDevColors.Blue.c400, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            node.children.forEach { child ->
                if (child.type != MarkdownTokenTypes.BLOCK_QUOTE) {
                    RenderNode(node = child, content = content, onLinkClick = onLinkClick)
                }
            }
        }
    }
}

@Composable
private fun MarkdownUnorderedList(
    node: ASTNode,
    content: String,
    depth: Int = 0,
    onLinkClick: ((String) -> Unit)? = null
) {
    val bulletChar = when (depth % 3) {
        0 -> "\u2022" // •
        1 -> "\u25E6" // ◦
        else -> "\u25AA" // ▪
    }
    val indent = (depth * 16).dp

    Column(modifier = Modifier.padding(start = indent, top = 2.dp, bottom = 2.dp)) {
        node.children.forEach { child ->
            if (child.type == MarkdownElementTypes.LIST_ITEM) {
                MarkdownListItem(
                    node = child,
                    content = content,
                    bullet = "$bulletChar ",
                    depth = depth,
                    onLinkClick = onLinkClick
                )
            }
        }
    }
}

@Composable
private fun MarkdownOrderedList(
    node: ASTNode,
    content: String,
    depth: Int = 0,
    onLinkClick: ((String) -> Unit)? = null
) {
    val indent = (depth * 16).dp
    var index = 1

    Column(modifier = Modifier.padding(start = indent, top = 2.dp, bottom = 2.dp)) {
        node.children.forEach { child ->
            if (child.type == MarkdownElementTypes.LIST_ITEM) {
                MarkdownListItem(
                    node = child,
                    content = content,
                    bullet = "${index++}. ",
                    depth = depth,
                    onLinkClick = onLinkClick
                )
            }
        }
    }
}

@Composable
private fun MarkdownListItem(
    node: ASTNode,
    content: String,
    bullet: String,
    depth: Int,
    onLinkClick: ((String) -> Unit)? = null
) {
    // Check for GFM checkbox
    val hasCheckbox = node.children.any {
        it.type == GFMTokenTypes.CHECK_BOX ||
        it.children.any { c -> c.type == GFMTokenTypes.CHECK_BOX }
    }

    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (!hasCheckbox) {
            Text(
                text = bullet,
                style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp),
                modifier = Modifier.width(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            node.children.forEach { child ->
                when (child.type) {
                    MarkdownElementTypes.PARAGRAPH -> {
                        MarkdownParagraph(node = child, content = content, onLinkClick = onLinkClick)
                    }
                    MarkdownElementTypes.UNORDERED_LIST -> {
                        MarkdownUnorderedList(node = child, content = content, depth = depth + 1, onLinkClick = onLinkClick)
                    }
                    MarkdownElementTypes.ORDERED_LIST -> {
                        MarkdownOrderedList(node = child, content = content, depth = depth + 1, onLinkClick = onLinkClick)
                    }
                    GFMTokenTypes.CHECK_BOX -> {
                        val isChecked = child.getTextInNode(content).toString().contains("x", ignoreCase = true)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isChecked) "\u2611 " else "\u2610 ", // ☑ or ☐
                                style = JewelTheme.defaultTextStyle.copy(fontSize = 14.sp)
                            )
                        }
                    }
                    else -> {
                        RenderNode(node = child, content = content, listDepth = depth + 1, onLinkClick = onLinkClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownHorizontalRule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .background(JewelTheme.globalColors.borders.normal)
    )
}

