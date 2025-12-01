package cc.unitmesh.devins.idea.components.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.render.TimelineItem
import com.intellij.openapi.project.Project
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Timeline content container for displaying chat history.
 * Similar to AgentMessageList in mpp-ui but using Jewel theming.
 */
@Composable
fun IdeaTimelineContent(
    timeline: List<TimelineItem>,
    streamingOutput: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    project: Project? = null
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
                IdeaTimelineItemView(item, project)
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
fun IdeaTimelineItemView(item: TimelineItem, project: Project? = null) {
    when (item) {
        is TimelineItem.MessageItem -> {
            IdeaMessageBubble(
                role = item.role,
                content = item.content
            )
        }
        is TimelineItem.ToolCallItem -> {
            IdeaToolCallBubble(item)
        }
        is TimelineItem.ErrorItem -> {
            IdeaErrorBubble(item.message)
        }
        is TimelineItem.TaskCompleteItem -> {
            IdeaTaskCompleteBubble(item)
        }
        is TimelineItem.TerminalOutputItem -> {
            IdeaTerminalOutputBubble(item, project = project)
        }
        is TimelineItem.LiveTerminalItem -> {
            // Live terminal not supported in IDEA yet, show placeholder
            IdeaTerminalOutputBubble(
                item = TimelineItem.TerminalOutputItem(
                    command = item.command,
                    output = "[Live terminal session: ${item.sessionId}]",
                    exitCode = 0,
                    executionTimeMs = 0
                ),
                project = project
            )
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

