package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.terminal.PlatformTerminalDisplay
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
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), // Reduce padding
        verticalArrangement = Arrangement.spacedBy(6.dp) // Reduce spacing
    ) {
        // Display timeline items in chronological order
        items(renderer.timeline) { timelineItem ->
            when (timelineItem) {
                is ComposeRenderer.TimelineItem.MessageItem -> {
                    MessageItem(message = timelineItem.message)
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

                is ComposeRenderer.TimelineItem.ToolErrorItem -> {
                    ToolErrorItem(error = timelineItem.error, onDismiss = { renderer.clearError() })
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
fun MessageItem(message: cc.unitmesh.devins.llm.Message) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            PlatformMessageTextContainer(text = message.content) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
expect fun PlatformMessageTextContainer(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)

@Composable
fun StreamingMessageItem(content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
fun TaskCompletedItem(
    success: Boolean,
    message: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (success) {
                Text(
                    text = "completed",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                    fontSize = 10.sp
                )
            } else {
                Text(
                    text = "⚠️",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Text(
                text = message,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Combined tool call and result display - shows both in a single compact row
 * Similar to TerminalOutputItem but for general tools (ReadFile, WriteFile, Glob, etc.)
 */
@Composable
fun CombinedToolItem(
    toolName: String,
    details: String?,
    fullParams: String? = null,
    filePath: String? = null,
    toolType: cc.unitmesh.agent.tool.ToolType? = null,
    success: Boolean? = null, // null means still executing
    summary: String? = null,
    output: String? = null,
    fullOutput: String? = null,
    executionTimeMs: Long? = null,
    onOpenFileViewer: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(success == false) } // Auto-expand on error
    var showFullParams by remember { mutableStateOf(false) }
    var showFullOutput by remember { mutableStateOf(success == false) }
    val clipboardManager = LocalClipboardManager.current

    // Determine which params/output to display
    val displayParams = if (showFullParams) fullParams else details
    val hasFullParams = fullParams != null && fullParams != details
    val displayOutput = if (showFullOutput) fullOutput else output
    val hasFullOutput = fullOutput != null && fullOutput != output

    // Check if this is a file operation that can be viewed
    val isFileOperation =
        toolType in
            listOf(
                cc.unitmesh.agent.tool.ToolType.ReadFile,
                cc.unitmesh.agent.tool.ToolType.WriteFile
            )

    val isExecuting = success == null

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header row - shows tool name, description, and result in one line
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { if (displayParams != null || displayOutput != null) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status indicator
                Text(
                    text = when {
                        isExecuting -> "●"
                        success == true -> "✓"
                        else -> "✗"
                    },
                    color = when {
                        isExecuting -> MaterialTheme.colorScheme.primary
                        success == true -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.Bold
                )

                // Tool name
                Text(
                    text = toolName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (summary != null) {
                    Text(
                        text = "→ $summary",
                        color = when {
                            success == true -> Color(0xFF4CAF50)
                            success == false -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Execution time (if available)
                if (executionTimeMs != null && executionTimeMs > 0) {
                    Text(
                        text = "${executionTimeMs}ms",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Add "View File" button for file operations
                if (isFileOperation && !filePath.isNullOrEmpty() && onOpenFileViewer != null) {
                    IconButton(
                        onClick = { onOpenFileViewer(filePath) },
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

                // Expand/collapse icon (if there's content to show)
                if (displayParams != null || displayOutput != null) {
                    Icon(
                        imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable content
            if (expanded) {
                // Show parameters if available
                if (displayParams != null) {
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
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )

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

                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(displayParams)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy parameters",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (showFullParams) (displayParams) else formatToolParameters(displayParams),
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Show output if available
                if (displayOutput != null) {
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
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )

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

                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(displayOutput)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy output",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
    val isFileOperation =
        toolType in
            listOf(
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
                )
                Text(
                    text = toolName,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )

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
fun TerminalOutputItem(
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
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isSuccess) "✓ Exit 0" else "✗ Exit $exitCode",
                    color =
                        if (isSuccess) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
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

            if (expanded && output.isNotEmpty()) {
                PlatformTerminalDisplay(
                    output = output,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
