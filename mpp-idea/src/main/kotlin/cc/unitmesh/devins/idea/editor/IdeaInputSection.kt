package cc.unitmesh.devins.idea.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

/**
 * Complete input section for mpp-idea module.
 * 
 * Combines a text input field with a bottom toolbar for actions.
 * Uses Jewel components for native IntelliJ IDEA integration.
 * 
 * Features:
 * - Multi-line text input with DevIn command support
 * - Enter to submit, Shift+Enter for newline
 * - Bottom toolbar with send/stop, @ trigger, settings
 * - Workspace and token info display
 * 
 * Note: This is a pure Compose implementation. For full DevIn language support
 * with completion, use IdeaDevInInput (Swing-based) embedded via ComposePanel.
 */
@Composable
fun IdeaInputSection(
    isProcessing: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit = {},
    onAtClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    workspacePath: String? = null,
    totalTokens: Int? = null,
    modifier: Modifier = Modifier
) {
    val textFieldState = rememberTextFieldState()
    var inputText by remember { mutableStateOf("") }

    // Sync text field state to inputText
    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { inputText = it }
    }

    // Extract send logic to avoid duplication
    val doSend: () -> Unit = {
        if (inputText.isNotBlank()) {
            onSend(inputText)
            textFieldState.edit { replace(0, length, "") }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(JewelTheme.globalColors.panelBackground)
            .border(
                width = 1.dp,
                color = JewelTheme.globalColors.borders.normal,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        // Input area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp, max = 200.dp)
                .padding(8.dp)
        ) {
            TextField(
                state = textFieldState,
                placeholder = { 
                    Text(
                        text = "Type your message or /help for commands...",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 14.sp,
                            color = JewelTheme.globalColors.text.info.copy(alpha = 0.6f)
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { keyEvent ->
                        // Enter to send (without modifiers)
                        if (keyEvent.key == Key.Enter &&
                            keyEvent.type == KeyEventType.KeyDown &&
                            !keyEvent.isShiftPressed &&
                            !keyEvent.isCtrlPressed &&
                            !keyEvent.isMetaPressed &&
                            !isProcessing
                        ) {
                            doSend()
                            true
                        } else {
                            false
                        }
                    },
                enabled = !isProcessing
            )
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth())

        // Bottom toolbar
        IdeaBottomToolbar(
            onSendClick = doSend,
            sendEnabled = inputText.isNotBlank() && !isProcessing,
            isExecuting = isProcessing,
            onStopClick = onStop,
            onAtClick = {
                // Insert @ character and trigger completion
                textFieldState.edit {
                    append("@")
                }
                onAtClick()
            },
            onSettingsClick = onSettingsClick,
            workspacePath = workspacePath,
            totalTokens = totalTokens
        )
    }
}

/**
 * Preview hints display for available commands.
 */
@Composable
fun InputHints(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "Enter to send, Shift+Enter for newline",
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 11.sp,
                color = JewelTheme.globalColors.text.info.copy(alpha = 0.6f)
            )
        )
    }
}

