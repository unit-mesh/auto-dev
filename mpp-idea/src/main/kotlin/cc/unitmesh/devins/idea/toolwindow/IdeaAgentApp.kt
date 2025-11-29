package cc.unitmesh.devins.idea.toolwindow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.model.AgentType
import cc.unitmesh.devins.idea.model.ChatMessage as ModelChatMessage
import cc.unitmesh.devins.idea.model.MessageRole
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
 * - Chat interface with message history
 * - LLM configuration support
 */
@Composable
fun IdeaAgentApp(viewModel: IdeaAgentViewModel) {
    val currentAgentType by viewModel.currentAgentType.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val streamingOutput by viewModel.streamingOutput.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val showConfigDialog by viewModel.showConfigDialog.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, streamingOutput) {
        if (messages.isNotEmpty() || streamingOutput.isNotEmpty()) {
            listState.animateScrollToItem(
                if (streamingOutput.isNotEmpty()) messages.size else messages.lastIndex.coerceAtLeast(0)
            )
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
                AgentType.CODING, AgentType.REMOTE -> {
                    ChatContent(
                        messages = messages,
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
        if (currentAgentType == AgentType.CODING || currentAgentType == AgentType.REMOTE) {
            ChatInputArea(
                isProcessing = isProcessing,
                onSend = { viewModel.sendMessage(it) },
                onAbort = { viewModel.abortRequest() }
            )
        }
    }
}

@Composable
private fun ChatContent(
    messages: List<ModelChatMessage>,
    streamingOutput: String,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    if (messages.isEmpty() && streamingOutput.isEmpty()) {
        EmptyStateMessage("Start a conversation with your AI Assistant!")
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
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
private fun MessageBubble(message: ModelChatMessage) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .background(
                    if (isUser)
                        JewelTheme.defaultBannerStyle.information.colors.background.copy(alpha = 0.75f)
                    else
                        JewelTheme.globalColors.panelBackground
                )
                .padding(8.dp)
        ) {
            Text(
                text = message.content,
                style = JewelTheme.defaultTextStyle
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
                .widthIn(max = 400.dp)
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
        // Left: Agent Type Tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgentType.entries.forEach { type ->
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
            text = type.displayName,
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
            placeholder = { Text("Type your message...") },
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

