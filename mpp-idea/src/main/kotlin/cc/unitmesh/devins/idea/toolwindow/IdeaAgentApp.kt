package cc.unitmesh.devins.idea.toolwindow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.theme.defaultBannerStyle

/**
 * Main Compose application for Agent ToolWindow.
 *
 * Features:
 * - Tab-based agent type switching (Agentic, Review, Knowledge, Remote)
 * - Timeline-based chat interface with tool calls
 * - LLM configuration support via mpp-ui's ConfigManager
 * - Real agent execution using mpp-core's CodingAgent
 * - Tool loading status bar
 *
 * Aligned with AgentChatInterface from mpp-ui for feature parity.
 */
@Composable
fun IdeaAgentApp(viewModel: IdeaAgentViewModel) {
    val currentAgentType by viewModel.currentAgentType.collectAsState()
    val timeline by viewModel.renderer.timeline.collectAsState()
    val streamingOutput by viewModel.renderer.currentStreamingOutput.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val showConfigDialog by viewModel.showConfigDialog.collectAsState()
    val mcpPreloadingMessage by viewModel.mcpPreloadingMessage.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new items arrive
    LaunchedEffect(timeline.size, streamingOutput) {
        if (timeline.isNotEmpty() || streamingOutput.isNotEmpty()) {
            val targetIndex = if (streamingOutput.isNotEmpty()) timeline.size else timeline.lastIndex.coerceAtLeast(0)
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        // Agent Type Tabs Header
        AgentTabsHeader(
            currentAgentType = currentAgentType,
            onAgentTypeChange = { viewModel.onAgentTypeChange(it) },
            onNewChat = { viewModel.clearHistory() },
            onSettings = { viewModel.setShowConfigDialog(true) }
        )

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Content based on agent type
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (currentAgentType) {
                AgentType.CODING, AgentType.REMOTE, AgentType.LOCAL_CHAT -> {
                    TimelineContent(
                        timeline = timeline,
                        streamingOutput = streamingOutput,
                        listState = listState
                    )
                }
                AgentType.CODE_REVIEW -> {
                    CodeReviewContent()
                }
                AgentType.KNOWLEDGE -> {
                    KnowledgeContent()
                }
            }
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Input area (only for chat-based modes)
        if (currentAgentType == AgentType.CODING || currentAgentType == AgentType.REMOTE || currentAgentType == AgentType.LOCAL_CHAT) {
            ChatInputArea(
                isProcessing = isExecuting,
                onSend = { viewModel.sendMessage(it) },
                onAbort = { viewModel.cancelTask() }
            )
        }

        // Tool loading status bar
        ToolLoadingStatusBar(
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TimelineContent(
    timeline: List<JewelRenderer.TimelineItem>,
    streamingOutput: String,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    if (timeline.isEmpty() && streamingOutput.isEmpty()) {
        EmptyStateMessage("Start a conversation with your AI Assistant!")
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(timeline, key = { it.timestamp }) { item ->
                TimelineItemView(item)
            }

            // Show streaming output
            if (streamingOutput.isNotEmpty()) {
                item {
                    StreamingMessageBubble(streamingOutput)
                }
            }
        }
    }
}

@Composable
private fun TimelineItemView(item: JewelRenderer.TimelineItem) {
    when (item) {
        is JewelRenderer.TimelineItem.MessageItem -> {
            MessageBubble(
                role = item.role,
                content = item.content
            )
        }
        is JewelRenderer.TimelineItem.ToolCallItem -> {
            ToolCallBubble(item)
        }
        is JewelRenderer.TimelineItem.ErrorItem -> {
            ErrorBubble(item.message)
        }
        is JewelRenderer.TimelineItem.TaskCompleteItem -> {
            TaskCompleteBubble(item)
        }
        is JewelRenderer.TimelineItem.TerminalOutputItem -> {
            TerminalOutputBubble(item)
        }
    }
}

@Composable
private fun CodeReviewContent() {
    EmptyStateMessage("Code Review mode - Coming soon!")
}

@Composable
private fun KnowledgeContent() {
    EmptyStateMessage("Knowledge mode - Coming soon!")
}

@Composable
private fun EmptyStateMessage(text: String) {
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

@Composable
private fun MessageBubble(role: JewelRenderer.MessageRole, content: String) {
    val isUser = role == JewelRenderer.MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .background(
                    if (isUser)
                        JewelTheme.defaultBannerStyle.information.colors.background.copy(alpha = 0.75f)
                    else
                        JewelTheme.globalColors.panelBackground
                )
                .padding(8.dp)
        ) {
            Text(
                text = content,
                style = JewelTheme.defaultTextStyle
            )
        }
    }
}

@Composable
private fun ToolCallBubble(item: JewelRenderer.TimelineItem.ToolCallItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            Column {
                // Tool name with icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusIcon = when (item.success) {
                        true -> "✓"
                        false -> "✗"
                        null -> "⏳"
                    }
                    // Use AutoDevColors design system - use lighter colors for dark theme compatibility
                    val statusColor = when (item.success) {
                        true -> AutoDevColors.Green.c400 // Success color from design system
                        false -> AutoDevColors.Red.c400 // Error color from design system
                        null -> JewelTheme.globalColors.text.info
                    }
                    Text(
                        text = statusIcon,
                        style = JewelTheme.defaultTextStyle.copy(color = statusColor)
                    )
                    Text(
                        text = "🔧 ${item.toolName}",
                        style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Tool parameters (truncated)
                if (item.params.isNotEmpty()) {
                    Text(
                        text = item.params.take(200) + if (item.params.length > 200) "..." else "",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 12.sp,
                            color = JewelTheme.globalColors.text.info
                        )
                    )
                }

                // Tool output (if available)
                item.output?.let { output ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = output.take(300) + if (output.length > 300) "..." else "",
                        style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBubble(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .background(AutoDevColors.Red.c400.copy(alpha = 0.2f)) // Error background from design system
                .padding(8.dp)
        ) {
            Text(
                text = "❌ $message",
                style = JewelTheme.defaultTextStyle.copy(
                    color = AutoDevColors.Red.c400 // Error text color from design system
                )
            )
        }
    }
}

@Composable
private fun TaskCompleteBubble(item: JewelRenderer.TimelineItem.TaskCompleteItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    // Use AutoDevColors design system with alpha for background
                    if (item.success)
                        AutoDevColors.Green.c400.copy(alpha = 0.2f)
                    else
                        AutoDevColors.Red.c400.copy(alpha = 0.2f)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val icon = if (item.success) "✅" else "❌"
            Text(
                text = "$icon ${item.message} (${item.iterations} iterations)",
                style = JewelTheme.defaultTextStyle.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun StreamingMessageBubble(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .background(JewelTheme.globalColors.panelBackground)
                .padding(8.dp)
        ) {
            Text(
                text = content + "▌",
                style = JewelTheme.defaultTextStyle
            )
        }
    }
}

@Composable
private fun AgentTabsHeader(
    currentAgentType: AgentType,
    onAgentTypeChange: (AgentType) -> Unit,
    onNewChat: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Agent Type Tabs (show main agent types, skip LOCAL_CHAT as it's similar to CODING)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show only main agent types for cleaner UI
            listOf(AgentType.CODING, AgentType.CODE_REVIEW, AgentType.KNOWLEDGE, AgentType.REMOTE).forEach { type ->
                AgentTab(
                    type = type,
                    isSelected = type == currentAgentType,
                    onClick = { onAgentTypeChange(type) }
                )
            }
        }

        // Right: Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNewChat) {
                Text("+", style = JewelTheme.defaultTextStyle.copy(fontWeight = FontWeight.Bold))
            }
            IconButton(onClick = onSettings) {
                Text("⚙", style = JewelTheme.defaultTextStyle)
            }
        }
    }
}

