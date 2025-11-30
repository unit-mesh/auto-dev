package cc.unitmesh.devins.idea.renderer.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Simple Jewel-themed Markdown renderer using JetBrains' intellij-markdown parser.
 * This avoids the version mismatch issues with mikepenz library.
 */
@Composable
fun SimpleJewelMarkdown(
    content: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val flavour = remember { GFMFlavourDescriptor() }
    val parser = remember { MarkdownParser(flavour) }
    val tree = remember(content) { parser.buildMarkdownTreeFromString(content) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RenderNode(node = tree, content = content)
    }
}

@Composable
private fun RenderNode(node: ASTNode, content: String) {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE -> {
            node.children.forEach { child ->
                RenderNode(node = child, content = content)
            }
        }
        MarkdownElementTypes.PARAGRAPH -> {
            val text = node.getTextInNode(content).toString().trim()
            Text(
                text = text,
                style = JewelTheme.defaultTextStyle.copy(fontSize = 13.sp),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        MarkdownElementTypes.ATX_1 -> {
            val text = extractHeaderText(node, content)
            Text(
                text = text,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
        MarkdownElementTypes.ATX_2 -> {
            val text = extractHeaderText(node, content)
            Text(
                text = text,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
        MarkdownElementTypes.ATX_3, MarkdownElementTypes.ATX_4, 
        MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 -> {
            val text = extractHeaderText(node, content)
            Text(
                text = text,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        MarkdownElementTypes.CODE_FENCE -> {
            val codeText = extractCodeFenceContent(node, content)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = codeText,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
            }
        }
        MarkdownElementTypes.CODE_BLOCK -> {
            val codeText = node.getTextInNode(content).toString()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = codeText,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
            }
        }
        MarkdownElementTypes.BLOCK_QUOTE -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(IntrinsicSize.Min)
                        .background(AutoDevColors.Blue.c400)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    node.children.forEach { child ->
                        RenderNode(node = child, content = content)
                    }
                }
            }
        }
        MarkdownElementTypes.UNORDERED_LIST -> {
            Column(modifier = Modifier.padding(start = 12.dp)) {
                node.children.forEach { child ->
                    if (child.type == MarkdownElementTypes.LIST_ITEM) {
                        Row {
                            Text("• ", style = JewelTheme.defaultTextStyle)
                            Column(modifier = Modifier.weight(1f)) {
                                child.children.forEach { itemChild ->
                                    RenderNode(node = itemChild, content = content)
                                }
                            }
                        }
                    }
                }
            }
        }
        MarkdownElementTypes.ORDERED_LIST -> {
            Column(modifier = Modifier.padding(start = 12.dp)) {
                var index = 1
                node.children.forEach { child ->
                    if (child.type == MarkdownElementTypes.LIST_ITEM) {
                        Row {
                            Text("${index++}. ", style = JewelTheme.defaultTextStyle)
                            Column(modifier = Modifier.weight(1f)) {
                                child.children.forEach { itemChild ->
                                    RenderNode(node = itemChild, content = content)
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> {
            // For other node types, try to render children or show raw text
            if (node.children.isNotEmpty()) {
                node.children.forEach { child ->
                    RenderNode(node = child, content = content)
                }
            }
        }
    }
}

/**
 * Extract header text, removing the # prefix
 */
private fun extractHeaderText(node: ASTNode, content: String): String {
    val fullText = node.getTextInNode(content).toString()
    return fullText.trimStart('#').trim()
}

/**
 * Extract code fence content, removing the ``` markers and language identifier
 */
private fun extractCodeFenceContent(node: ASTNode, content: String): String {
    val lines = node.getTextInNode(content).toString().lines()
    if (lines.size <= 2) return ""
    // Remove first line (``` + language) and last line (```)
    return lines.drop(1).dropLast(1).joinToString("\n")
}

