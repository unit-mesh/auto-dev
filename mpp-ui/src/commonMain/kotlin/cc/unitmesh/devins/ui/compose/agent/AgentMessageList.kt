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
import cc.unitmesh.agent.render.TimelineItem
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.sketch.SketchRenderer
import kotlinx.coroutines.delay
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

    // Track if user manually scrolled away from bottom
    var userScrolledAway by remember { mutableStateOf(false) }

    // Track content updates from SketchRenderer for streaming content
    var streamingBlockCount by remember { mutableIntStateOf(0) }

    // Function to scroll to bottom
    fun scrollToBottomIfNeeded() {
        if (!userScrolledAway) {
            coroutineScope.launch {
                // Delay to ensure layout is complete before scrolling
                delay(50)
                val lastIndex = maxOf(0, listState.layoutInfo.totalItemsCount - 1)
                listState.scrollToItem(lastIndex)
            }
        }
    }

    // Monitor scroll state to detect user scrolling away
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            // If user scrolled to a position not near the bottom, they want to view history
            userScrolledAway = lastVisibleIndex < totalItems - 2
        }
    }

    // Scroll when timeline changes (new messages, tool calls, etc.)
    LaunchedEffect(renderer.timeline.size) {
        if (renderer.timeline.isNotEmpty()) {
            userScrolledAway = false
            coroutineScope.launch {
                delay(50)
                listState.animateScrollToItem(
                    index = maxOf(0, listState.layoutInfo.totalItemsCount - 1)
                )
            }
        }
    }

    // Scroll when streaming content changes
    LaunchedEffect(renderer.currentStreamingOutput) {
        if (renderer.currentStreamingOutput.isNotEmpty()) {
            // Calculate content signature based on line count and character chunks
            val lineCount = renderer.currentStreamingOutput.count { it == '\n' }
            val chunkIndex = renderer.currentStreamingOutput.length / 100
            val contentSignature = lineCount + chunkIndex

            // Delay to ensure Markdown layout is complete
            delay(100)
            scrollToBottomIfNeeded()
        }
    }

    // Scroll when SketchRenderer reports new blocks rendered
    LaunchedEffect(streamingBlockCount) {
        if (streamingBlockCount > 0) {
            delay(50)
            scrollToBottomIfNeeded()
        }
    }

    // Reset user scroll state when streaming starts
    LaunchedEffect(renderer.isProcessing) {
        if (renderer.isProcessing) {
            userScrolledAway = false
        }
    }

    LazyColumn(
        state = listState,
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(
            items = renderer.timeline,
            key = { "${it.timestamp}_${it.hashCode()}" }
        ) { timelineItem ->
            RenderMessageItem(
                timelineItem = timelineItem,
                onOpenFileViewer = onOpenFileViewer,
                renderer = renderer,
                onExpand = {
                    coroutineScope.launch {
                        // Scroll to the bottom when an item expands
                        delay(50)
                        val totalItems = listState.layoutInfo.totalItemsCount
                        if (totalItems > 0) {
                            listState.animateScrollToItem(totalItems - 1)
                        }
                    }
                }
            )
        }

        if (renderer.currentStreamingOutput.isNotEmpty()) {
            item(key = "streaming") {
                StreamingMessageItem(
                    content = renderer.currentStreamingOutput,
                    onContentUpdate = { blockCount ->
                        // When SketchRenderer renders new blocks, trigger scroll
                        streamingBlockCount = blockCount
                    }
                )
            }
        }

        renderer.currentToolCall?.let { toolCall ->
            item(key = "current_tool_call") {
                CurrentToolCallItem(toolCall = toolCall)
            }
        }
    }
}

@Composable
fun RenderMessageItem(
    timelineItem: TimelineItem,
    onOpenFileViewer: ((String) -> Unit)?,
    renderer: ComposeRenderer,
    onExpand: () -> Unit = {}
) {
    when (timelineItem) {
        is TimelineItem.MessageItem -> {
            val msg = timelineItem.message ?: Message(
                role = timelineItem.role,
                content = timelineItem.content,
                timestamp = timelineItem.timestamp
            )
            MessageItem(
                message = msg,
                tokenInfo = timelineItem.tokenInfo
            )
        }

        is TimelineItem.ToolCallItem -> {
            ToolItem(
                toolName = timelineItem.toolName,
                details = timelineItem.params,
                fullParams = timelineItem.fullParams,
                filePath = timelineItem.filePath,
                toolType = timelineItem.toolType,
                success = timelineItem.success,
                summary = timelineItem.summary,
                output = timelineItem.output,
                fullOutput = timelineItem.fullOutput,
                executionTimeMs = timelineItem.executionTimeMs,
                docqlStats = timelineItem.docqlStats,
                onOpenFileViewer = onOpenFileViewer,
                onExpand = onExpand
            )
        }

        is TimelineItem.ErrorItem -> {
            ToolErrorItem(error = timelineItem.message, onDismiss = { renderer.clearError() })
        }

        is TimelineItem.TaskCompleteItem -> {
            TaskCompletedItem(
                success = timelineItem.success,
                message = timelineItem.message
            )
        }

        is TimelineItem.TerminalOutputItem -> {
            TerminalOutputItem(
                command = timelineItem.command,
                output = timelineItem.output,
                exitCode = timelineItem.exitCode,
                executionTimeMs = timelineItem.executionTimeMs,
                onExpand = onExpand
            )
        }

        is TimelineItem.LiveTerminalItem -> {
            LiveTerminalItem(
                sessionId = timelineItem.sessionId,
                command = timelineItem.command,
                workingDirectory = timelineItem.workingDirectory,
                ptyHandle = timelineItem.ptyHandle
            )
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
                    if (!isUser) {
                        SketchRenderer.RenderResponse(
                            content = message.content,
                            isComplete = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = message.content,
                            fontFamily = if (Platform.isWasm) FontFamily(Font(Res.font.NotoSansSC_Regular)) else FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

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
fun StreamingMessageItem(
    content: String,
    onContentUpdate: (blockCount: Int) -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                // Use SketchRenderer to support thinking blocks in streaming content
                // Pass onContentUpdate to trigger scroll when new blocks are rendered
                SketchRenderer.RenderResponse(
                    content = content,
                    isComplete = false,
                    onContentUpdate = onContentUpdate,
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

