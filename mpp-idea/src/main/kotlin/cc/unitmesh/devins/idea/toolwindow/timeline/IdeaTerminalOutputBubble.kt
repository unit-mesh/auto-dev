package cc.unitmesh.devins.idea.toolwindow.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Terminal output bubble for displaying shell command results.
 * Shows output with scrollable area (4 lines visible), full width layout.
 */
@Composable
fun IdeaTerminalOutputBubble(
    item: JewelRenderer.TimelineItem.TerminalOutputItem,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when output changes
    LaunchedEffect(item.output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AutoDevColors.Neutral.c900)
    ) {
        Column {
            // Header with command and status
            TerminalHeader(
                command = item.command,
                exitCode = item.exitCode,
                executionTimeMs = item.executionTimeMs,
                expanded = expanded,
                onExpandToggle = { expanded = !expanded },
                onCopy = {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(StringSelection(item.output), null)
                }
            )

            // Collapsible output content with scrolling
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp) // ~4 lines at 12sp + padding
                        .background(Color(0xFF1E1E1E))
                        .padding(12.dp)
                ) {
                    if (item.output.isNotEmpty()) {
                        Text(
                            text = item.output,
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AutoDevColors.Neutral.c300,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.verticalScroll(scrollState)
                        )
                    } else {
                        Text(
                            text = "(No output)",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AutoDevColors.Neutral.c500
                            )
                        )
                    }
                }
            }
        }
    }
}


/**
 * Header component for terminal bubble with command, status, and copy button.
 */
@Composable
private fun TerminalHeader(
    command: String,
    exitCode: Int,
    executionTimeMs: Long,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AutoDevColors.Neutral.c800)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onExpandToggle() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Command
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Expand/Collapse icon
            Icon(
                imageVector = if (expanded) IdeaComposeIcons.ExpandLess else IdeaComposeIcons.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = AutoDevColors.Neutral.c400,
                modifier = Modifier.size(16.dp)
            )

            // Terminal icon
            Icon(
                imageVector = IdeaComposeIcons.Terminal,
                contentDescription = "Terminal",
                tint = AutoDevColors.Cyan.c400,
                modifier = Modifier.size(14.dp)
            )

            // Command text (truncated if too long)
            val displayCommand = if (command.length > 60) command.take(60) + "..." else command
            Text(
                text = "$ $displayCommand",
                style = JewelTheme.defaultTextStyle.copy(
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = AutoDevColors.Cyan.c400
                )
            )
        }

        // Right side: Status and copy
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status badge
            TerminalStatusBadge(exitCode = exitCode, executionTimeMs = executionTimeMs)

            // Copy button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AutoDevColors.Neutral.c700)
                    .clickable { onCopy() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.ContentCopy,
                    contentDescription = "Copy output",
                    tint = AutoDevColors.Neutral.c300,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Status badge showing exit code and execution time.
 */
@Composable
private fun TerminalStatusBadge(
    exitCode: Int,
    executionTimeMs: Long
) {
    val (bgColor, textColor, text) = when {
        exitCode == 0 -> Triple(
            AutoDevColors.Green.c600.copy(alpha = 0.3f),
            AutoDevColors.Green.c400,
            "exit: 0  ${executionTimeMs}ms"
        )
        else -> Triple(
            AutoDevColors.Red.c600.copy(alpha = 0.3f),
            AutoDevColors.Red.c400,
            "exit: $exitCode  ${executionTimeMs}ms"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 11.sp,
                color = textColor
            )
        )
    }
}

