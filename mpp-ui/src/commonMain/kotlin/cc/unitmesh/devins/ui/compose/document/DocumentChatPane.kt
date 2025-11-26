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

/**
 * Document Chat Pane - AI chat interface for document queries
 *
 * This pane displays the indexing status and chat messages.
 * The top bar (title, actions) is now handled by the parent page using AgentTopAppBar.
 *
 * @param viewModel The DocumentReaderViewModel that manages document state
 * @param modifier Modifier for the pane
 */
@Composable
fun DocumentChatPane(
    viewModel: DocumentReaderViewModel,
    modifier: Modifier = Modifier
) {
    val indexingStatus by viewModel.indexingStatus.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        IndexingStatusBanner(indexingStatus)

        Box(modifier = Modifier.weight(1f)) {
            AgentMessageList(
                renderer = viewModel.renderer,
                modifier = Modifier.fillMaxSize()
            )

            // 当没有消息且索引完成时显示欢迎提示
            if (indexingStatus is cc.unitmesh.devins.service.IndexingStatus.Completed
                && viewModel.renderer.timeline.isEmpty()) {
                WelcomeMessage(
                    onQuickQuery = { query ->
                        viewModel.sendMessage(query)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        HorizontalDivider()

        ChatInputArea(
            isGenerating = viewModel.isGenerating,
            onSendMessage = { viewModel.sendMessage(it) },
            onStopGeneration = { viewModel.stopGeneration() }
        )
    }
}

/**
 * Indexing status banner - shows the current indexing progress or completion status
 */
@Composable
private fun IndexingStatusBanner(
    indexingStatus: cc.unitmesh.devins.service.IndexingStatus
) {
    when (indexingStatus) {
        is cc.unitmesh.devins.service.IndexingStatus.Indexing -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Indexing project code...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                        Text(
                            text = "Completed ${indexingStatus.current}/${indexingStatus.total} documents",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is cc.unitmesh.devins.service.IndexingStatus.Completed -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        AutoDevComposeIcons.CheckCircle,
                        contentDescription = "Indexing complete",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Indexing Complete",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Indexed ${indexingStatus.total} documents (${indexingStatus.succeeded} succeeded, ${indexingStatus.failed} failed)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is cc.unitmesh.devins.service.IndexingStatus.Idle -> {
            // Don't show banner when idle
        }
    }
}

/**
 * Welcome message and quick query suggestions
 */
@Composable
private fun WelcomeMessage(
    onQuickQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = AutoDevComposeIcons.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "欢迎使用 AI 代码助手",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "我可以帮你理解项目代码、查找类和方法、解释代码逻辑",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "💡 试试这些查询：",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 快速查询建议
        val quickQueries = listOf(
            "What is DocQLExecutor?" to "了解 DocQL 执行器",
            "How does CodingAgent work?" to "了解代码生成 Agent",
            "List all classes in document package" to "查看文档包的所有类",
            "Find all parse methods" to "查找所有解析方法"
        )

        quickQueries.forEach { (query, description) ->
            OutlinedCard(
                onClick = { onQuickQuery(query) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = AutoDevComposeIcons.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        AutoDevComposeIcons.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
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
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        AutoDevComposeIcons.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(16.dp),
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
