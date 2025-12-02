package cc.unitmesh.devins.idea.renderer.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import cc.unitmesh.markdown.MarkdownTextParser
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/** Default cell width for table columns */
private val TABLE_CELL_WIDTH = 120.dp

/**
 * GFM Table renderer following the intellij-markdown AST structure.
 * Table structure:
 * - TABLE (GFMElementTypes.TABLE)
 *   - HEADER (GFMElementTypes.HEADER) - first row with column headers
 *     - CELL (GFMTokenTypes.CELL) - individual header cells
 *   - TABLE_SEPARATOR (GFMTokenTypes.TABLE_SEPARATOR) - the |---|---| row
 *   - ROW (GFMElementTypes.ROW) - data rows
 *     - CELL (GFMTokenTypes.CELL) - individual data cells
 *
 * Uses BoxWithConstraints to determine if horizontal scrolling is needed.
 */
@Composable
fun MarkdownTable(node: ASTNode, content: String) {
    val headerRow = node.children.find { it.type == GFMElementTypes.HEADER }
    val bodyRows = node.children.filter { it.type == GFMElementTypes.ROW }

    // Calculate column count from header
    val columnsCount = headerRow?.children?.count { it.type == GFMTokenTypes.CELL } ?: 0
    if (columnsCount == 0) return

    // Calculate table width based on column count
    val tableWidth = columnsCount * TABLE_CELL_WIDTH

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
                        val raw = MarkdownTextParser.extractCellText(cell, content)
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

    BoxWithConstraints(
        modifier = Modifier.Companion
            .padding(vertical = 4.dp)
            .background(
                JewelTheme.Companion.globalColors.panelBackground.copy(alpha = 0.3f),
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                JewelTheme.Companion.globalColors.borders.normal,
                androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
            )
    ) {
        // Determine if scrolling is needed
        val scrollable = maxWidth < tableWidth

        Column(
            modifier = if (scrollable) {
                Modifier.Companion.horizontalScroll(rememberScrollState()).requiredWidth(tableWidth)
            } else {
                Modifier.Companion.fillMaxWidth()
            }
        ) {
            // Header row
            if (headerRow != null) {
                MarkdownTableRow(
                    node = headerRow,
                    content = content,
                    isHeader = true,
                    columnWeights = columnWeights,
                    tableWidth = tableWidth
                )
                Box(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(JewelTheme.Companion.globalColors.borders.normal)
                )
            }

            // Body rows (skip TABLE_SEPARATOR which is handled implicitly)
            bodyRows.forEachIndexed { index, row ->
                MarkdownTableRow(
                    node = row,
                    content = content,
                    isHeader = false,
                    columnWeights = columnWeights,
                    tableWidth = tableWidth
                )
                if (index < bodyRows.size - 1) {
                    Box(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(JewelTheme.Companion.globalColors.borders.normal.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownTableRow(
    node: ASTNode,
    content: String,
    isHeader: Boolean,
    columnWeights: List<Float>,
    tableWidth: Dp
) {
    val cells = node.children.filter { it.type == GFMTokenTypes.CELL }

    if (cells.isEmpty()) return

    Row(
        modifier = Modifier.Companion
            .widthIn(min = tableWidth)
            .height(IntrinsicSize.Max)
            .then(
                if (isHeader) {
                    Modifier.Companion.background(JewelTheme.Companion.globalColors.panelBackground.copy(alpha = 0.5f))
                } else {
                    Modifier.Companion
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        cells.forEachIndexed { idx, cell ->
            val weight = if (idx < columnWeights.size) columnWeights[idx] else 1f / cells.size.coerceAtLeast(1)
            val cellText = MarkdownTextParser.extractCellText(cell, content)

            Text(
                text = cellText,
                style = JewelTheme.Companion.defaultTextStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = if (isHeader) FontWeight.Companion.SemiBold else FontWeight.Companion.Normal
                ),
                modifier = Modifier.Companion
                    .weight(weight)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}