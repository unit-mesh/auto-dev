package cc.unitmesh.devins.idea.toolwindow.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Timeline content container for displaying chat history.
 * Similar to AgentMessageList in mpp-ui but using Jewel theming.
 */
@Composable
fun IdeaTimelineContent(
    timeline: List<JewelRenderer.TimelineItem>,
    streamingOutput: String,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    if (timeline.isEmpty() && streamingOutput.isEmpty()) {
        IdeaEmptyStateMessage("Start a conversation with your AI Assistant!")
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(timeline, key = { it.id }) { item ->
                IdeaTimelineItemView(item)
            }

            // Show streaming output
            if (streamingOutput.isNotEmpty()) {
                item {
                    IdeaStreamingMessageBubble(streamingOutput)
                }
            }
        }
    }
}

/**
 * Dispatch timeline item to appropriate bubble component.
 */
@Composable
fun IdeaTimelineItemView(item: JewelRenderer.TimelineItem) {
    when (item) {
        is JewelRenderer.TimelineItem.MessageItem -> {
            IdeaMessageBubble(
                role = item.role,
                content = item.content
            )
        }
        is JewelRenderer.TimelineItem.ToolCallItem -> {
            IdeaToolCallBubble(item)
        }
        is JewelRenderer.TimelineItem.ErrorItem -> {
            IdeaErrorBubble(item.message)
        }
        is JewelRenderer.TimelineItem.TaskCompleteItem -> {
            IdeaTaskCompleteBubble(item)
        }
        is JewelRenderer.TimelineItem.TerminalOutputItem -> {
            IdeaTerminalOutputBubble(item)
        }
    }
}

/**
 * Empty state message displayed when timeline is empty.
 */
@Composable
fun IdeaEmptyStateMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 14.sp,
                color = JewelTheme.globalColors.text.info
            )
        )
    }
}

