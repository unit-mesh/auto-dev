package cc.unitmesh.devins.idea.agent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.agent.IdeaCodingAgentViewModel
import cc.unitmesh.devins.idea.agent.MessageInputState
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

/**
 * Main CodingAgent panel for IntelliJ IDEA.
 * Uses Jewel theme for native integration.
 */
@Composable
fun CodingAgentPanel(viewModel: IdeaCodingAgentViewModel) {
    val inputState by viewModel.inputState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        // Header
        AgentHeader(
            onNewConversation = { viewModel.onNewConversation() }
        )

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Message list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (viewModel.renderer.timeline.isEmpty() && !viewModel.isExecuting) {
                EmptyStateMessage()
            } else {
                AgentMessageList(
                    renderer = viewModel.renderer,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Input area
        AgentInput(
            inputState = inputState,
            isExecuting = viewModel.isExecuting,
            onInputChanged = { viewModel.onInputChanged(it) },
            onSubmit = { viewModel.executeTask(it) },
            onStop = { viewModel.cancelTask() }
        )
    }
}

@Composable
private fun AgentHeader(onNewConversation: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CodingAgent",
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🤖 CodingAgent",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Describe your coding task to get started!",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 14.sp,
                    color = JewelTheme.globalColors.text.info
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Commands: /clear, /help",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 12.sp,
                    color = JewelTheme.globalColors.text.info
                )
            )
        }
    }
}

@Composable
private fun AgentInput(
    inputState: MessageInputState,
    isExecuting: Boolean,
    onInputChanged: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onStop: () -> Unit
) {
    val textFieldState = rememberTextFieldState()

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
            placeholder = { Text("Describe your coding task...") },
            modifier = Modifier
                .weight(1f)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown && !isExecuting) {
                        val text = textFieldState.text.toString()
                        if (text.isNotBlank()) {
                            onSubmit(text)
                            textFieldState.edit { replace(0, length, "") }
                        }
                        true
                    } else false
                },
            enabled = !isExecuting
        )

        if (isExecuting) {
            DefaultButton(onClick = onStop) { Text("Stop") }
        } else {
            DefaultButton(
                onClick = {
                    val text = textFieldState.text.toString()
                    if (text.isNotBlank()) {
                        onSubmit(text)
                        textFieldState.edit { replace(0, length, "") }
                    }
                },
                enabled = inputState is MessageInputState.Enabled
            ) { Text("Send") }
        }
    }
}

