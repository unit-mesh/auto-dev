package cc.unitmesh.devins.idea.components.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.render.TimelineItem
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.ide.CopyPasteManager
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import java.awt.datatransfer.StringSelection

/**
 * Tool call bubble for displaying tool execution with status.
 * Similar to ToolItem/CombinedToolItem in mpp-ui but using Jewel theming.
 *
 * Features aligned with mpp-ui:
 * - Expand/collapse for params and output
 * - Copy to clipboard functionality
 * - Status indicators with colors
 * - Execution time display
 */
@Composable
fun IdeaToolCallBubble(
    item: TimelineItem.ToolCallItem,
    modifier: Modifier = Modifier
) {
    // Auto-expand on error
    var expanded by remember { mutableStateOf(item.success == false) }
    var showFullParams by remember { mutableStateOf(false) }
    var showFullOutput by remember { mutableStateOf(item.success == false) }

    val isExecuting = item.success == null
    val hasParams = item.params.isNotEmpty()
    val hasOutput = !item.output.isNullOrEmpty()
    val hasExpandableContent = hasParams || hasOutput

    // Determine display content
    val displayParams = if (showFullParams) item.params else item.params.take(100)
    val displayOutput = if (showFullOutput) item.output else item.output?.take(200)
    val hasMoreParams = item.params.length > 100
    val hasMoreOutput = (item.output?.length ?: 0) > 200

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        Column {
            // Header row: Status + Tool name + Output (dynamic) + Time + Expand icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { if (hasExpandableContent) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status icon
                Icon(
                    imageVector = when {
                        isExecuting -> IdeaComposeIcons.PlayArrow
                        item.success == true -> IdeaComposeIcons.CheckCircle
                        else -> IdeaComposeIcons.Error
                    },
                    contentDescription = when {
                        isExecuting -> "Executing"
                        item.success == true -> "Success"
                        else -> "Failed"
                    },
                    modifier = Modifier.size(16.dp),
                    tint = when {
                        isExecuting -> AutoDevColors.Blue.c400
                        item.success == true -> AutoDevColors.Green.c400
                        else -> AutoDevColors.Red.c400
                    }
                )

                // Tool name (fixed width, no shrink)
                Text(
                    text = item.toolName,
                    style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )

                // Output summary (dynamic width, fills remaining space)
                if (hasOutput && !expanded) {
                    Text(
                        text = "-> ${item.output?.replace("\n", " ")?.trim() ?: ""}",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 12.sp,
                            color = when {
                                item.success == true -> AutoDevColors.Green.c400
                                item.success == false -> AutoDevColors.Red.c400
                                else -> JewelTheme.globalColors.text.info
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clipToBounds()
                    )
                } else if (!expanded) {
                    // Show params as fallback when no output
                    if (hasParams) {
                        Text(
                            text = "-> ${item.params.replace("\n", " ").trim()}",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 12.sp,
                                color = JewelTheme.globalColors.text.info
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clipToBounds()
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Execution time (if available)
                item.executionTimeMs?.let { time: Long ->
                    if (time > 0L) {
                        Text(
                            text = "${time}ms",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 11.sp,
                                color = JewelTheme.globalColors.text.info.copy(alpha = 0.6f)
                            )
                        )
                    }
                }

                // Expand/collapse icon
                if (hasExpandableContent) {
                    Icon(
                        imageVector = if (expanded) IdeaComposeIcons.ExpandLess else IdeaComposeIcons.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp),
                        tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
                    )
                }
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    // Parameters section
                    if (hasParams) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Parameters:",
                                    style = JewelTheme.defaultTextStyle.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                )

                                if (hasMoreParams) {
                                    Text(
                                        text = if (showFullParams) "Show Less" else "Show All",
                                        style = JewelTheme.defaultTextStyle.copy(
                                            fontSize = 11.sp,
                                            color = AutoDevColors.Blue.c400
                                        ),
                                        modifier = Modifier.clickable { showFullParams = !showFullParams }
                                    )
                                }
                            }

                            // Copy button
                            IconButton(
                                onClick = { copyToClipboard(item.params) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = IdeaComposeIcons.ContentCopy,
                                    contentDescription = "Copy parameters",
                                    modifier = Modifier.size(14.dp),
                                    tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Parameters content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = displayParams + if (!showFullParams && hasMoreParams) "..." else "",
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }

                    // Output section
                    if (hasOutput) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Output:",
                                    style = JewelTheme.defaultTextStyle.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                )

                                if (hasMoreOutput) {
                                    Text(
                                        text = if (showFullOutput) "Show Less" else "Show Full",
                                        style = JewelTheme.defaultTextStyle.copy(
                                            fontSize = 11.sp,
                                            color = AutoDevColors.Blue.c400
                                        ),
                                        modifier = Modifier.clickable { showFullOutput = !showFullOutput }
                                    )
                                }
                            }

                            // Copy button
                            IconButton(
                                onClick = { copyToClipboard(item.output ?: "") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = IdeaComposeIcons.ContentCopy,
                                    contentDescription = "Copy output",
                                    modifier = Modifier.size(14.dp),
                                    tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Output content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = formatToolOutput(displayOutput ?: "") +
                                        if (!showFullOutput && hasMoreOutput) "..." else "",
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Currently executing tool call indicator with progress animation.
 * Similar to CurrentToolCallItem in mpp-ui.
 */
@Composable
fun IdeaCurrentToolCallItem(
    toolName: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AutoDevColors.Blue.c400.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Spinning indicator (using text for simplicity in Jewel)
            Text(
                text = "...",
                style = JewelTheme.defaultTextStyle.copy(
                    color = AutoDevColors.Blue.c400,
                    fontWeight = FontWeight.Bold
                )
            )

            // Tool icon
            Icon(
                imageVector = IdeaComposeIcons.Build,
                contentDescription = "Tool",
                modifier = Modifier.size(16.dp),
                tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
            )

            // Tool name and description
            Text(
                text = "$toolName - $description",
                style = JewelTheme.defaultTextStyle,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            // Executing badge
            Box(
                modifier = Modifier
                    .background(
                        color = AutoDevColors.Blue.c400,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "EXECUTING",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AutoDevColors.Neutral.c50
                    )
                )
            }
        }
    }
}

/**
 * Copy text to system clipboard using IntelliJ CopyPasteManager
 */
private fun copyToClipboard(text: String) {
    try {
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    } catch (e: Exception) {
        // Ignore clipboard errors
    }
}

/**
 * Format tool output for better readability.
 * Returns output as-is to preserve valid JSON/table formats.
 * No fixed truncation - UI handles overflow dynamically.
 */
private fun formatToolOutput(output: String): String = output

