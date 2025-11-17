package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.DiffHunk
import cc.unitmesh.DiffLine
import cc.unitmesh.DiffLineType
import cc.unitmesh.DiffParser
import cc.unitmesh.FileDiff
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * Diff Sketch 渲染器 - 渲染 AI 生成的代码差异
 * 参考 AutoDev IDEA 版本的 DiffLangSketch 设计
 */
object DiffSketchRenderer {
    // 颜色定义 - 使用 AutoDev 设计系统
    // 注意：这里使用暗色主题颜色，因为大多数开发者偏好暗色主题
    // TODO: 根据实际主题模式动态切换颜色
    val AddedLineBackground = AutoDevColors.Diff.Dark.addedBg
    val AddedLineBorder = AutoDevColors.Diff.Dark.addedBorder
    val DeletedLineBackground = AutoDevColors.Diff.Dark.deletedBg
    val DeletedLineBorder = AutoDevColors.Diff.Dark.deletedBorder
    val LineNumberColor = AutoDevColors.Diff.Dark.lineNumber
    val ContextLineBackground = Color.Transparent

    /**
     * 渲染 Diff 内容
     */
    @Composable
    fun RenderDiff(
        diffContent: String,
        modifier: Modifier = Modifier,
        onAccept: (() -> Unit)? = null,
        onReject: (() -> Unit)? = null
    ) {
        val fileDiffs =
            remember(diffContent) {
                DiffParser.parse(diffContent)
            }

        Column(modifier = modifier) {
            if (fileDiffs.isNotEmpty() && (onAccept != null || onReject != null)) {
                DiffHeader(
                    fileCount = fileDiffs.size,
                    onAccept = onAccept,
                    onReject = onReject
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            fileDiffs.forEach { fileDiff ->
                FileDiffView(fileDiff)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (fileDiffs.isEmpty()) {
                Text(
                    text = "无法解析 diff 内容",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = diffContent,
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }

    /**
     * Diff 头部 - 包含 Accept/Reject 按钮
     */
    @Composable
    private fun DiffHeader(
        fileCount: Int,
        onAccept: (() -> Unit)?,
        onReject: (() -> Unit)?
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "修改了 $fileCount 个文件",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onAccept != null) {
                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.CheckCircle,
                                contentDescription = "接受修改",
                                tint = Color(0xFF2EA043)
                            )
                        }
                    }

                    if (onReject != null) {
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Close,
                                contentDescription = "拒绝修改",
                                tint = Color(0xFFDA3633)
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 单个文件的 Diff 视图
     */
    @Composable
    fun FileDiffView(fileDiff: FileDiff) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                FileHeader(fileDiff)

                Divider()

                if (fileDiff.isBinaryFile) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "二进制文件已更改",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (fileDiff.hunks.isEmpty()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "仅元数据变更",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    fileDiff.hunks.forEach { hunk ->
                        HunkView(hunk)
                    }
                }
            }
        }
    }

    @Composable
    fun FileHeader(fileDiff: FileDiff) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val displayPath = fileDiff.newPath ?: fileDiff.oldPath ?: "未知文件"
                Text(
                    text = displayPath,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 文件状态标签
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (fileDiff.isNewFile) {
                        Text(
                            text = "新建文件",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2EA043)
                        )
                    } else if (fileDiff.isDeletedFile) {
                        Text(
                            text = "删除文件",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFDA3633)
                        )
                    }

                    if (fileDiff.isBinaryFile) {
                        Text(
                            text = "二进制文件",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (fileDiff.oldMode != null && fileDiff.newMode != null && fileDiff.oldMode != fileDiff.newMode) {
                        Text(
                            text = "模式变更: ${fileDiff.oldMode} → ${fileDiff.newMode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // 统计信息
            val stats = calculateStats(fileDiff)
            if (stats.first > 0 || stats.second > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (stats.first > 0) {
                        Text(
                            text = "+${stats.first}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2EA043)
                        )
                    }
                    if (stats.second > 0) {
                        Text(
                            text = "-${stats.second}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFDA3633)
                        )
                    }
                }
            }
        }
    }

    private fun calculateStats(fileDiff: FileDiff): Pair<Int, Int> {
        var added = 0
        var deleted = 0

        fileDiff.hunks.forEach { hunk ->
            hunk.lines.forEach { line ->
                when (line.type) {
                    DiffLineType.ADDED -> added++
                    DiffLineType.DELETED -> deleted++
                    else -> {}
                }
            }
        }

        return Pair(added, deleted)
    }

    /**
     * Hunk 视图 - 支持折叠/展开
     */
    @Composable
    fun HunkView(hunk: DiffHunk) {
        var expanded by remember { mutableStateOf(true) }
        val defaultVisibleLines = 5
        val hasMoreLines = hunk.lines.size > defaultVisibleLines

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { if (hasMoreLines) expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hunk.header,
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                    color = LineNumberColor
                )

                if (hasMoreLines) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (expanded) "折叠" else "展开 ${hunk.lines.size} 行",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (expanded) AutoDevComposeIcons.KeyboardArrowUp else AutoDevComposeIcons.KeyboardArrowDown,
                            contentDescription = if (expanded) "折叠" else "展开",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            val visibleLines =
                if (expanded || !hasMoreLines) {
                    hunk.lines
                } else {
                    hunk.lines.take(defaultVisibleLines)
                }

            visibleLines.forEach { line ->
                DiffLineView(line)
            }

            if (!expanded && hasMoreLines) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { expanded = true }
                            .padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "... 还有 ${hunk.lines.size - defaultVisibleLines} 行 (点击展开)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }


    @Composable
    fun DiffLineView(line: DiffLine) {
        val (backgroundColor, borderColor, prefix) =
            when (line.type) {
                DiffLineType.ADDED -> Triple(AddedLineBackground, AddedLineBorder, "+")
                DiffLineType.DELETED -> Triple(DeletedLineBackground, DeletedLineBorder, "-")
                DiffLineType.CONTEXT -> Triple(ContextLineBackground, Color.Transparent, " ")
                DiffLineType.HEADER ->
                    Triple(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        Color.Transparent,
                        ""
                    )
            }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .then(
                        if (borderColor != Color.Transparent) {
                            Modifier.border(
                                width = 1.dp,
                                color = borderColor.copy(alpha = 0.2f)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            // 旧行号
            Text(
                text = line.oldLineNumber?.toString() ?: "",
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                color = LineNumberColor,
                textAlign = TextAlign.End,
                modifier = Modifier.width(40.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 新行号
            Text(
                text = line.newLineNumber?.toString() ?: "",
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                color = LineNumberColor,
                textAlign = TextAlign.End,
                modifier = Modifier.width(40.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 行前缀（+/-/ ）
            Text(
                text = prefix,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                color =
                    when (line.type) {
                        DiffLineType.ADDED -> Color(0xFF2EA043)
                        DiffLineType.DELETED -> Color(0xFFDA3633)
                        else -> LineNumberColor
                    },
                modifier = Modifier.width(12.dp)
            )

            // 行内容
            SelectionContainer {
                Text(
                    text = line.content,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
