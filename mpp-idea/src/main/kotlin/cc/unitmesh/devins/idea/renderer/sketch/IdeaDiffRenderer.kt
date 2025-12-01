package cc.unitmesh.devins.idea.renderer.sketch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.diff.DiffLineType
import cc.unitmesh.agent.diff.DiffParser
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Diff renderer for IntelliJ IDEA with Jewel styling.
 * Renders unified diff format with syntax highlighting.
 */
@Composable
fun IdeaDiffRenderer(
    diffContent: String,
    modifier: Modifier = Modifier
) {
    val fileDiffs = remember(diffContent) { DiffParser.parse(diffContent) }

    Column(modifier = modifier) {
        if (fileDiffs.isEmpty()) {
            Text(
                text = "Unable to parse diff content",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = AutoDevColors.Red.c400
                ),
                modifier = Modifier.padding(8.dp)
            )
            return@Column
        }

        fileDiffs.forEach { fileDiff ->
            // File header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(JewelTheme.globalColors.panelBackground)
                    .padding(8.dp)
            ) {
                val displayPath = fileDiff.newPath?.takeIf { it.isNotBlank() }
                    ?: fileDiff.oldPath
                    ?: "unknown"
                Text(
                    text = displayPath,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AutoDevColors.Blue.c400
                    )
                )
            }

            // Diff hunks
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f))
            ) {
                fileDiff.hunks.forEach { hunk ->
                    // Hunk header
                    Text(
                        text = "@@ -${hunk.oldStartLine},${hunk.oldLineCount} +${hunk.newStartLine},${hunk.newLineCount} @@",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = AutoDevColors.Blue.c300
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    // Diff lines
                    hunk.lines.forEach { diffLine ->
                        val (bgColor, textColor) = when (diffLine.type) {
                            DiffLineType.ADDED -> Pair(
                                AutoDevColors.Diff.Dark.addedBg,
                                AutoDevColors.Green.c400
                            )
                            DiffLineType.DELETED -> Pair(
                                AutoDevColors.Diff.Dark.deletedBg,
                                AutoDevColors.Red.c400
                            )
                            else -> Pair(
                                Color.Transparent,
                                JewelTheme.globalColors.text.normal
                            )
                        }

                        Text(
                            text = diffLine.content,
                            style = JewelTheme.defaultTextStyle.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = textColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .padding(horizontal = 8.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

