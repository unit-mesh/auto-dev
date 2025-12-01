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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
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
fun SimpleJewelMarkdown(
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

// ============ Header Component ============

@Composable
private fun MarkdownHeader(node: ASTNode, content: String, level: Int) {
    val text = extractHeaderText(node, content)
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

// ============ Paragraph Component with Inline Formatting ============

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

/**
 * Build annotated string with inline formatting support
 */
private fun AnnotatedString.Builder.appendMarkdownChildren(
    node: ASTNode,
    content: String,
    codeBackground: Color
) {
    node.children.forEach { child ->
        when (child.type) {
            MarkdownTokenTypes.TEXT -> {
                append(child.getTextInNode(content).toString())
            }
            MarkdownTokenTypes.WHITE_SPACE -> {
                append(" ")
            }
            MarkdownTokenTypes.EOL -> {
                append(" ")
            }
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
                    val codeText = extractCodeSpanText(child, content)
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
                // For reference links, just show the text
                appendMarkdownChildren(child, content, codeBackground)
            }
            MarkdownElementTypes.IMAGE -> {
                // Show image alt text in brackets
                val altText = child.findChildOfType(MarkdownElementTypes.LINK_TEXT)
                    ?.getTextInNode(content)?.toString()?.trim('[', ']') ?: "image"
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append("[$altText]")
                }
            }
            else -> {
                // Recursively handle other children
                if (child.children.isNotEmpty()) {
                    appendMarkdownChildren(child, content, codeBackground)
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInlineLink(node: ASTNode, content: String) {
    val linkText = node.findChildOfType(MarkdownElementTypes.LINK_TEXT)
    val linkDest = node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)

    val text = linkText?.children?.filter { it.type == MarkdownTokenTypes.TEXT }
        ?.joinToString("") { it.getTextInNode(content).toString() }
        ?: node.getTextInNode(content).toString()
    val url = linkDest?.getTextInNode(content)?.toString() ?: ""

    pushStringAnnotation("URL", url)
    withStyle(SpanStyle(
        color = AutoDevColors.Blue.c400,
        textDecoration = TextDecoration.Underline
    )) {
        append(text)
    }
    pop()
}

private fun AnnotatedString.Builder.appendAutoLink(node: ASTNode, content: String) {
    val url = node.getTextInNode(content).toString().trim('<', '>')
    pushStringAnnotation("URL", url)
    withStyle(SpanStyle(
        color = AutoDevColors.Blue.c400,
        textDecoration = TextDecoration.Underline
    )) {
        append(url)
    }
    pop()
}

private fun extractCodeSpanText(node: ASTNode, content: String): String {
    return node.children
        .filter { it.type != MarkdownTokenTypes.BACKTICK }
        .joinToString("") { it.getTextInNode(content).toString() }
        .trim()
}

// ============ Code Block Components ============

@Composable
private fun MarkdownCodeFence(node: ASTNode, content: String) {
    val language = node.findChildOfType(MarkdownTokenTypes.FENCE_LANG)
        ?.getTextInNode(content)?.toString()?.trim()
    val codeText = extractCodeFenceContent(node, content)

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

// ============ Block Quote Component ============

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

// ============ List Components ============

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

// ============ Horizontal Rule Component ============

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

// ============ Table Component ============

/**
 * GFM Table renderer following the intellij-markdown AST structure.
 * Table structure:
 * - TABLE (GFMElementTypes.TABLE)
 *   - HEADER (GFMElementTypes.HEADER) - first row with column headers
 *     - CELL (GFMTokenTypes.CELL) - individual header cells
 *   - TABLE_SEPARATOR (GFMTokenTypes.TABLE_SEPARATOR) - the |---|---| row
 *   - ROW (GFMElementTypes.ROW) - data rows
 *     - CELL (GFMTokenTypes.CELL) - individual data cells
 */
@Composable
private fun MarkdownTable(node: ASTNode, content: String) {
    val headerRow = node.children.find { it.type == GFMElementTypes.HEADER }
    val bodyRows = node.children.filter { it.type == GFMElementTypes.ROW }

    // Calculate column count from header
    val columnsCount = headerRow?.children?.count { it.type == GFMTokenTypes.CELL } ?: 0
    if (columnsCount == 0) return

    // Calculate adaptive column weights based on content length
    val columnWeights = remember(node, content) {
        val lengths = IntArray(columnsCount) { 0 }
        // Iterate header + rows to find max length per column
        node.children
            .filter { it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW }
            .forEach { rowNode ->
                val cells = rowNode.children.filter { it.type == GFMTokenTypes.CELL }
                cells.forEachIndexed { idx, cell ->
                    if (idx < columnsCount) {
                        val raw = extractCellText(cell, content)
                        if (raw.length > lengths[idx]) lengths[idx] = raw.length
                    }
                }
            }
        // Convert to weights with min/max constraints
        val floatLengths = lengths.map { it.coerceAtLeast(1).toFloat() }
        val total = floatLengths.sum()
        val constrained = floatLengths.map { (it / total).coerceIn(0.15f, 0.65f) }
        val constrainedTotal = constrained.sum()
        constrained.map { it / constrainedTotal }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f),
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                JewelTheme.globalColors.borders.normal,
                RoundedCornerShape(6.dp)
            )
            .horizontalScroll(rememberScrollState())
    ) {
        // Header row
        if (headerRow != null) {
            MarkdownTableRow(
                node = headerRow,
                content = content,
                isHeader = true,
                columnWeights = columnWeights
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(JewelTheme.globalColors.borders.normal)
            )
        }

        // Body rows (skip TABLE_SEPARATOR which is handled implicitly)
        bodyRows.forEachIndexed { index, row ->
            MarkdownTableRow(
                node = row,
                content = content,
                isHeader = false,
                columnWeights = columnWeights
            )
            if (index < bodyRows.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
private fun MarkdownTableRow(
    node: ASTNode,
    content: String,
    isHeader: Boolean,
    columnWeights: List<Float>
) {
    val cells = node.children.filter { it.type == GFMTokenTypes.CELL }

    if (cells.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHeader) {
                    Modifier.background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cells.forEachIndexed { idx, cell ->
            val weight = if (idx < columnWeights.size) columnWeights[idx] else 1f / cells.size.coerceAtLeast(1)
            val cellText = extractCellText(cell, content)

            Text(
                text = cellText,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal
                ),
                modifier = Modifier
                    .weight(weight)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * Extract clean text from a table cell node.
 * Uses the raw cell text and strips markdown formatting.
 */
private fun extractCellText(cell: ASTNode, content: String): String {
    return cell.getTextInNode(content).toString()
        .replace("|", "")
        .replace("`", "")
        .replace("**", "")
        .replace("*", "")
        .trim()
}

// ============ Helper Functions ============

/**
 * Extract header text, removing the # prefix
 */
private fun extractHeaderText(node: ASTNode, content: String): String {
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
 * Extract code fence content, removing the ``` markers and language identifier
 */
private fun extractCodeFenceContent(node: ASTNode, content: String): String {
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
