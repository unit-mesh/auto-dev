package cc.unitmesh.devins.ui.compose.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.completion.CompletionItem
import cc.unitmesh.devins.completion.CompletionTriggerType
import cc.unitmesh.devins.editor.EditorCallbacks
import cc.unitmesh.devins.completion.CompletionManager
import cc.unitmesh.devins.ui.compose.editor.completion.CompletionPopup
import cc.unitmesh.devins.ui.compose.editor.completion.CompletionTrigger
import cc.unitmesh.devins.ui.compose.editor.highlighting.DevInSyntaxHighlighter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * DevIn 编辑器输入组件
 * 完整的输入界面，包含底部工具栏
 */
@Composable
fun DevInEditorInput(
    initialText: String = "",
    placeholder: String = "Plan, @ for context, / for commands",
    callbacks: EditorCallbacks? = null,
    completionManager: CompletionManager? = null,
    initialModelConfig: cc.unitmesh.devins.llm.ModelConfig? = null,
    availableConfigs: List<cc.unitmesh.devins.llm.ModelConfig> = emptyList(),
    onModelConfigChange: (cc.unitmesh.devins.llm.ModelConfig) -> Unit = {},
    isCompactMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialText)) }
    var highlightedText by remember { mutableStateOf(initialText) }
    
    // 补全相关状态
    var showCompletion by remember { mutableStateOf(false) }
    var completionItems by remember { mutableStateOf<List<CompletionItem>>(emptyList()) }
    var selectedCompletionIndex by remember { mutableStateOf(0) }
    var currentTriggerType by remember { mutableStateOf(CompletionTriggerType.NONE) }

    val highlighter = remember { DevInSyntaxHighlighter() }
    val manager = completionManager ?: remember { CompletionManager() }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    
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
                val context = CompletionTrigger.buildContext(
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
                    println("🔍 补全触发: char='$addedChar', type=$triggerType, items=${completionItems.size}")
                }
            } else if (showCompletion) {
                // 更新补全列表
                val context = CompletionTrigger.buildContext(
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
                val context = CompletionTrigger.buildContext(
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



    // 应用补全
    fun applyCompletion(item: CompletionItem) {
        val insertHandler = item.insertHandler
        if (insertHandler != null) {
            val result = insertHandler(textFieldValue.text, textFieldValue.selection.start)
            textFieldValue = TextFieldValue(
                text = result.newText,
                selection = androidx.compose.ui.text.TextRange(result.newCursorPosition)
            )
            
            if (result.shouldTriggerNextCompletion) {
                // 延迟触发下一个补全
                scope.launch {
                    delay(50)
                    val lastChar = result.newText.getOrNull(result.newCursorPosition - 1)
                    if (lastChar == ':') {
                        val triggerType = CompletionTriggerType.COMMAND_VALUE
                        val context = CompletionTrigger.buildContext(
                            result.newText,
                            result.newCursorPosition,
                            triggerType
                        )
                        if (context != null) {
                            currentTriggerType = triggerType
                            completionItems = manager.getCompletions(context)
                            selectedCompletionIndex = 0
                            showCompletion = completionItems.isNotEmpty()
                        }
                    }
                }
            }
        }
        showCompletion = false
    }
    
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        
        return when {
            event.key == Key.Enter && !event.isShiftPressed -> {
                if (showCompletion) {
                    if (completionItems.isNotEmpty()) {
                        applyCompletion(completionItems[selectedCompletionIndex])
                    }
                    true
                } else {
                    if (textFieldValue.text.isNotBlank()) {
                        callbacks?.onSubmit(textFieldValue.text)
                        textFieldValue = TextFieldValue("")
                        showCompletion = false
                    }
                    true
                }
            }
            
            event.key == Key.Enter && event.isShiftPressed -> {
                false
            }
            
            showCompletion -> {
                when (event.key) {
                    Key.DirectionDown -> {
                        selectedCompletionIndex = (selectedCompletionIndex + 1) % completionItems.size
                        true
                    }
                    Key.DirectionUp -> {
                        selectedCompletionIndex = if (selectedCompletionIndex > 0) {
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
            // 其他键不处理
            else -> false
        }
    }
    
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 编辑器区域 - 根据模式调整高度
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = if (isCompactMode) 48.dp else 72.dp,
                            max = if (isCompactMode) 48.dp else 120.dp
                        )
                        .padding(if (isCompactMode) 8.dp else 16.dp)
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { handleTextChange(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { handleKeyEvent(it) },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box {
                                // 显示带高亮的文本
                                if (highlightedText.isNotEmpty()) {
                                    Text(
                                        text = highlighter.highlight(highlightedText),
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp
                                        ),
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                                
                                // 占位符
                                if (textFieldValue.text.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                                
                                // 实际的输入框（透明）
                                Box(modifier = Modifier.matchParentSize()) {
                                    innerTextField()
                                }
                            }
                        }
                    )
                }
                
                // 底部工具栏
                HorizontalDivider()
                BottomToolbar(
                    onSendClick = {
                        callbacks?.onSubmit(textFieldValue.text)
                    },
                    onAtClick = {
                        // 插入 @ 并触发补全
                        val current = textFieldValue
                        textFieldValue = TextFieldValue(
                            text = current.text + "@",
                            selection = androidx.compose.ui.text.TextRange(current.text.length + 1)
                        )
                    },
                    onSlashClick = {
                        // 插入 / 并触发补全
                        val current = textFieldValue
                        textFieldValue = TextFieldValue(
                            text = current.text + "/",
                            selection = androidx.compose.ui.text.TextRange(current.text.length + 1)
                        )
                    },
                    sendEnabled = textFieldValue.text.isNotBlank(),
                    initialModelConfig = initialModelConfig,
                    availableConfigs = availableConfigs,
                    onModelConfigChange = onModelConfigChange
                )
            }
        }
        
        // 补全弹窗
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
    
    // 自动聚焦
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