@Composable
private fun AgentTab(
    type: AgentType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        JewelTheme.defaultBannerStyle.information.colors.background.copy(alpha = 0.5f)
    } else {
        JewelTheme.globalColors.panelBackground
    }

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(28.dp)
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = type.getDisplayName(),
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun TerminalOutputBubble(item: JewelRenderer.TimelineItem.TerminalOutputItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .background(AutoDevColors.Neutral.c900) // Terminal background from design system
                .padding(8.dp)
        ) {
            Column {
                // Command header
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$ ${item.command}",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontWeight = FontWeight.Bold,
                            color = AutoDevColors.Cyan.c400 // Cyan for commands from design system
                        )
                    )
                    val exitColor = if (item.exitCode == 0) AutoDevColors.Green.c400 else AutoDevColors.Red.c400
                    Text(
                        text = "exit: ${item.exitCode}",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = exitColor
                        )
                    )
                    Text(
                        text = "${item.executionTimeMs}ms",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = JewelTheme.globalColors.text.info
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Output content
                val outputText = item.output.take(1000) + if (item.output.length > 1000) "\n..." else ""
                Text(
                    text = outputText,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 12.sp,
                        color = AutoDevColors.Neutral.c300 // Light gray for output from design system
                    )
                )
            }
        }
    }
}

