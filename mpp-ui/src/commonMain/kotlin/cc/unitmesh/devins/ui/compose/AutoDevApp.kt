package cc.unitmesh.devins.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.filesystem.DefaultFileSystem
import cc.unitmesh.devins.llm.ChatHistoryManager
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.ui.compose.agent.AgentChatInterface
import cc.unitmesh.devins.ui.compose.chat.DebugDialog
import cc.unitmesh.devins.ui.compose.chat.MessageList
import cc.unitmesh.devins.ui.compose.chat.TopBarMenu
import cc.unitmesh.devins.ui.compose.chat.createChatCallbacks
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme
import cc.unitmesh.devins.ui.compose.theme.ThemeManager
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.platform.createFileChooser
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDevApp() {
    val currentTheme = ThemeManager.currentTheme

    // 应用主题
    AutoDevTheme(themeMode = currentTheme) {
        AutoDevContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoDevContent() {
    val scope = rememberCoroutineScope()
    var compilerOutput by remember { mutableStateOf("") }

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var currentStreamingOutput by remember { mutableStateOf("") }
    var isLLMProcessing by remember { mutableStateOf(false) }

    val chatHistoryManager = remember { ChatHistoryManager.getInstance() }

    LaunchedEffect(Unit) {
        messages = chatHistoryManager.getMessages()
    }

    var currentModelConfig by remember { mutableStateOf<ModelConfig?>(null) }
    var llmService by remember { mutableStateOf<KoogLLMService?>(null) }
    var showConfigWarning by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showModelConfigDialog by remember { mutableStateOf(false) }
    var showToolConfigDialog by remember { mutableStateOf(false) }
    var selectedAgent by remember { mutableStateOf("Default") }
    var useAgentMode by remember { mutableStateOf(true) } // New: toggle between chat and agent mode

    val availableAgents = listOf("Default")

    var currentWorkspace by remember { mutableStateOf(WorkspaceManager.getCurrentOrEmpty()) }

    val workspaceState by WorkspaceManager.workspaceFlow.collectAsState()

    LaunchedEffect(workspaceState) {
        workspaceState?.let { workspace ->
            currentWorkspace = workspace
        }
    }

    LaunchedEffect(Unit) {
        if (!WorkspaceManager.hasActiveWorkspace()) {
            val defaultPath = "/Users/phodal/IdeaProjects/untitled"
            val fileSystem = DefaultFileSystem(defaultPath)
            if (fileSystem.exists(defaultPath)) {
                WorkspaceManager.openWorkspace("Default Project", defaultPath)
            } else {
                WorkspaceManager.openEmptyWorkspace("Empty Workspace")
            }
        }
    }

    // Load configuration from file
    LaunchedEffect(Unit) {
        try {
            val wrapper = ConfigManager.load()
            val activeConfig = wrapper.getActiveModelConfig()

            if (activeConfig != null && activeConfig.isValid()) {
                currentModelConfig = activeConfig
                llmService = KoogLLMService.create(activeConfig)
                println("✅ 加载配置: ${activeConfig.provider.displayName} / ${activeConfig.modelName}")
            } else {
                println("⚠️ 未找到有效配置")
            }
        } catch (e: Exception) {
            println("⚠️ 加载配置失败: ${e.message}")
        }
    }

    val callbacks =
        createChatCallbacks(
            fileSystem = currentWorkspace.fileSystem,
            llmService = llmService,
            chatHistoryManager = chatHistoryManager,
            scope = scope,
            onCompilerOutput = { compilerOutput = it },
            onUserMessage = { userMsg ->
                // 添加用户消息到本地状态
                messages = messages + userMsg
            },
            onStreamingOutput = { output ->
                // 更新流式输出
                currentStreamingOutput = output
            },
            onAssistantMessage = { assistantMsg ->
                // AI 响应完成，添加到本地状态
                messages = messages + assistantMsg
                currentStreamingOutput = "" // 清空流式输出
            },
            onProcessingChange = { isLLMProcessing = it },
            onError = {
                errorMessage = it
                showErrorDialog = true
            },
            onConfigWarning = { showConfigWarning = true }
        )

    // 打开目录选择器
    fun openDirectoryChooser() {
        scope.launch {
            val fileChooser = createFileChooser()
            val selectedPath =
                fileChooser.chooseDirectory(
                    title = "Select Project Directory",
                    initialDirectory = currentWorkspace.rootPath
                )

            selectedPath?.let { path ->
                val projectName = path.substringAfterLast('/')
                try {
                    WorkspaceManager.openWorkspace(projectName, path)
                    println("📁 已切换项目路径: $path")
                } catch (e: Exception) {
                    errorMessage = "切换工作空间失败: ${e.message}"
                    showErrorDialog = true
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBarMenu(
                hasHistory = messages.isNotEmpty(),
                hasDebugInfo = compilerOutput.isNotEmpty(),
                currentModelConfig = currentModelConfig,
                selectedAgent = selectedAgent,
                availableAgents = availableAgents,
                useAgentMode = useAgentMode,
                onOpenDirectory = { openDirectoryChooser() },
                onClearHistory = {
                    chatHistoryManager.clearCurrentSession()
                    messages = emptyList()
                    currentStreamingOutput = ""
                    println("🗑️ [SimpleAIChat] 聊天历史已清空")
                },
                onShowDebug = { showDebugDialog = true },
                onModelConfigChange = { config ->
                    currentModelConfig = config
                    if (config.isValid()) {
                        try {
                            llmService = KoogLLMService.create(config)
                            println("✅ 切换模型: ${config.provider.displayName} / ${config.modelName}")
                        } catch (e: Exception) {
                            println("❌ 切换模型失败: ${e.message}")
                        }
                    }
                },
                onAgentChange = { agent ->
                    selectedAgent = agent
                    println("🤖 切换 Agent: $agent")
                },
                onModeToggle = { useAgentMode = !useAgentMode },
                onShowModelConfig = { showModelConfigDialog = true },
                onShowToolConfig = { showToolConfigDialog = true },
                modifier =
                    Modifier
                        .statusBarsPadding() // 添加状态栏边距
            )

            if (useAgentMode) {
                AgentChatInterface(
                    llmService = llmService,
                    onConfigWarning = { showConfigWarning = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val isCompactMode = messages.isNotEmpty() || isLLMProcessing

                if (isCompactMode) {
                    MessageList(
                        messages = messages,
                        isLLMProcessing = isLLMProcessing,
                        currentOutput = currentStreamingOutput,
                        projectPath = currentWorkspace.rootPath,
                        fileSystem = currentWorkspace.fileSystem,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                    )

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .imePadding()
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp) // 外部边距
                    ) {
                        DevInEditorInput(
                            initialText = "",
                            placeholder = "Type your message...",
                            callbacks = callbacks,
                            completionManager = currentWorkspace.completionManager,
                            isCompactMode = true,
                            onModelConfigChange = { config ->
                                currentModelConfig = config
                                if (config.isValid()) {
                                    try {
                                        llmService = KoogLLMService.create(config)
                                        println("✅ 切换模型: ${config.provider.displayName} / ${config.modelName}")
                                    } catch (e: Exception) {
                                        println("❌ 切换模型失败: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    val isAndroid = Platform.isAndroid
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .imePadding()
                                .padding(if (isAndroid) 16.dp else 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DevInEditorInput(
                            initialText = "",
                            placeholder = "Type your message...",
                            callbacks = callbacks,
                            completionManager = currentWorkspace.completionManager,
                            onModelConfigChange = { config ->
                                currentModelConfig = config
                                if (config.isValid()) {
                                    try {
                                        llmService = KoogLLMService.create(config)
                                        println("✅ 切换模型: ${config.provider.displayName} / ${config.modelName}")
                                    } catch (e: Exception) {
                                        println("❌ 配置 LLM 服务失败: ${e.message}")
                                        llmService = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(if (isAndroid) 1f else 0.9f)
                        )
                    }
                }
            }
        }
    }

    // Model Config Dialog
    if (showModelConfigDialog) {
        cc.unitmesh.devins.ui.compose.editor.ModelConfigDialog(
            currentConfig = currentModelConfig ?: ModelConfig(),
            onDismiss = { showModelConfigDialog = false },
            onSave = { newConfig ->
                currentModelConfig = newConfig
                if (newConfig.isValid()) {
                    try {
                        llmService = KoogLLMService.create(newConfig)
                        println("✅ 模型配置已保存")
                    } catch (e: Exception) {
                        println("❌ 配置 LLM 服务失败: ${e.message}")
                        llmService = null
                    }
                }
                showModelConfigDialog = false
            }
        )
    }

    // Tool Config Dialog
    if (showToolConfigDialog) {
        cc.unitmesh.devins.ui.compose.config.ToolConfigDialog(
            onDismiss = { showToolConfigDialog = false },
            onSave = { newConfig ->
                println("✅ 工具配置已保存")
                println("   启用的内置工具: ${newConfig.enabledBuiltinTools.size}")
                println("   启用的 MCP 工具: ${newConfig.enabledMcpTools.size}")
                showToolConfigDialog = false
            }
        )
    }

    if (showDebugDialog) {
        DebugDialog(
            compilerOutput = compilerOutput,
            onDismiss = { showDebugDialog = false }
        )
    }

    if (showConfigWarning) {
        AlertDialog(
            onDismissRequest = { showConfigWarning = false },
            title = {
                Text("⚠️ 未配置 LLM 模型")
            },
            text = {
                Column {
                    Text("请先配置 LLM 模型才能使用 AI 功能。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击右下角的模型选择器进行配置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showConfigWarning = false }) {
                    Text("知道了")
                }
            }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Text("❌ LLM API 错误")
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "调用 LLM API 时发生错误：",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 错误信息卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                    ) {
                        SelectionContainer {
                            Text(
                                text = errorMessage,
                                style =
                                    MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 常见问题提示
                    Text(
                        "常见解决方法：",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• 检查 API Key 是否正确\n" +
                            "• 确认账户余额充足\n" +
                            "• 检查网络连接\n" +
                            "• 验证模型名称是否正确",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("关闭")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showErrorDialog = false
                        // 打开模型配置
                    }
                ) {
                    Text("重新配置")
                }
            }
        )
    }
}
