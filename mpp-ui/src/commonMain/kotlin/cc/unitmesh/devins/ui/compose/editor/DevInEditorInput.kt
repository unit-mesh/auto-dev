package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import autodev_intellij.mpp_ui.generated.resources.NotoColorEmoji
import autodev_intellij.mpp_ui.generated.resources.NotoSansSC_Regular
import autodev_intellij.mpp_ui.generated.resources.Res
import cc.unitmesh.agent.Platform
import cc.unitmesh.agent.mcp.McpClientManagerFactory
import cc.unitmesh.agent.mcp.McpConfig
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionManager
import cc.unitmesh.devins.completion.CompletionTriggerType
import cc.unitmesh.devins.editor.EditorCallbacks
import cc.unitmesh.devins.ui.compose.config.ToolConfigDialog
import cc.unitmesh.devins.ui.compose.editor.changes.FileChangeSummary
import cc.unitmesh.devins.ui.compose.editor.completion.CompletionPopup
import cc.unitmesh.devins.ui.compose.editor.completion.CompletionTrigger
import cc.unitmesh.devins.ui.compose.editor.highlighting.DevInSyntaxHighlighter
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.PromptEnhancer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font

/**
 * DevIn 编辑器输入组件
 * 完整的输入界面，包含底部工具栏
 *
 * Model configuration is now managed internally by ModelSelector via ConfigManager.
 */
