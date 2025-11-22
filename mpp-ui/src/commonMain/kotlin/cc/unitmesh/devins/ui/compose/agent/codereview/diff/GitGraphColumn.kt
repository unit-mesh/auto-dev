package cc.unitmesh.devins.ui.compose.agent.codereview.diff

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Git Graph Column component
 * Renders a visual representation of git commit history similar to SourceTree
 * 
 * @param node The graph node for this commit (null for empty graph)
 * @param graphStructure Complete graph structure
 * @param rowHeight Height of each commit row
 * @param columnWidth Width of each graph column
 * @param modifier Modifier for the graph column
 */
@Composable
fun GitGraphColumn(
    node: GitGraphNode?,
    graphStructure: GitGraphStructure,
    rowHeight: Dp = 60.dp,
    columnWidth: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val totalWidth = (graphStructure.maxColumns * columnWidth.value).dp
    
    Box(modifier = modifier.width(totalWidth)) {
        Canvas(
            modifier = Modifier
                .width(totalWidth)
                .fillMaxHeight()
        ) {
            if (node == null) return@Canvas
            
            val rowHeightPx = rowHeight.toPx()
            val columnWidthPx = columnWidth.toPx()
            val nodeRadius = 4.dp.toPx()
            val lineStrokeWidth = 2.dp.toPx()
            
            // Draw lines that pass through or end at this row
            graphStructure.lines
                .filter { line -> 
                    line.fromRow == node.row || line.toRow == node.row ||
                    (line.fromRow < node.row && line.toRow > node.row)
                }
                .forEach { line ->
                    drawGraphLine(
                        line = line,
                        currentRow = node.row,
                        rowHeightPx = rowHeightPx,
                        columnWidthPx = columnWidthPx,
                        strokeWidth = lineStrokeWidth
                    )
                }
            
            // Draw the commit node on top of lines
            val nodeX = node.column * columnWidthPx + columnWidthPx / 2
            val nodeY = rowHeightPx / 2
            
            drawCommitNode(
                center = Offset(nodeX, nodeY),
                radius = nodeRadius,
                color = node.color,
                nodeType = node.type,
                strokeWidth = lineStrokeWidth
            )
        }
    }
}

/**
 * Draw a git graph line (branch line, merge line, or continuation)
 */
private fun DrawScope.drawGraphLine(
    line: GitGraphLine,
    currentRow: Int,
    rowHeightPx: Float,
    columnWidthPx: Float,
    strokeWidth: Float
) {
    val startX = line.fromColumn * columnWidthPx + columnWidthPx / 2
    val endX = line.toColumn * columnWidthPx + columnWidthPx / 2
    
    // Calculate Y positions based on the current row
    val startY = when {
        line.fromRow == currentRow -> rowHeightPx / 2
        line.fromRow < currentRow -> 0f
        else -> rowHeightPx
    }
    
    val endY = when {
        line.toRow == currentRow -> rowHeightPx / 2
        line.toRow > currentRow -> rowHeightPx
        else -> 0f
    }
    
    // Only draw if this line segment is visible in the current row
    if (line.fromRow > currentRow || line.toRow < currentRow) {
        return
    }
    
    if (line.isMerge && line.fromColumn != line.toColumn) {
        // Draw curved merge line
        val path = Path().apply {
            moveTo(startX, startY)
            
            // Create smooth curve for merge
            val controlPointY = (startY + endY) / 2
            cubicTo(
                startX, controlPointY,
                endX, controlPointY,
                endX, endY
            )
        }
        
        drawPath(
            path = path,
            color = line.color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
    } else if (line.fromColumn != line.toColumn) {
        // Draw angled branch line
        val path = Path().apply {
            moveTo(startX, startY)
            
            // Create angled line for branching
            val midY = (startY + endY) / 2
            lineTo(startX, midY)
            lineTo(endX, midY)
            lineTo(endX, endY)
        }
        
        drawPath(
            path = path,
            color = line.color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.cornerPathEffect(4.dp.toPx())
            )
        )
    } else {
        // Draw straight vertical line
        drawLine(
            color = line.color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Draw a commit node (circle representing a commit)
 */
private fun DrawScope.drawCommitNode(
    center: Offset,
    radius: Float,
    color: Color,
    nodeType: GitGraphNodeType,
    strokeWidth: Float
) {
    when (nodeType) {
        GitGraphNodeType.COMMIT -> {
            // Regular commit: filled circle
            drawCircle(
                color = color,
                radius = radius,
                center = center
            )
            // Outline for better visibility
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        GitGraphNodeType.MERGE -> {
            // Merge commit: double circle
            drawCircle(
                color = color,
                radius = radius * 1.3f,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = color,
                radius = radius * 0.7f,
                center = center
            )
        }
        
        GitGraphNodeType.BRANCH_START -> {
            // Branch start: filled circle with outline
            drawCircle(
                color = color,
                radius = radius * 1.2f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = radius * 0.5f,
                center = center
            )
        }
        
        GitGraphNodeType.BRANCH_END -> {
            // Branch end: square
            val size = radius * 1.6f
            drawRect(
                color = color,
                topLeft = Offset(center.x - size / 2, center.y - size / 2),
                size = androidx.compose.ui.geometry.Size(size, size)
            )
        }
    }
}

/**
 * Simple graph column for preview/testing
 * Shows a basic linear history
 */
@Composable
fun SimpleGitGraphColumn(
    color: Color = Color(0xFF5C6BC0),
    rowHeight: Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.width(16.dp)) {
        Canvas(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
        ) {
            val centerX = size.width / 2
            val rowHeightPx = rowHeight.toPx()
            val nodeRadius = 4.dp.toPx()
            
            // Draw vertical line
            drawLine(
                color = color,
                start = Offset(centerX, 0f),
                end = Offset(centerX, rowHeightPx),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Draw commit node
            drawCircle(
                color = color,
                radius = nodeRadius,
                center = Offset(centerX, rowHeightPx / 2)
            )
        }
    }
}

