package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.agent.AgentMessageList
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

/**
 * AI 聊天面板 - 右侧
 * 专用于文档问答，集成 AgentMessageList
 */
@Composable
fun DocumentChatPane(
    viewModel: DocumentReaderViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(AutoDevComposeIcons.SmartToy, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI 助手", style = MaterialTheme.typography.titleMedium)
        }

        HorizontalDivider()

        // 消息列表 (使用 AgentMessageList)
        AgentMessageList(
            renderer = viewModel.renderer,
            modifier = Modifier.weight(1f)
        )

        HorizontalDivider()

        // 输入区域
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
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask about the document...") },
            maxLines = 4,
            enabled = !isGenerating
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (isGenerating) {
            IconButton(onClick = onStopGeneration) {
                Icon(AutoDevComposeIcons.Stop, contentDescription = "Stop")
            }
        } else {
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Icon(AutoDevComposeIcons.Send, contentDescription = "Send")
            }
        }
    }
}
