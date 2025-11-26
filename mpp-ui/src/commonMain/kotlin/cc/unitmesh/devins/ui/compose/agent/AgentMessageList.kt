package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import cc.unitmesh.devins.ui.compose.sketch.SketchRenderer
import kotlinx.coroutines.launch
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

    // Additional effect to handle streaming content growth within the same item
    LaunchedEffect(renderer.currentStreamingOutput.length) {
        if (renderer.currentStreamingOutput.isNotEmpty()) {
            coroutineScope.launch {
                listState.scrollToItem(
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
                        docqlStats = timelineItem.docqlStats,
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            PlatformMessageTextContainer(text = message.content) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Use SketchRenderer for assistant messages to support thinking blocks
                    if (!isUser) {
                        SketchRenderer.RenderResponse(
                            content = message.content,
                            isComplete = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // For user messages, use simple text
                        Text(
                            text = message.content,
                            fontFamily = if (Platform.isWasm) FontFamily(Font(Res.font.NotoSansSC_Regular)) else FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Display token info if available (only for assistant messages)
                    if (!isUser && tokenInfo != null && tokenInfo.totalTokens > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${tokenInfo.inputTokens} + ${tokenInfo.outputTokens} (${tokenInfo.totalTokens} tokens)",
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                // Use SketchRenderer to support thinking blocks in streaming content
                SketchRenderer.RenderResponse(
                    content = content,
                    isComplete = false,
                    modifier = Modifier.fillMaxWidth()
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
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
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