@Composable
fun DevInEditorInput(
    initialText: String = "",
    placeholder: String = "Type your message...",
    callbacks: EditorCallbacks? = null,
    completionManager: CompletionManager? = null,
    isCompactMode: Boolean = false,
    isExecuting: Boolean = false,
    onStopClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onModelConfigChange: (ModelConfig) -> Unit = {}
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialText)) }
    var highlightedText by remember { mutableStateOf(initialText) }

    // 补全相关状态
    var showCompletion by remember { mutableStateOf(false) }
    var completionItems by remember { mutableStateOf<List<CompletionItem>>(emptyList()) }
    var selectedCompletionIndex by remember { mutableStateOf(0) }
    var currentTriggerType by remember { mutableStateOf(CompletionTriggerType.NONE) }

    // 提示词增强相关状态
    var isEnhancing by remember { mutableStateOf(false) }
    var enhancer by remember { mutableStateOf<Any?>(null) }

    // Tool Configuration 对话框状态
    var showToolConfig by remember { mutableStateOf(false) }
    var mcpServers by remember { mutableStateOf<Map<String, McpServerConfig>>(emptyMap()) }
    val mcpClientManager = remember { McpClientManagerFactory.create() }

    val highlighter = remember { DevInSyntaxHighlighter() }
    val manager = completionManager ?: remember { CompletionManager() }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Initialize MCP client manager with config
    LaunchedEffect(Unit) {
        val configWrapper = ConfigManager.load()
        mcpServers = configWrapper.getMcpServers()
        if (mcpServers.isNotEmpty()) {
            mcpClientManager.initialize(McpConfig(mcpServers = mcpServers))
        }
    }

    var llmService by remember { mutableStateOf<KoogLLMService?>(null) }

    LaunchedEffect(Unit) {
        try {
            val workspace = WorkspaceManager.currentWorkspace
            val projectPath = workspace?.rootPath
            if (projectPath != null) {
                val configWrapper = ConfigManager.load()
                val activeConfig = configWrapper.getActiveModelConfig()
                if (activeConfig != null && activeConfig.isValid()) {
                    llmService = KoogLLMService.create(activeConfig)

                    // Use workspace file system
                    val fileSystem = workspace.fileSystem

                    // Create domain dict service
                    val domainDictService = cc.unitmesh.indexer.DomainDictService(fileSystem)

                    // Create prompt enhancer
                    enhancer = PromptEnhancer(llmService!!, fileSystem, domainDictService)
                }
            }
        } catch (e: Exception) {
            println("Failed to initialize prompt enhancer: ${e.message}")
        }
    }

    // 延迟高亮以避免频繁解析
    LaunchedEffect(textFieldValue.text) {
        delay(50) // 50ms 防抖
        highlightedText = textFieldValue.text
        callbacks?.onTextChanged(textFieldValue.text)
    }

    // 处理文本变化和补全触发
    fun handleTextChange(newValue: TextFieldValue) {
        val oldText = textFieldValue.text
        textFieldValue = newValue

        // 检查是否应该触发补全
        if (newValue.text.length > oldText.length) {
            val addedChar = newValue.text.getOrNull(newValue.selection.start - 1)
            if (addedChar != null && CompletionTrigger.shouldTrigger(addedChar)) {
                val triggerType = CompletionTrigger.getTriggerType(addedChar)
                val context =
                    CompletionTrigger.buildContext(
                        newValue.text,
                        newValue.selection.start,
                        triggerType
                    )

                if (context != null) {
                    currentTriggerType = triggerType

                    // 使用增强的过滤补全功能
                    completionItems = manager.getFilteredCompletions(context)

                    selectedCompletionIndex = 0
                    showCompletion = completionItems.isNotEmpty()
                    println("[Completion] Triggered: char='$addedChar', type=$triggerType, items=${completionItems.size}")
                }
            } else if (showCompletion) {
                // 更新补全列表
                val context =
                    CompletionTrigger.buildContext(
                        newValue.text,
                        newValue.selection.start,
                        currentTriggerType
                    )
                if (context != null) {
                    // 使用增强的过滤补全功能，支持边输入边补全
                    completionItems = manager.getFilteredCompletions(context)
                    selectedCompletionIndex = 0
                    if (completionItems.isEmpty()) {
                        showCompletion = false
                    }
                } else {
                    showCompletion = false
                }
            }
        } else {
            // 文本减少，关闭补全
            if (showCompletion) {
                val context =
                    CompletionTrigger.buildContext(
                        newValue.text,
                        newValue.selection.start,
                        currentTriggerType
                    )
                if (context == null) {
                    showCompletion = false
                }
            }
        }
    }

    fun applyCompletion(item: CompletionItem) {
        val insertHandler = item.insertHandler
        val result =
            if (insertHandler != null) {
                insertHandler(textFieldValue.text, textFieldValue.selection.start)
            } else {
                item.defaultInsert(textFieldValue.text, textFieldValue.selection.start)
            }

        textFieldValue =
            TextFieldValue(
                text = result.newText,
                selection = androidx.compose.ui.text.TextRange(result.newCursorPosition)
            )

        // Check if this is a built-in command that should be auto-executed
        val trimmedText = result.newText.trim()
        if (currentTriggerType == CompletionTriggerType.COMMAND &&
            (trimmedText == "/init" || trimmedText == "/clear" || trimmedText == "/help")
        ) {
            scope.launch {
                delay(100) // Small delay to ensure UI updates
                callbacks?.onSubmit(trimmedText)
                textFieldValue = TextFieldValue("")
                showCompletion = false
            }
            return
        }

        if (result.shouldTriggerNextCompletion) {
            // 延迟触发下一个补全
            scope.launch {
                kotlinx.coroutines.delay(50)
                val lastChar = result.newText.getOrNull(result.newCursorPosition - 1)
                val triggerType =
                    when (lastChar) {
                        ':' -> CompletionTriggerType.COMMAND_VALUE
                        '/' -> CompletionTriggerType.COMMAND
                        else -> null
                    }

                if (triggerType != null) {
                    val context =
                        CompletionTrigger.buildContext(
                            result.newText,
                            result.newCursorPosition,
                            triggerType
                        )
                    if (context != null) {
                        currentTriggerType = triggerType
                        completionItems = manager.getFilteredCompletions(context)
                        selectedCompletionIndex = 0
                        showCompletion = completionItems.isNotEmpty()
                    }
                } else {
                    showCompletion = false
                }
            }
        } else {
            showCompletion = false
        }

        focusRequester.requestFocus()
    }

    // 增强当前输入的函数
    fun enhanceCurrentInput() {
        val currentEnhancer = enhancer
        if (currentEnhancer == null || textFieldValue.text.isBlank() || isEnhancing) {
            return
        }

        scope.launch {
            try {
                isEnhancing = true
                println("[Enhancement] Enhancing current input...")

                val enhanced = (currentEnhancer as PromptEnhancer).enhance(textFieldValue.text.trim(), "zh")

                if (enhanced.isNotEmpty() && enhanced != textFieldValue.text.trim() && enhanced.length > textFieldValue.text.trim().length) {
                    textFieldValue =
                        TextFieldValue(
                            text = enhanced,
                            selection = androidx.compose.ui.text.TextRange(enhanced.length)
                        )
                    println("✨ Enhanced: \"${textFieldValue.text.trim()}\" -> \"$enhanced\"")
                } else {
                    println("ℹ️ No enhancement needed or failed")
                }
            } catch (e: Exception) {
                println("❌ Enhancement failed: ${e.message}")
            } finally {
                isEnhancing = false
            }
        }
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false

        // 移动端：不拦截 Enter 键，让输入法和虚拟键盘正常工作
        // 桌面端：Enter 发送，Shift+Enter 换行
        val isAndroid = Platform.isAndroid

        return when {
            // 补全弹窗显示时的特殊处理
            showCompletion -> {
                when (event.key) {
                    Key.Enter -> {
                        // 应用选中的补全
                        if (completionItems.isNotEmpty()) {
                            applyCompletion(completionItems[selectedCompletionIndex])
                        }
                        true
                    }
                    Key.DirectionDown -> {
                        selectedCompletionIndex = (selectedCompletionIndex + 1) % completionItems.size
                        true
                    }
                    Key.DirectionUp -> {
                        selectedCompletionIndex =
                            if (selectedCompletionIndex > 0) {
                                selectedCompletionIndex - 1
                            } else {
                                completionItems.size - 1
                            }
                        true
                    }
                    Key.Tab -> {
                        if (completionItems.isNotEmpty()) {
                            applyCompletion(completionItems[selectedCompletionIndex])
                        }
                        true
                    }
                    Key.Escape -> {
                        showCompletion = false
                        true
                    }
                    else -> false
                }
            }

            // 桌面端：Enter 发送消息（但不在移动端拦截）
            !isAndroid && event.key == Key.Enter && !event.isShiftPressed -> {
                if (textFieldValue.text.isNotBlank()) {
                    callbacks?.onSubmit(textFieldValue.text)
                    textFieldValue = TextFieldValue("")
                    showCompletion = false
                }
                true
            }

            // Ctrl+P 增强提示词
            event.key == Key.P && event.isCtrlPressed -> {
                enhanceCurrentInput()
                true
            }

            // 其他键不处理，让系统和输入法处理
            else -> false
        }
    }

    val isAndroid = Platform.isAndroid

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // File Change Summary - shown above the editor
        FileChangeSummary()

        Box(
            contentAlignment = if (isAndroid && isCompactMode) Alignment.Center else Alignment.TopStart
        ) {
            Surface(
                modifier =
                    if (isAndroid && isCompactMode) {
                        Modifier.fillMaxWidth() // Android 紧凑模式：full width
                    } else {
                        Modifier.fillMaxWidth()
                    },
                shape = RoundedCornerShape(if (isAndroid && isCompactMode) 12.dp else 4.dp),
                border =
                    androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    ),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp, // 无叠影
                shadowElevation = 0.dp // 无阴影
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min =
                                        if (isCompactMode) {
                                            if (isAndroid) 48.dp else 56.dp
                                        } else {
                                            80.dp
                                        },
                                    max =
                                        if (isCompactMode) {
                                            // 移动端紧凑模式：允许自动扩展到 3 行
                                            if (isAndroid) 120.dp else 96.dp
                                        } else {
                                            160.dp
                                        }
                                )
                                .padding(
                                    if (isCompactMode) {
                                        if (isAndroid) 12.dp else 12.dp
                                    } else {
                                        20.dp
                                    }
                                )
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { handleTextChange(it) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight() // 允许高度自动撑开
                                    .focusRequester(focusRequester)
                                    .onPreviewKeyEvent { handleKeyEvent(it) },
                            textStyle =
                                TextStyle(
                                    fontFamily = if (Platform.isWasm) FontFamily(Font(Res.font.NotoSansSC_Regular)) else FontFamily.Monospace,
                                    fontSize = if (isAndroid && isCompactMode) 16.sp else 15.sp, // 移动端更大
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = if (isAndroid && isCompactMode) 24.sp else 22.sp // 增加行高
                                ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            maxLines = if (isAndroid && isCompactMode) 5 else 8, // 限制最大行数
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                ) {
                                    // 显示带高亮的文本
                                    if (highlightedText.isNotEmpty()) {
                                        Text(
                                            text = highlighter.highlight(highlightedText),
                                            style =
                                                TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = if (isAndroid && isCompactMode) 16.sp else 15.sp,
                                                    lineHeight = if (isAndroid && isCompactMode) 24.sp else 22.sp
                                                ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // 占位符
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = placeholder,
                                            style =
                                                TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = if (isAndroid && isCompactMode) 16.sp else 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    lineHeight = if (isAndroid && isCompactMode) 24.sp else 22.sp
                                                )
                                        )
                                    }

                                    // 实际的输入框（透明）
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                    ) {
                                        innerTextField()
                                    }
                                }
                            }
                        )
                    }

                    // 提示文本
                    if (!isAndroid) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = if (isEnhancing) "Enhancing..." else "Ctrl+P to enhance prompt",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    val currentWorkspace by WorkspaceManager.workspaceFlow.collectAsState()

                    BottomToolbar(
                        onSendClick = {
                            if (textFieldValue.text.isNotBlank()) {
                                callbacks?.onSubmit(textFieldValue.text)
                                textFieldValue = TextFieldValue("")
                                showCompletion = false
                            }
                        },
                        sendEnabled = textFieldValue.text.isNotBlank(),
                        isExecuting = isExecuting,
                        onStopClick = onStopClick,
                        workspacePath = currentWorkspace?.rootPath,
                        onAtClick = {
                            // 插入 @ 并触发 Agent 补全
                            val current = textFieldValue
                            val newText = current.text + "@"
                            val newPosition = current.text.length + 1

                            textFieldValue =
                                TextFieldValue(
                                    text = newText,
                                    selection = androidx.compose.ui.text.TextRange(newPosition)
                                )

                            // 立即触发补全
                            scope.launch {
                                delay(50) // 等待状态更新
                                val context =
                                    CompletionTrigger.buildContext(
                                        newText,
                                        newPosition,
                                        CompletionTriggerType.AGENT
                                    )
                                if (context != null && manager != null) {
                                    currentTriggerType = CompletionTriggerType.AGENT
                                    completionItems = manager.getFilteredCompletions(context)
                                    selectedCompletionIndex = 0
                                    showCompletion = completionItems.isNotEmpty()
                                    println("[Completion] @ trigger: items=${completionItems.size}")
                                }
                            }
                        },
                        onSettingsClick = {
                            showToolConfig = true
                        },
                        onModelConfigChange = onModelConfigChange
                    )
                }
            }

            if (showToolConfig) {
                ToolConfigDialog(
                    onDismiss = { showToolConfig = false },
                    onSave = { toolConfigFile ->
                        scope.launch {
                            mcpServers = toolConfigFile.mcpServers
                        }
                    },
                    llmService = llmService
                )
            }

            if (showCompletion && completionItems.isNotEmpty()) {
                CompletionPopup(
                    items = completionItems,
                    selectedIndex = selectedCompletionIndex,
                    offset = IntOffset(12, if (isCompactMode) 60 else 120),
                    onItemSelected = { item ->
                        applyCompletion(item)
                    },
                    onSelectedIndexChanged = { index ->
                        selectedCompletionIndex = index
                    },
                    onDismiss = {
                        showCompletion = false
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
