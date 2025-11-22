package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.platform.FileChooser
import com.mikepenz.markdown.annotator.AnnotatorSettings
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.compose.LocalMarkdownColors
import com.mikepenz.markdown.compose.LocalMarkdownComponents
import com.mikepenz.markdown.compose.LocalMarkdownDimens
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.compose.elements.material.MarkdownBasicText
import cc.unitmesh.devins.workspace.WorkspaceManager
import org.intellij.markdown.MarkdownElementTypes.IMAGE
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.flavours.gfm.GFMElementTypes.HEADER
import org.intellij.markdown.flavours.gfm.GFMElementTypes.ROW
import org.intellij.markdown.flavours.gfm.GFMTokenTypes.CELL
import org.intellij.markdown.flavours.gfm.GFMTokenTypes.TABLE_SEPARATOR

/**
 * Custom table component with horizontal scrolling support for overflow content
 * Based on the official mikepenz/markdown-compose implementation
 */
@Composable
fun MarkdownTable(
    content: String,
    node: ASTNode,
    style: TextStyle,
    annotatorSettings: AnnotatorSettings = annotatorSettings(),
    onOpenFile: (String) -> Unit = {},
    headerBlock: @Composable (String, ASTNode, Dp, TextStyle) -> Unit = { content, header, tableWidth, style ->
        MarkdownTableHeader(
            content = content,
            header = header,
            tableWidth = tableWidth,
            style = style,
            annotatorSettings = annotatorSettings,
        )
    },
    rowBlock: @Composable (String, ASTNode, Dp, TextStyle) -> Unit = { content, row, tableWidth, style ->
        MarkdownTableRow(
            content = content,
            row = row,
            tableWidth = tableWidth,
            style = style,
            annotatorSettings = annotatorSettings,
        )
    },
) {
    val tableMaxWidth = LocalMarkdownDimens.current.tableMaxWidth
    val tableCellWidth = LocalMarkdownDimens.current.tableCellWidth
    val tableCornerSize = LocalMarkdownDimens.current.tableCornerSize

    val columnsCount = remember(node) {
        node.findChildOfType(HEADER)?.children?.count { it.type == CELL } ?: 0
    }
    val tableWidth = columnsCount * tableCellWidth

    // Pre-compute adaptive column weights based on max plain text length per column
    val columnWeights = remember(node, content) {
        if (columnsCount == 0) emptyList() else {
            val lengths = IntArray(columnsCount) { 0 }
            // Iterate header + rows
            node.children.filter { it.type == HEADER || it.type == ROW }.forEach { rowNode ->
                val cells = rowNode.children.filter { it.type == CELL }
                cells.forEachIndexed { idx, cell ->
                    if (idx < columnsCount) {
                        val raw = content.substring(cell.startOffset, cell.endOffset)
                            .replace("|", "")
                            .replace("`", "")
                            .replace("**", "")
                            .trim()
                        if (raw.length > lengths[idx]) lengths[idx] = raw.length
                    }
                }
            }
            val floatLengths = lengths.map { it.coerceAtLeast(1).toFloat() }
            val total = floatLengths.sum()
            // Apply min/max constraints for usability
            val constrained = floatLengths.map { (it / total).coerceIn(0.15f, 0.65f) }
            val constrainedTotal = constrained.sum()
            constrained.map { it / constrainedTotal } // normalize again
        }
    }

    val backgroundCodeColor = LocalMarkdownColors.current.tableBackground
    BoxWithConstraints(
        modifier = Modifier
            .background(backgroundCodeColor, RoundedCornerShape(tableCornerSize))
            .widthIn(max = tableMaxWidth)
    ) {
        val scrollable = maxWidth <= tableWidth
        CompositionLocalProvider(
            LocalMarkdownColumnWeights provides columnWeights,
            LocalOnOpenFile provides onOpenFile,
        ) {
            Column(
                modifier = if (scrollable) {
                    Modifier.horizontalScroll(rememberScrollState()).requiredWidth(tableWidth)
                } else Modifier.fillMaxWidth()
            ) {
                node.children.forEach {
                    when (it.type) {
                        HEADER -> headerBlock(content, it, tableWidth, style)
                        ROW -> rowBlock(content, it, tableWidth, style)
                        TABLE_SEPARATOR -> {
                            // divider optional
                        }
                    }
                }
            }
        }
    }
}

// Composition locals for adaptive weights and file open action
val LocalMarkdownColumnWeights = compositionLocalOf<List<Float>> { emptyList() }
val LocalOnOpenFile = compositionLocalOf<(String) -> Unit> { {} }

