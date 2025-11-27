package cc.unitmesh.devins.ui.compose.document

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.ui.compose.agent.AgentMessageList
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.indexer.DomainDictService
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.PromptEnhancer
import kotlinx.coroutines.launch

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

/**
 * Enhanced Chat Input Area with PromptEnhancer support
 *
 * Features:
 * - Ctrl+P to enhance prompt using domain dictionary
 * - Desktop: Enter to send, Shift+Enter for new line
 * - Mobile: IME Send action, dismisses keyboard after send
 */
@Composable
private fun ChatInputArea(
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    // PromptEnhancer related state
    var isEnhancing by remember { mutableStateOf(false) }
    var enhancer by remember { mutableStateOf<PromptEnhancer?>(null) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isMobile = Platform.isAndroid || Platform.isIOS

    // Initialize PromptEnhancer
    LaunchedEffect(Unit) {
        try {
            val workspace = WorkspaceManager.currentWorkspace
            val projectPath = workspace?.rootPath
            if (projectPath != null) {
                val configWrapper = ConfigManager.load()
                val activeConfig = configWrapper.getActiveModelConfig()
                if (activeConfig != null && activeConfig.isValid()) {
                    val llmService = KoogLLMService.create(activeConfig)
                    val fileSystem = workspace.fileSystem
                    val domainDictService = DomainDictService(fileSystem)
                    enhancer = PromptEnhancer(llmService, fileSystem, domainDictService)
                    println("[DocumentChat] PromptEnhancer initialized successfully")
                }
            }
        } catch (e: Exception) {
            println("[DocumentChat] Failed to initialize PromptEnhancer: ${e.message}")
        }
    }

    // Enhance current input function
    fun enhanceCurrentInput() {
        val currentEnhancer = enhancer
        if (currentEnhancer == null || textFieldValue.text.isBlank() || isEnhancing) {
            return
        }

        scope.launch {
            try {
                isEnhancing = true
                println("[Enhancement] Enhancing current input...")

                val enhanced = currentEnhancer.enhance(textFieldValue.text.trim(), "zh")

                if (enhanced.isNotEmpty() && enhanced != textFieldValue.text.trim() && enhanced.length > textFieldValue.text.trim().length) {
                    textFieldValue = TextFieldValue(
                        text = enhanced,
                        selection = androidx.compose.ui.text.TextRange(enhanced.length)
                    )
                    println("[Enhancement] Enhanced: \"${textFieldValue.text.trim().take(50)}...\" -> \"${enhanced.take(50)}...\"")
                } else {
                    println("[Enhancement] No enhancement needed or failed")
                }
            } catch (e: Exception) {
                println("[Enhancement] Failed: ${e.message}")
            } finally {
                isEnhancing = false
            }
        }
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false

        return when {
            // Desktop: Enter to send (without Shift)
            !isMobile && event.key == Key.Enter && !event.isShiftPressed -> {
                if (textFieldValue.text.isNotBlank()) {
                    onSendMessage(textFieldValue.text)
                    textFieldValue = TextFieldValue("")
                }
                true
            }

            // Ctrl+P to enhance prompt
            event.key == Key.P && event.isCtrlPressed -> {
                enhanceCurrentInput()
                true
            }

            else -> false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 120.dp)
                            .onPreviewKeyEvent { handleKeyEvent(it) },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 6,
                        enabled = !isGenerating && !isEnhancing,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = if (isMobile) ImeAction.Send else ImeAction.Default
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textFieldValue.text.isNotBlank()) {
                                    onSendMessage(textFieldValue.text)
                                    textFieldValue = TextFieldValue("")
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            ) {
                                if (textFieldValue.text.isEmpty()) {
                                    Text(
                                        text = if (isMobile) "Ask about the document..." else "Ask about the document... (Shift+Enter for new line)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Enhance button (only show when enhancer is available and text is not blank)
                    if (enhancer != null && textFieldValue.text.isNotBlank() && !isGenerating) {
                        IconButton(
                            onClick = { enhanceCurrentInput() },
                            enabled = !isEnhancing,
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isEnhancing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    AutoDevComposeIcons.Custom.AI,
                                    contentDescription = "Enhance prompt (Ctrl+P)",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (isGenerating) {
                        IconButton(
                            onClick = onStopGeneration,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                AutoDevComposeIcons.Stop,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (textFieldValue.text.isNotBlank()) {
                                    onSendMessage(textFieldValue.text)
                                    textFieldValue = TextFieldValue("")
                                    if (isMobile) {
                                        focusManager.clearFocus()
                                    }
                                }
                            },
                            enabled = textFieldValue.text.isNotBlank() && !isEnhancing,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                AutoDevComposeIcons.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(18.dp),
                                tint = if (textFieldValue.text.isNotBlank() && !isEnhancing) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                        }
                    }
                }

                // Hint text for Ctrl+P (desktop only)
                if (!isMobile && enhancer != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (isEnhancing) "Enhancing..." else "Ctrl+P to enhance with domain knowledge",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
