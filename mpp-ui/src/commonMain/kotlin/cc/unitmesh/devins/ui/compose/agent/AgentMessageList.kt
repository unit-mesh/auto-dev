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
import autodev_intellij.mpp_ui.generated.resources.NotoSansSC_Regular
import autodev_intellij.mpp_ui.generated.resources.Res
import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.terminal.PlatformTerminalDisplay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.Font

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
        items(renderer.timeline) { timelineItem ->
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
fun MessageItem(
    message: Message,
    tokenInfo: cc.unitmesh.llm.compression.TokenInfo? = null
) {
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
                        fontFamily = if (Platform.isWasm) FontFamily(Font(Res.font.NotoSansSC_Regular)) else FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Display token info if available (only for assistant messages)
                    if (!isUser && tokenInfo != null && tokenInfo.totalTokens > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text ="${tokenInfo.inputTokens} + ${tokenInfo.outputTokens} (${tokenInfo.totalTokens} tokens)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
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
                Icon(
                    imageVector = AutoDevComposeIcons.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
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
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { if (displayParams != null || displayOutput != null) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        isExecuting -> AutoDevComposeIcons.PlayArrow
                        success -> AutoDevComposeIcons.CheckCircle
                        else -> AutoDevComposeIcons.Error
                    },
                    contentDescription = when {
                        isExecuting -> "Executing"
                        success -> "Success"
                        else -> "Failed"
                    },
                    tint = when {
                        isExecuting -> MaterialTheme.colorScheme.primary
                        success -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(16.dp)
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
                        color = when (success) {
                            true -> Color(0xFF4CAF50)
                            false -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (executionTimeMs != null && executionTimeMs > 0) {
                    Text(
                        text = "${executionTimeMs}ms",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

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

                if (displayParams != null || displayOutput != null) {
                    Icon(
                        imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded) {
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

    val displayParams = if (showFullParams) fullParams else details
    val hasFullParams = fullParams != null && fullParams != details

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
                Icon(
                    imageVector = AutoDevComposeIcons.PlayArrow,
                    contentDescription = "Tool Call",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
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
        output.trim().startsWith("{") || output.trim().startsWith("[") -> {
            try {
                output.replace(",", ",\n")
                    .replace("{", "{\n  ")
                    .replace("}", "\n}")
                    .replace("[", "[\n  ")
                    .replace("]", "\n]")
            } catch (e: Exception) {
                output
            }
        }
        output.contains("│") -> output
        output.contains("\n") -> output
        output.length > 100 -> "${output.take(100)}..."
        else -> output
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isSuccess) AutoDevComposeIcons.CheckCircle else AutoDevComposeIcons.Error,
                        contentDescription = if (isSuccess) "Success" else "Error",
                        tint = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isSuccess) "Exit 0" else "Exit $exitCode",
                        color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
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
