package cc.unitmesh.devins.idea.toolwindow.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import cc.unitmesh.devins.idea.services.IdeaShellExecutor
import cc.unitmesh.devins.idea.services.IdeaTerminalExecutionState
import cc.unitmesh.devins.idea.services.IdeaUIWriter
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Terminal output bubble for displaying shell command results.
 * Enhanced with execute/re-execute, expand/collapse, and copy functionality.
 * Inspired by TerminalSketchProvider from ext-terminal module.
 */
@Composable
fun IdeaTerminalOutputBubble(
    item: JewelRenderer.TimelineItem.TerminalOutputItem,
    project: Project? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(item.exitCode != 0) }
    var showFullOutput by remember { mutableStateOf(false) }

    // Execution state for re-run functionality
    var executionState by remember { mutableStateOf(IdeaTerminalExecutionState.READY) }
    var isExecuting by remember { mutableStateOf(false) }
    var currentOutput by remember { mutableStateOf(item.output) }
    var currentExitCode by remember { mutableStateOf(item.exitCode) }
    var currentExecutionTime by remember { mutableStateOf(item.executionTimeMs) }
    var executionJob by remember { mutableStateOf<Job?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AutoDevColors.Neutral.c900)
        ) {
            Column {
                // Header with command and actions
                TerminalHeader(
                    command = item.command,
                    exitCode = currentExitCode,
                    executionTimeMs = currentExecutionTime,
                    executionState = executionState,
                    isExecuting = isExecuting,
                    expanded = expanded,
                    onExpandToggle = { expanded = !expanded },
                    onExecute = {
                        if (project != null && !isExecuting) {
                            isExecuting = true
                            executionState = IdeaTerminalExecutionState.EXECUTING
                            currentOutput = ""

                            val uiWriter = IdeaUIWriter(
                                onTextUpdate = { text, _ -> currentOutput = text },
                                onStateUpdate = { state, _ -> executionState = state }
                            )

                            val startTime = System.currentTimeMillis()
                            executionJob = coroutineScope.launch {
                                try {
                                    val executor = IdeaShellExecutor.getInstance(project)
                                    uiWriter.setExecuting(true)
                                    val exitCode = executor.exec(
                                        item.command,
                                        uiWriter,
                                        uiWriter,
                                        Dispatchers.IO
                                    )
                                    currentExitCode = exitCode
                                    currentExecutionTime = System.currentTimeMillis() - startTime
                                    if (exitCode == 0) {
                                        uiWriter.setSuccess()
                                    } else {
                                        uiWriter.setFailed("Exit code: $exitCode")
                                    }
                                } catch (e: Exception) {
                                    uiWriter.setFailed(e.message)
                                    currentExitCode = -1
                                } finally {
                                    isExecuting = false
                                    expanded = true
                                }
                            }
                        }
                    },
                    onStop = {
                        executionJob?.cancel()
                        isExecuting = false
                        executionState = IdeaTerminalExecutionState.TERMINATED
                    },
                    onCopy = {
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(StringSelection(currentOutput), null)
                    }
                )

                // Collapsible output content
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E))
                            .padding(12.dp)
                    ) {
                        // Output text
                        val displayOutput = if (showFullOutput || currentOutput.length <= 1000) {
                            currentOutput
                        } else {
                            currentOutput.take(1000) + "\n..."
                        }

                        if (displayOutput.isNotEmpty()) {
                            Text(
                                text = displayOutput,
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AutoDevColors.Neutral.c300
                                )
                            )
                        } else if (isExecuting) {
                            Text(
                                text = "Executing...",
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AutoDevColors.Neutral.c500
                                )
                            )
                        }

                        // Show more/less toggle
                        if (currentOutput.length > 1000) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (showFullOutput) "Show Less" else "Show Full Output",
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 11.sp,
                                    color = AutoDevColors.Blue.c400
                                ),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showFullOutput = !showFullOutput }
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * Header component for terminal bubble with command, status, and action buttons.
 */
@Composable
private fun TerminalHeader(
    command: String,
    exitCode: Int,
    executionTimeMs: Long,
    executionState: IdeaTerminalExecutionState,
    isExecuting: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onExecute: () -> Unit,
    onStop: () -> Unit,
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
        // Left side: Command and status
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
            val displayCommand = if (command.length > 50) command.take(50) + "..." else command
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

        // Right side: Status and actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Execution state indicator
            TerminalStatusBadge(
                executionState = executionState,
                exitCode = exitCode,
                executionTimeMs = executionTimeMs,
                isExecuting = isExecuting
            )

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Execute/Stop button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isExecuting) AutoDevColors.Red.c600
                            else AutoDevColors.Green.c600
                        )
                        .clickable {
                            if (isExecuting) onStop() else onExecute()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExecuting) IdeaComposeIcons.Stop else IdeaComposeIcons.PlayArrow,
                        contentDescription = if (isExecuting) "Stop" else "Execute",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

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
}

/**
 * Status badge showing execution state with appropriate colors.
 */
@Composable
private fun TerminalStatusBadge(
    executionState: IdeaTerminalExecutionState,
    exitCode: Int,
    executionTimeMs: Long,
    isExecuting: Boolean
) {
    val (bgColor, textColor, text) = when {
        isExecuting -> Triple(
            AutoDevColors.Blue.c600.copy(alpha = 0.3f),
            AutoDevColors.Blue.c400,
            "Running..."
        )
        executionState == IdeaTerminalExecutionState.SUCCESS || exitCode == 0 -> Triple(
            AutoDevColors.Green.c600.copy(alpha = 0.3f),
            AutoDevColors.Green.c400,
            "exit: 0  ${executionTimeMs}ms"
        )
        executionState == IdeaTerminalExecutionState.FAILED || exitCode != 0 -> Triple(
            AutoDevColors.Red.c600.copy(alpha = 0.3f),
            AutoDevColors.Red.c400,
            "exit: $exitCode  ${executionTimeMs}ms"
        )
        executionState == IdeaTerminalExecutionState.TERMINATED -> Triple(
            AutoDevColors.Amber.c600.copy(alpha = 0.3f),
            AutoDevColors.Amber.c400,
            "Terminated"
        )
        else -> Triple(
            AutoDevColors.Neutral.c700,
            AutoDevColors.Neutral.c400,
            "Ready"
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

