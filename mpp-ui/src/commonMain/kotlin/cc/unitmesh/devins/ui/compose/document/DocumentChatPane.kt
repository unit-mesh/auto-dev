package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.agent.AgentMessageList
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

@Composable
fun DocumentChatPane(
    viewModel: DocumentReaderViewModel,
    modifier: Modifier = Modifier
) {
    val indexingStatus by viewModel.indexingStatus.collectAsState()
    
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(AutoDevComposeIcons.SmartToy, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI 助手", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            
            // 索引状态指示器
            when (indexingStatus) {
                is cc.unitmesh.devins.service.IndexingStatus.Indexing -> {
                    val status = indexingStatus as cc.unitmesh.devins.service.IndexingStatus.Indexing
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "${status.current}/${status.total}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                is cc.unitmesh.devins.service.IndexingStatus.Completed -> {
                    Icon(
                        AutoDevComposeIcons.CheckCircle,
                        contentDescription = "索引完成",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                else -> {}
            }
            
            IconButton(
                onClick = { viewModel.clearChatHistory() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    AutoDevComposeIcons.Delete,
                    contentDescription = "Clear History",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider()

        AgentMessageList(
            renderer = viewModel.renderer,
            modifier = Modifier.weight(1f)
        )

        HorizontalDivider()

        ChatInputArea(
            isGenerating = viewModel.isGenerating,
            onSendMessage = { viewModel.sendMessage(it) },
            onStopGeneration = { viewModel.stopGeneration() }
        )
    }
}

@Composable
private fun ChatInputArea(
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit
) {
    var textFieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 120.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when {
                                // Enter to send (without Shift)
                                event.key == Key.Enter &&
                                !event.isShiftPressed &&
                                textFieldValue.text.isNotBlank() -> {
                                    onSendMessage(textFieldValue.text)
                                    textFieldValue = TextFieldValue("")
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 6,
                enabled = !isGenerating,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "Ask about the document... (Shift+Enter for new line)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isGenerating) {
                IconButton(
                    onClick = onStopGeneration,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        AutoDevComposeIcons.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        if (textFieldValue.text.isNotBlank()) {
                            onSendMessage(textFieldValue.text)
                            textFieldValue = androidx.compose.ui.text.input.TextFieldValue("")
                        }
                    },
                    enabled = textFieldValue.text.isNotBlank(),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        AutoDevComposeIcons.Send,
                        contentDescription = "Send",
                        tint = if (textFieldValue.text.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }
                    )
                }
            }
        }
    }
}
