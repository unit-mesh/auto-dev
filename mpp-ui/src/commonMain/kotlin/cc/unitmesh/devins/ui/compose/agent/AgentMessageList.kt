package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.terminal.PlatformTerminalDisplay
import cc.unitmesh.devins.llm.MessageRole
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun AgentMessageList(
    renderer: ComposeRenderer,
    modifier: Modifier = Modifier,
    onOpenFileViewer: ((String) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(renderer.timeline.size, renderer.currentStreamingOutput) {
        if (renderer.timeline.isNotEmpty() || renderer.currentStreamingOutput.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = maxOf(0, listState.layoutInfo.totalItemsCount - 1)
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        // Match CLI background
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), // Reduce padding
        verticalArrangement = Arrangement.spacedBy(6.dp) // Reduce spacing
    ) {
        // Display timeline items in chronological order
        items(renderer.timeline) { timelineItem ->
            when (timelineItem) {
                is ComposeRenderer.TimelineItem.MessageItem -> {
                    MessageItem(message = timelineItem.message)
                }

                is ComposeRenderer.TimelineItem.ToolCallItem -> {
                    ToolCallItem(
                        toolName = timelineItem.toolName,
                        description = timelineItem.description,
                        details = timelineItem.details,
                        fullParams = timelineItem.fullParams,
                        filePath = timelineItem.filePath,
                        toolType = timelineItem.toolType,
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

                is ComposeRenderer.TimelineItem.ErrorItem -> {
                    ErrorItem(error = timelineItem.error, onDismiss = { renderer.clearError() })
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

        if (renderer.currentStreamingOutput.isNotEmpty()) {
            item {
                StreamingMessageItem(content = renderer.currentStreamingOutput)
            }
        }

        renderer.currentToolCall?.let { toolCall ->
            item {
                CurrentToolCallItem(toolCall = toolCall)
            }
        }
    }
}

/**
 * Platform-specific live terminal display.
 * On JVM with PTY support: Renders an interactive terminal widget
 * On other platforms: Shows a message that live terminal is not available
 */
@Composable
expect fun LiveTerminalItem(
    sessionId: String,
    command: String,
    workingDirectory: String?,
    ptyHandle: Any?
)

@Composable
private fun MessageItem(message: cc.unitmesh.devins.llm.Message) {
    val isUser = message.role == MessageRole.USER
    val clipboardManager = LocalClipboardManager.current
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isUser) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                ),
            shape = RoundedCornerShape(4.dp), // Smaller radius for CLI-like appearance
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Remove shadow
        ) {
            Column(modifier = Modifier.padding(8.dp)) { // Reduce padding
                // Message content
                Text(
                    text = message.content,
                    color =
                        if (isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    style = MaterialTheme.typography.bodyMedium
                )

                // Copy button and timestamp row (show when expanded or for AI messages)
                if (expanded || !isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Timestamp for AI messages
                        if (!isUser) {
                            Text(
                                text = formatTimestamp(Clock.System.now().toEpochMilliseconds()),
                                color =
                                    if (isUser) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    },
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Copy button (always show for easy access)
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(message.content)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy message",
                                tint =
                                    if (isUser) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingMessageItem(content: String) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💭",
                    style = MaterialTheme.typography.bodyMedium
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }

            if (content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ToolResultItem(
    toolName: String,
    success: Boolean,
    summary: String,
    output: String?,
    fullOutput: String? = null
) {
    // 失败的工具调用默认展开，并显示完整输出
    var expanded by remember { mutableStateOf(!success) }
    var showFullOutput by remember { mutableStateOf(!success) }
    val clipboardManager = LocalClipboardManager.current

    // Determine which output to display
    val displayOutput = if (showFullOutput) fullOutput else output
    val hasFullOutput = fullOutput != null && fullOutput != output

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header row - always visible
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { if (displayOutput != null) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (success) "✓" else "✗",
                    color =
                        if (success) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = toolName,
                    fontWeight = FontWeight.Medium,
                    color =
                        if (success) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "→ $summary",
                    color =
                        if (success) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                // Show expand icon only if there's output to show
                if (displayOutput != null) {
                    Icon(
                        imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint =
                            if (success) {
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable output
            if (expanded && displayOutput != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Output:",
                            fontWeight = FontWeight.Medium,
                            color =
                                if (success) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Show toggle button if there's a full output different from truncated output
                        if (hasFullOutput) {
                            TextButton(
                                onClick = { showFullOutput = !showFullOutput },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (showFullOutput) "Show Less" else "Show Full Output",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row {
                        // Copy output button
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(displayOutput ?: "")) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy output",
                                tint =
                                    if (success) {
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                    },
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Copy entire block button (always copy full output if available)
                        IconButton(
                            onClick = {
                                val blockText =
                                    buildString {
                                        val status = if (success) "SUCCESS" else "FAILED"
                                        appendLine("[Tool Result]: $toolName - $status")
                                        appendLine("Summary: $summary")
                                        appendLine("Output: ${fullOutput ?: output ?: ""}")
                                    }
                                clipboardManager.setText(AnnotatedString(blockText))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy entire block",
                                tint =
                                    if (success) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatOutput(displayOutput),
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorItem(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❌ Error",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }

            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun TaskCompletedItem(
    success: Boolean,
    message: String
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (success) "✅" else "⚠️",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CurrentToolCallItem(toolCall: ComposeRenderer.ToolCallInfo) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Animated progress indicator
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔧 ${toolCall.toolName}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = toolCall.description,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // "Executing" badge
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "EXECUTING",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            toolCall.details?.let { details ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Parameters: ${formatToolParameters(details)}",
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ToolCallItem(
    toolName: String,
    description: String,
    details: String?,
    fullParams: String? = null,
    filePath: String? = null,
    toolType: cc.unitmesh.agent.tool.ToolType? = null,
    onOpenFileViewer: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showFullParams by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    // Determine which params to display
    val displayParams = if (showFullParams) fullParams else details
    val hasFullParams = fullParams != null && fullParams != details

    // Check if this is a file operation that can be viewed
    val isFileOperation = toolType in listOf(
        cc.unitmesh.agent.tool.ToolType.ReadFile,
        cc.unitmesh.agent.tool.ToolType.WriteFile
    )

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { if (displayParams != null) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "●",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = toolName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "- $description",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Add "View File" button for file operations
                if (isFileOperation && !filePath.isNullOrEmpty() && onOpenFileViewer != null) {
                    IconButton(
                        onClick = {
                            onOpenFileViewer(filePath)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Visibility,
                            contentDescription = "View File",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (expanded && displayParams != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Parameters:",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Show toggle button if there are full params different from formatted details
                        if (hasFullParams) {
                            TextButton(
                                onClick = { showFullParams = !showFullParams },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (showFullParams) "Show Formatted" else "Show Raw Params",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row {
                        // Copy parameters button
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(displayParams ?: "")) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy parameters",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Copy entire block button (always copy full params if available)
                        IconButton(
                            onClick = {
                                val blockText =
                                    buildString {
                                        appendLine("[Tool Call]: $toolName")
                                        appendLine("Description: $description")
                                        appendLine("Parameters: ${fullParams ?: details ?: ""}")
                                    }
                                clipboardManager.setText(AnnotatedString(blockText))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy entire block",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (showFullParams) (displayParams ?: "") else formatToolParameters(displayParams ?: ""),
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

fun formatToolParameters(params: String): String {
    return try {
        val lines = mutableListOf<String>()
        val regex = Regex("""(\w+)=["']?([^"']*?)["']?(?:\s|$)""")
        regex.findAll(params).forEach { match ->
            val key = match.groups[1]?.value ?: ""
            val value = match.groups[2]?.value ?: ""
            lines.add("$key: $value")
        }

        if (lines.isNotEmpty()) {
            lines.joinToString("\n")
        } else {
            params // Fallback to original if parsing fails
        }
    } catch (e: Exception) {
        params // Fallback to original on any error
    }
}

fun formatOutput(output: String): String {
    return when {
        // If it looks like JSON, try to format it
        output.trim().startsWith("{") || output.trim().startsWith("[") -> {
            try {
                // Simple JSON formatting - add line breaks after commas and braces
                output.replace(",", ",\n")
                    .replace("{", "{\n  ")
                    .replace("}", "\n}")
                    .replace("[", "[\n  ")
                    .replace("]", "\n]")
            } catch (e: Exception) {
                output
            }
        }
        // If it's file content with line numbers, preserve formatting
        output.contains("│") -> output
        // If it's multi-line, preserve formatting
        output.contains("\n") -> output
        // For single line, limit length and add ellipsis if too long
        output.length > 100 -> "${output.take(100)}..."
        else -> output
    }
}

fun formatTimestamp(timestamp: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}

@Composable
private fun TerminalOutputItem(
    command: String,
    output: String,
    exitCode: Int,
    executionTimeMs: Long
) {
    var expanded by remember { mutableStateOf(exitCode != 0) } // Auto-expand on error
    val clipboardManager = LocalClipboardManager.current
    val isSuccess = exitCode == 0

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header row
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💻",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Shell",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (isSuccess) "✓ Exit 0" else "✗ Exit $exitCode",
                    color =
                        if (isSuccess) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${executionTimeMs}ms",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall
                )
                Icon(
                    imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Command display
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$ $command",
                modifier = Modifier.padding(start = 28.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )

            // Expandable output
            if (expanded && output.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "Output:",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row {
                        // Copy output button
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(output)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy output",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Copy entire block button
                        IconButton(
                            onClick = {
                                val blockText =
                                    buildString {
                                        appendLine("[Shell Command]")
                                        appendLine("Command: $command")
                                        appendLine("Exit Code: $exitCode")
                                        appendLine("Execution Time: ${executionTimeMs}ms")
                                        appendLine("Output:")
                                        appendLine(output)
                                    }
                                clipboardManager.setText(AnnotatedString(blockText))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy entire block",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PlatformTerminalDisplay(
                        output = output,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
