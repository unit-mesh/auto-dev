package cc.unitmesh.devins.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.completion.CompletionManager
import cc.unitmesh.devins.llm.ChatHistoryManager
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.ui.app.AppleNavLayout
import cc.unitmesh.devins.ui.app.AppScreen
import cc.unitmesh.devins.ui.compose.agent.AgentChatInterface
import cc.unitmesh.devins.ui.compose.agent.AgentInterfaceRouter
import cc.unitmesh.devins.ui.compose.chat.createChatCallbacks
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.ui.compose.editor.ModelConfigDialog
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme
import cc.unitmesh.devins.ui.compose.theme.ThemeManager
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.session.SessionViewModel
import cc.unitmesh.devins.ui.session.SessionClient
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.launch

/**
 * Apple (iOS/macOS) implementation of PlatformAutoDevApp
 * 
 * Features:
 * - TabBar navigation (iOS style)
 * - Multi-agent support (Chat, Coding, Code Review, Remote)
 * - Settings sheet
 * - Material 3 design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun PlatformAutoDevApp(
    triggerFileChooser: Boolean,
    onFileChooserHandled: () -> Unit,
    initialMode: String
) {
    val currentTheme = ThemeManager.currentTheme

    AutoDevTheme(themeMode = currentTheme) {
        AppleAutoDevContent(
            triggerFileChooser = triggerFileChooser,
            onFileChooserHandled = onFileChooserHandled,
            initialMode = initialMode
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppleAutoDevContent(
    triggerFileChooser: Boolean = false,
    onFileChooserHandled: () -> Unit = {},
    initialMode: String = "auto"
) {
    val scope = rememberCoroutineScope()

    // Chat 状态
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var currentStreamingOutput by remember { mutableStateOf("") }
    var isLLMProcessing by remember { mutableStateOf(false) }
    val chatHistoryManager = remember { ChatHistoryManager.getInstance() }

    // 配置状态
    var currentModelConfig by remember { mutableStateOf<ModelConfig?>(null) }
    var llmService by remember { mutableStateOf<KoogLLMService?>(null) }
    var compilerOutput by remember { mutableStateOf("") }

    // Dialog 状态
    var showModelConfigDialog by remember { mutableStateOf(false) }
    var showToolConfigDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 导航状态 - 默认从 Chat 开始
    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }

    // Session 管理
    val sessionClient = remember { SessionClient("http://localhost:8080") }
    val sessionViewModel = remember { SessionViewModel(sessionClient) }

    // Agent 模式状态
    var selectedAgentType by remember { mutableStateOf(AgentType.LOCAL_CHAT) }
    var useAgentMode by remember { mutableStateOf(true) }
    var isTreeViewVisible by remember { mutableStateOf(false) }

    // 工作空间
    var currentWorkspace by remember { mutableStateOf(WorkspaceManager.getCurrentOrEmpty()) }
    val workspaceState by WorkspaceManager.workspaceFlow.collectAsState()

    // 初始化
    LaunchedEffect(Unit) {
        chatHistoryManager.initialize()
        messages = chatHistoryManager.getMessages()

        // 加载配置
        try {
            val wrapper = ConfigManager.load()
            val activeConfig = wrapper.getActiveModelConfig()

            if (activeConfig != null && activeConfig.isValid()) {
                currentModelConfig = activeConfig
                llmService = KoogLLMService.create(activeConfig)
            }

            selectedAgentType = wrapper.getAgentType()
        } catch (e: Exception) {
            println("⚠️ 加载配置失败: ${e.message}")
        }
    }

    LaunchedEffect(workspaceState) {
        workspaceState?.let { workspace ->
            currentWorkspace = workspace
        }
    }

    // 根据屏幕切换更新 Agent 类型
    LaunchedEffect(currentScreen) {
        val agentType = when (currentScreen) {
            AppScreen.CHAT -> AgentType.LOCAL_CHAT
            AppScreen.CODING -> AgentType.CODING
            AppScreen.CODE_REVIEW -> AgentType.CODE_REVIEW
            AppScreen.REMOTE -> AgentType.REMOTE
            else -> null
        }
        agentType?.let { selectedAgentType = it }
    }

    // Chat callbacks
    val callbacks: cc.unitmesh.devins.editor.EditorCallbacks = createChatCallbacks(
        fileSystem = currentWorkspace.fileSystem,
        llmService = llmService,
        chatHistoryManager = chatHistoryManager,
        scope = scope,
        onCompilerOutput = { compilerOutput = it },
        onUserMessage = { userMsg -> messages = messages + userMsg },
        onStreamingOutput = { output -> currentStreamingOutput = output },
        onAssistantMessage = { assistantMsg ->
            messages = messages + assistantMsg
            currentStreamingOutput = ""
        },
        onProcessingChange = { isProcessing -> isLLMProcessing = isProcessing },
        onError = { error ->
            errorMessage = error
            showErrorDialog = true
        },
        onConfigWarning = {
            showModelConfigDialog = true
        }
    )

    // 主界面布局
    AppleNavLayout(
        currentScreen = currentScreen,
        onScreenChange = { newScreen ->
            currentScreen = newScreen
        },
        sessionViewModel = sessionViewModel,
        onShowSettings = {
            showModelConfigDialog = true
        },
        onShowTools = {
            showToolConfigDialog = true
        },
        actions = {
            when (currentScreen) {
                AppScreen.CHAT, AppScreen.CODING, AppScreen.CODE_REVIEW, AppScreen.REMOTE -> {
                    IconButton(onClick = { useAgentMode = !useAgentMode }) {
                        Icon(
                            imageVector = if (useAgentMode) Icons.Default.SmartToy else Icons.Filled.Chat,
                            contentDescription = if (useAgentMode) "Agent 模式" else "Chat 模式"
                        )
                    }
                    if (useAgentMode) {
                        IconButton(onClick = { isTreeViewVisible = !isTreeViewVisible }) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "文件树"
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                AppScreen.CHAT, AppScreen.CODING, AppScreen.CODE_REVIEW, AppScreen.REMOTE -> {
                    // 统一通过 AgentInterfaceRouter 路由
                    AgentInterfaceRouter(
                        llmService = llmService,
                        isTreeViewVisible = isTreeViewVisible,
                        onConfigWarning = { showModelConfigDialog = true },
                        onToggleTreeView = { isTreeViewVisible = it },
                        chatHistoryManager = chatHistoryManager,
                        selectedAgentType = selectedAgentType,
                        onAgentTypeChange = { type -> selectedAgentType = type },
                        hasHistory = messages.isNotEmpty(),
                        hasDebugInfo = compilerOutput.isNotEmpty(),
                        currentModelConfig = currentModelConfig,
                        selectedAgent = "Default",
                        availableAgents = listOf("Default"),
                        useAgentMode = useAgentMode,
                        onOpenDirectory = {},
                        onClearHistory = {
                            chatHistoryManager.clearCurrentSession()
                            messages = emptyList()
                            currentStreamingOutput = ""
                        },
                        onModelConfigChange = { config ->
                            currentModelConfig = config
                            if (config.isValid()) {
                                try {
                                    llmService = KoogLLMService.create(config)
                                } catch (e: Exception) {
                                    println("❌ 切换模型失败: ${e.message}")
                                }
                            }
                        },
                        onAgentChange = {},
                        onModeToggle = { useAgentMode = !useAgentMode },
                        onConfigureRemote = {},
                        onShowModelConfig = { showModelConfigDialog = true },
                        onShowToolConfig = { showToolConfigDialog = true },
                        showTopBar = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                AppScreen.PROFILE -> {
                    // 个人中心
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text("个人中心", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("工作空间: ${currentWorkspace.rootPath ?: "未设置"}")
                        Text("模型: ${currentModelConfig?.modelName ?: "未配置"}")
                    }
                }
                
                else -> {
                    // 占位界面
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text("${currentScreen.name} 功能开发中...")
                    }
                }
            }
        }
    }

    // 配置对话框
    if (showModelConfigDialog) {
        ModelConfigDialog(
            currentConfig = currentModelConfig ?: ModelConfig(),
            currentConfigName = null,
            onDismiss = { showModelConfigDialog = false },
            onSave = { configName, newConfig ->
                currentModelConfig = newConfig
                if (newConfig.isValid()) {
                    try {
                        scope.launch {
                            val namedConfig = cc.unitmesh.llm.NamedModelConfig(
                                name = configName,
                                provider = newConfig.provider.name,
                                apiKey = newConfig.apiKey,
                                model = newConfig.modelName,
                                baseUrl = newConfig.baseUrl,
                                temperature = newConfig.temperature,
                                maxTokens = newConfig.maxTokens
                            )
                            ConfigManager.saveConfig(namedConfig, setActive = true)
                        }

                        llmService = KoogLLMService.create(newConfig)
                    } catch (e: Exception) {
                        println("❌ 配置 LLM 服务失败: ${e.message}")
                    }
                }
                showModelConfigDialog = false
            }
        )
    }

    // 工具配置对话框（与 Android 保持一致）
    if (showToolConfigDialog) {
        cc.unitmesh.devins.ui.compose.config.ToolConfigDialog(
            onDismiss = { showToolConfigDialog = false },
            onSave = { _ ->
                println("✅ 工具配置已保存")
                showToolConfigDialog = false
            },
            llmService = llmService
        )
    }

    // 错误对话框
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("错误") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