@Composable
private fun ToolLoadingStatusBar(
    viewModel: IdeaAgentViewModel,
    modifier: Modifier = Modifier
) {
    val mcpPreloadingMessage by viewModel.mcpPreloadingMessage.collectAsState()
    val mcpPreloadingStatus by viewModel.mcpPreloadingStatus.collectAsState()
    // Recompute when preloading status changes to make it reactive
    val toolStatus = remember(mcpPreloadingStatus) { viewModel.getToolLoadingStatus() }

    Row(
        modifier = modifier
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SubAgents status
        ToolStatusChip(
            label = "SubAgents",
            count = toolStatus.subAgentsEnabled,
            total = toolStatus.subAgentsTotal,
            isLoading = false,
            color = AutoDevColors.Blue.c400
        )

        // MCP Tools status
        ToolStatusChip(
            label = "MCP Tools",
            count = toolStatus.mcpToolsEnabled,
            total = if (toolStatus.isLoading) -1 else toolStatus.mcpToolsTotal,
            isLoading = toolStatus.isLoading,
            color = if (!toolStatus.isLoading && toolStatus.mcpToolsEnabled > 0)
                AutoDevColors.Green.c400
            else
                JewelTheme.globalColors.text.info
        )

        Spacer(modifier = Modifier.weight(1f))

        // Status message
        if (mcpPreloadingMessage.isNotEmpty()) {
            Text(
                text = mcpPreloadingMessage,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = JewelTheme.globalColors.text.info
                ),
                maxLines = 1
            )
        } else if (!toolStatus.isLoading && toolStatus.mcpServersLoaded > 0) {
            Text(
                text = "✓ All tools ready",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = AutoDevColors.Green.c400
                )
            )
        }
    }
}

@Composable
private fun ToolStatusChip(
    label: String,
    count: Int,
    total: Int,
    isLoading: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Status indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isLoading) JewelTheme.globalColors.text.info.copy(alpha = 0.5f) else color,
                    shape = CircleShape
                )
        )

        val totalDisplay = if (total < 0) "∞" else total.toString()
        Text(
            text = "$label ($count/$totalDisplay)",
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 11.sp,
                color = if (isLoading)
                    JewelTheme.globalColors.text.info.copy(alpha = 0.7f)
                else
                    JewelTheme.globalColors.text.info
            )
        )
    }
}

@Composable
private fun ChatInputArea(
    isProcessing: Boolean,
    onSend: (String) -> Unit,
    onAbort: () -> Unit
) {
    val textFieldState = rememberTextFieldState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { inputText = it }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            state = textFieldState,
            placeholder = { Text("Type your message or /help for commands...") },
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown && !isProcessing) {
                        if (inputText.isNotBlank()) {
                            onSend(inputText)
                            textFieldState.edit { replace(0, length, "") }
                        }
                        true
                    } else {
                        false
                    }
                },
            enabled = !isProcessing
        )

        if (isProcessing) {
            DefaultButton(onClick = onAbort) {
                Text("Stop")
            }
        } else {
            DefaultButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSend(inputText)
                        textFieldState.edit { replace(0, length, "") }
                    }
                },
                enabled = inputText.isNotBlank()
            ) {
                Text("Send")
            }
        }
    }
}