@Composable
fun MarkdownTableHeader(
    content: String,
    header: ASTNode,
    tableWidth: Dp,
    style: TextStyle,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Visible,
    annotatorSettings: AnnotatorSettings = annotatorSettings(),
) {
    val markdownComponents = LocalMarkdownComponents.current
    val tableCellPadding = LocalMarkdownDimens.current.tableCellPadding
    val weights = LocalMarkdownColumnWeights.current
    Row(
        verticalAlignment = verticalAlignment,
        modifier = Modifier.widthIn(tableWidth).height(IntrinsicSize.Max)
    ) {
        header.children.filter { it.type == CELL }.forEachIndexed { idx, cell ->
            val weight =
                if (weights.size == header.children.count { it.type == CELL }) weights[idx] else 1f / (header.children.count { it.type == CELL }).coerceAtLeast(
                    1
                )
            Column(
                modifier = Modifier.padding(tableCellPadding).weight(weight),
            ) {
                if (cell.children.any { it.type == IMAGE }) {
                    MarkdownElement(
                        node = cell,
                        components = markdownComponents,
                        content = content,
                        includeSpacer = false
                    )
                } else {
                    MarkdownTableBasicText(
                        content = content,
                        cell = cell,
                        style = style.copy(fontWeight = FontWeight.Bold),
                        maxLines = maxLines,
                        overflow = overflow,
                        annotatorSettings = annotatorSettings
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownTableRow(
    content: String,
    row: ASTNode,
    tableWidth: Dp,
    style: TextStyle,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Visible,
    annotatorSettings: AnnotatorSettings = annotatorSettings(),
) {
    val markdownComponents = LocalMarkdownComponents.current
    val tableCellPadding = LocalMarkdownDimens.current.tableCellPadding
    val weights = LocalMarkdownColumnWeights.current
    val onOpenFile = LocalOnOpenFile.current
    Row(
        verticalAlignment = verticalAlignment,
        modifier = Modifier.widthIn(tableWidth)
    ) {
        val cellCount = row.children.count { it.type == CELL }
        row.children.filter { it.type == CELL }.forEachIndexed { idx, cell ->
            val weight = if (weights.size == cellCount) weights[idx] else 1f / cellCount.coerceAtLeast(1)
            Column(
                modifier = Modifier.padding(tableCellPadding).weight(weight),
            ) {
                if (cell.children.any { it.type == IMAGE }) {
                    MarkdownElement(
                        node = cell,
                        components = markdownComponents,
                        content = content,
                        includeSpacer = false
                    )
                } else {
                    val raw = content.substring(cell.startOffset, cell.endOffset)
                    val filePath = extractBacktickedPath(raw)
                    if (filePath != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                modifier = Modifier.size(16.dp),
                                onClick = {
                                    val root = WorkspaceManager.getCurrentOrEmpty().rootPath
                                    val abs = if (root != null) root + "/" + filePath else filePath
                                    onOpenFile(abs)
                                }) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = AutoDevComposeIcons.Visibility, contentDescription = "View File"
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            MarkdownTableBasicText(
                                content = content,
                                cell = cell,
                                style = style,
                                maxLines = maxLines,
                                overflow = overflow,
                                annotatorSettings = annotatorSettings
                            )
                        }
                    } else {
                        MarkdownTableBasicText(
                            content = content,
                            cell = cell,
                            style = style,
                            maxLines = maxLines,
                            overflow = overflow,
                            annotatorSettings = annotatorSettings
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownTableBasicText(
    content: String,
    cell: ASTNode,
    style: TextStyle,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Visible,
    annotatorSettings: AnnotatorSettings = annotatorSettings(),
) {
    MarkdownBasicText(
        text = content.buildMarkdownAnnotatedString(
            textNode = cell,
            style = style,
            annotatorSettings = annotatorSettings,
        ),
        style = style,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * Extract backticked file path from markdown cell content
 * Supports paths like:
 * - `mpp-core/.../CodeReviewArtifact.kt`
 * - `src/main/kotlin/MyFile.kt`
 * - `path/to/file-name_v2.kt`
 */
private fun extractBacktickedPath(raw: String): String? {
    // Match backticked content that looks like a file path
    // Supports: alphanumeric, dots, slashes, hyphens, underscores, ellipsis (...), brackets, parentheses
    val pathRegex = Regex("`([\\w\\-_./@()\\[\\]]+(?:\\.{3})?[\\w\\-_./@()\\[\\]]*)`")
    val match = pathRegex.find(raw)
    val candidate = match?.groupValues?.getOrNull(1)

    // Filter: must contain at least one slash or a file extension
    return if (candidate != null && (candidate.contains('/') || candidate.contains('.'))) {
        // Expand ellipsis if present (e.g., mpp-core/.../File.kt -> mpp-core/src/.../File.kt)
        candidate
    } else {
        null
    }
}
