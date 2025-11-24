package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.agent.*
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * Suggested Fixes Agent View - Displays fix generation timeline
 * 
 * This component shows the streaming agent output when CodingAgent generates fixes.
 * Note: Does NOT use AgentMessageList to avoid nested LazyColumn issues.
 *
 * @param fixRenderer The ComposeRenderer instance for fix generation (null if not started)
 * @param isGenerating Whether fixes are currently being generated
 * @param modifier Compose Modifier
 * @param onOpenFileViewer Callback to open file viewer for a specific file
 */
@Composable
fun SuggestedFixesAgentView(
    fixRenderer: ComposeRenderer?,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
    onOpenFileViewer: ((String) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGenerating) {
                AutoDevColors.Indigo.c600.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Icon(
                        imageVector = AutoDevComposeIcons.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = "Fix Generation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Status badge
                    when {
                        isGenerating -> {
                            Surface(
                                color = AutoDevColors.Indigo.c600,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        strokeWidth = 1.5.dp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "GENERATING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        fixRenderer != null && fixRenderer.timeline.isNotEmpty() -> {
                            Surface(
                                color = AutoDevColors.Green.c600.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "COMPLETED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AutoDevColors.Green.c600,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Content - Agent timeline (render items directly to avoid nested LazyColumn)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 0.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (fixRenderer != null) {
                        // Render timeline items directly (no LazyColumn to avoid nesting)
                        fixRenderer.timeline.forEach { timelineItem ->
                            when (timelineItem) {
                                is ComposeRenderer.TimelineItem.MessageItem -> {
                                    MessageItem(
                                        message = timelineItem.message,
                                        tokenInfo = timelineItem.tokenInfo
                                    )
                                }
                                is ComposeRenderer.TimelineItem.CombinedToolItem -> {
                                    CombinedToolItem(
                                        toolName = timelineItem.toolName,
                                        details = timelineItem.details,
                                        fullParams = timelineItem.fullParams,
                                        filePath = timelineItem.filePath,
                                        toolType = timelineItem.toolType,
                                        success = timelineItem.success,
                                        summary = timelineItem.summary,
                                        output = timelineItem.output,
                                        fullOutput = timelineItem.fullOutput,
                                        executionTimeMs = timelineItem.executionTimeMs,
                                        onOpenFileViewer = onOpenFileViewer
                                    )
                                }
                                is ComposeRenderer.TimelineItem.ToolResultItem -> {
                                    ToolResultItem(
                                        toolName = timelineItem.toolName,
                                        success = timelineItem.success,
                                        summary = timelineItem.summary,
                                        output = timelineItem.output,
                                        fullOutput = timelineItem.fullOutput
                                    )
                                }
                                is ComposeRenderer.TimelineItem.ToolErrorItem -> {
                                    ToolErrorItem(
                                        error = timelineItem.error,
                                        onDismiss = { fixRenderer.clearError() }
                                    )
                                }
                                is ComposeRenderer.TimelineItem.TaskCompleteItem -> {
                                    TaskCompletedItem(
                                        success = timelineItem.success,
                                        message = timelineItem.message
                                    )
                                }
                                is ComposeRenderer.TimelineItem.TerminalOutputItem -> {
                                    TerminalOutputItem(
                                        command = timelineItem.command,
                                        output = timelineItem.output,
                                        exitCode = timelineItem.exitCode,
                                        executionTimeMs = timelineItem.executionTimeMs
                                    )
                                }
                                is ComposeRenderer.TimelineItem.LiveTerminalItem -> {
                                    LiveTerminalItem(
                                        sessionId = timelineItem.sessionId,
                                        command = timelineItem.command,
                                        workingDirectory = timelineItem.workingDirectory,
                                        ptyHandle = timelineItem.ptyHandle
                                    )
                                }
                            }
                        }

                        // Render streaming content if present
                        if (fixRenderer.currentStreamingOutput.isNotEmpty()) {
                            StreamingMessageItem(content = fixRenderer.currentStreamingOutput)
                        }

                        // Render current tool call if present
                        fixRenderer.currentToolCall?.let { toolCall ->
                            CurrentToolCallItem(toolCall = toolCall)
                        }
                    } else if (isGenerating) {
                        // Show loading indicator while waiting for renderer to initialize
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        // No fix generation has started yet
                        Text(
                            text = "No fixes generated yet. Click 'Generate Fixes' to start.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
