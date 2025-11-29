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
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.theme.defaultBannerStyle

/**
 * Main Compose application for AutoDev Chat.
 *
 * Uses Jewel theme for native IntelliJ IDEA integration.
 */
@Composable
fun AutoDevChatApp(viewModel: AutoDevChatViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val inputState by viewModel.inputState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        // Header
        ChatHeader(
            onNewConversation = { viewModel.onNewConversation() }
        )

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Message list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (messages.isEmpty()) {
                EmptyStateMessage()
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
                }
            }
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Input area
        ChatInput(
            inputState = inputState,
            onInputChanged = { viewModel.onInputChanged(it) },
            onSend = { viewModel.onSendMessage() },
            onAbort = { viewModel.onAbortMessage() }
        )
    }
}

@Composable
private fun ChatHeader(
    onNewConversation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "AutoDev Chat",
            style = JewelTheme.defaultTextStyle.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )

        IconButton(onClick = onNewConversation) {
            Text("+", style = JewelTheme.defaultTextStyle)
        }
    }
}

@Composable
private fun EmptyStateMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Start a conversation with your AI Assistant!",
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 16.sp,
                color = JewelTheme.globalColors.text.info
            )
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (message.isUser)
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
private fun ChatInput(
    inputState: MessageInputState,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAbort: () -> Unit
) {
    val textFieldState = rememberTextFieldState()
    val isSending = inputState is MessageInputState.Sending

    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { onInputChanged(it) }
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
                    if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown && !isSending) {
                        onSend()
                        textFieldState.edit { replace(0, length, "") }
                        true
                    } else {
                        false
                    }
                },
            enabled = !isSending
        )

        if (isSending) {
            DefaultButton(onClick = onAbort) {
                Text("Stop")
            }
        } else {
            DefaultButton(
                onClick = {
                    onSend()
                    textFieldState.edit { replace(0, length, "") }
                },
                enabled = inputState is MessageInputState.Enabled
            ) {
                Text("Send")
            }
        }
    }
}

