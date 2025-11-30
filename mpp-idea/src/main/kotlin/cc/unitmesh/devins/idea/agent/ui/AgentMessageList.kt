package cc.unitmesh.devins.idea.agent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.idea.agent.IdeaAgentRenderer
import cc.unitmesh.devins.idea.agent.TimelineItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * Agent message list component for displaying the agent timeline.
 * Uses Jewel theme for IntelliJ IDEA integration.
 */
@Composable
fun AgentMessageList(
    renderer: IdeaAgentRenderer,
    modifier: Modifier = Modifier,
    onOpenFileViewer: ((String) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Track if user manually scrolled away from bottom
    var userScrolledAway by remember { mutableStateOf(false) }

    // Function to scroll to bottom
    fun scrollToBottomIfNeeded() {
        if (!userScrolledAway) {
            coroutineScope.launch {
                delay(50)
                val lastIndex = maxOf(0, listState.layoutInfo.totalItemsCount - 1)
                listState.scrollToItem(lastIndex)
            }
        }
    }

    // Monitor scroll state
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            userScrolledAway = lastVisibleIndex < totalItems - 2
        }
    }

    // Scroll when timeline changes
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
            delay(100)
            scrollToBottomIfNeeded()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(
            items = renderer.timeline,
            key = { "${it.timestamp}_${it.hashCode()}" }
        ) { timelineItem ->
            RenderTimelineItem(
                timelineItem = timelineItem,
                onOpenFileViewer = onOpenFileViewer,
                onExpand = {
                    coroutineScope.launch {
                        delay(50)
                        val totalItems = listState.layoutInfo.totalItemsCount
                        if (totalItems > 0) {
                            listState.animateScrollToItem(totalItems - 1)
                        }
                    }
                }
            )
        }

        // Show streaming content
        if (renderer.currentStreamingOutput.isNotEmpty()) {
            item(key = "streaming") {
                StreamingMessageItem(content = renderer.currentStreamingOutput)
            }
        }

        // Show current tool call
        renderer.currentToolCall?.let { toolCall ->
            item(key = "current_tool_call") {
                CurrentToolCallItem(toolCall = toolCall)
            }
        }
    }
}

/**
 * Render a timeline item based on its type.
 */
@Composable
private fun RenderTimelineItem(
    timelineItem: TimelineItem,
    onOpenFileViewer: ((String) -> Unit)?,
    onExpand: () -> Unit = {}
) {
    when (timelineItem) {
        is TimelineItem.MessageItem -> {
            MessageItem(
                content = timelineItem.content,
                isUser = timelineItem.isUser,
                tokenInfo = timelineItem.tokenInfo
            )
        }
        is TimelineItem.ToolCallItem -> {
            ToolCallItem(
                toolName = timelineItem.toolName,
                details = timelineItem.details,
                success = timelineItem.success,
                summary = timelineItem.summary,
                output = timelineItem.output,
                executionTimeMs = timelineItem.executionTimeMs,
                onExpand = onExpand
            )
        }
        is TimelineItem.ErrorItem -> {
            ErrorItem(error = timelineItem.error)
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
    }
}

