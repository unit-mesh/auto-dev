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
import cc.unitmesh.devins.ui.app.UnifiedAppContent
import cc.unitmesh.devins.ui.compose.agent.AgentInterfaceRouter
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.ui.compose.chat.MessageList
import cc.unitmesh.devins.ui.compose.chat.SessionSidebar
import cc.unitmesh.devins.ui.compose.chat.TopBarMenu
import cc.unitmesh.devins.ui.compose.chat.createChatCallbacks
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme
import cc.unitmesh.devins.ui.compose.theme.ThemeManager
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.i18n.Strings
import cc.unitmesh.devins.ui.platform.createFileChooser
import cc.unitmesh.devins.ui.state.UIStateManager
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDevApp(
    triggerFileChooser: Boolean = false,
    onFileChooserHandled: () -> Unit = {},
    initialMode: String = "auto",
    showTopBarInContent: Boolean = true,
    initialAgentType: AgentType = AgentType.CODING,
    initialTreeViewVisible: Boolean = false,
    onAgentTypeChanged: (AgentType) -> Unit = {},
    onTreeViewVisibilityChanged: (Boolean) -> Unit = {},
    onSidebarVisibilityChanged: (Boolean) -> Unit = {},
    onWorkspacePathChanged: (String) -> Unit = {},
    onHasHistoryChanged: (Boolean) -> Unit = {},
    onNotification: (String, String) -> Unit = { _, _ -> }
) {
    val currentTheme = ThemeManager.currentTheme

    AutoDevTheme(themeMode = currentTheme) {
        AutoDevContent(
            triggerFileChooser = triggerFileChooser,
            onFileChooserHandled = onFileChooserHandled,
            initialMode = initialMode,
            showTopBarInContent = showTopBarInContent,
            initialAgentType = initialAgentType,
            initialTreeViewVisible = initialTreeViewVisible,
            onAgentTypeChanged = onAgentTypeChanged,
            onTreeViewVisibilityChanged = onTreeViewVisibilityChanged,
            onSidebarVisibilityChanged = onSidebarVisibilityChanged,
            onWorkspacePathChanged = onWorkspacePathChanged,
            onHasHistoryChanged = onHasHistoryChanged,
            onNotification = onNotification
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoDevContent(
    triggerFileChooser: Boolean = false,
    onFileChooserHandled: () -> Unit = {},
    initialMode: String = "auto",
    showTopBarInContent: Boolean = true,
    initialAgentType: AgentType = AgentType.CODING,
    initialTreeViewVisible: Boolean = false,
    onAgentTypeChanged: (AgentType) -> Unit = {},
    onTreeViewVisibilityChanged: (Boolean) -> Unit = {},
    onSidebarVisibilityChanged: (Boolean) -> Unit = {},
    onWorkspacePathChanged: (String) -> Unit = {},
    onHasHistoryChanged: (Boolean) -> Unit = {},
    onNotification: (String, String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    var compilerOutput by remember { mutableStateOf("") }

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var currentStreamingOutput by remember { mutableStateOf("") }
    var isLLMProcessing by remember { mutableStateOf(false) }

    // Wasm Git Clone dialog state
    var showWasmGitDialog by remember { mutableStateOf(false) }

    val chatHistoryManager = remember { ChatHistoryManager.getInstance() }

    // 从全局状态管理器获取 UI 状态
    val isTreeViewVisible by UIStateManager.isTreeViewVisible.collectAsState()
    val showSessionSidebar by UIStateManager.isSessionSidebarVisible.collectAsState()

    LaunchedEffect(Unit) {
        chatHistoryManager.initialize()
        messages = chatHistoryManager.getMessages()
    }

    var currentModelConfig by remember { mutableStateOf<ModelConfig?>(null) }
    var llmService by remember { mutableStateOf<KoogLLMService?>(null) }
    var showConfigWarning by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showModelConfigDialog by remember { mutableStateOf(false) }
    var showToolConfigDialog by remember { mutableStateOf(false) }
    var selectedAgent by remember { mutableStateOf("Default") }

    // Unified Agent Type Selection (LOCAL, CODING, CODE_REVIEW, REMOTE)
    // Desktop: 由 Main.kt 管理，通过 initialAgentType 传递
    // Mobile/Web: 在此组件内部管理
    var selectedAgentType by remember { mutableStateOf(initialAgentType) }

    // Desktop: 监听 initialAgentType 的变化（从 Main.kt 的标题栏点击事件触发）
    LaunchedEffect(initialAgentType) {
        if (!showTopBarInContent) { // 仅在 Desktop 模式下同步
            selectedAgentType = initialAgentType
        }
    }

    // Remote Agent state
    var serverUrl by remember { mutableStateOf("http://localhost:8080") }
    var useServerConfig by remember { mutableStateOf(false) }
    var showRemoteConfigDialog by remember { mutableStateOf(false) }
    var remoteGitUrl by remember { mutableStateOf("") }
    var remoteProjectId by remember { mutableStateOf("") }

    // Session Management mode (for Remote Session UI)
    var useSessionManagement by remember { mutableStateOf(false) }

    // Agent 模式的会话处理器（用于连接 SessionSidebar 和 AgentChatInterface）
    var agentSessionSelectedHandler by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var agentNewChatHandler by remember { mutableStateOf<(() -> Unit)?>(null) }

    val availableAgents = listOf("Default")

    var currentWorkspace by remember { mutableStateOf(WorkspaceManager.getCurrentOrEmpty()) }

    val workspaceState by WorkspaceManager.workspaceFlow.collectAsState()

    fun handleAgentTypeChange(type: AgentType) {
        // Check remote configuration if switching to remote mode
        if (type == AgentType.REMOTE) {
            val hasValidServerConfig = serverUrl.isNotBlank() && serverUrl != "http://localhost:8080"
            if (!hasValidServerConfig) {
                showRemoteConfigDialog = true
                return
            }
        }

        selectedAgentType = type
        onAgentTypeChanged(type)
        scope.launch {
            try {
                // Save as string for config compatibility
                val typeString = when (type) {
                    AgentType.REMOTE -> "Remote"
                    AgentType.LOCAL_CHAT -> "Local"
                    else -> "Local" // CODING and CODE_REVIEW are local modes
                }
                cc.unitmesh.devins.ui.config.saveAgentTypePreference(typeString)
            } catch (e: Exception) {
                println("⚠️ 保存 Agent 类型失败: ${e.message}")
            }
        }
    }

    LaunchedEffect(workspaceState) {
        workspaceState?.let { workspace ->
            currentWorkspace = workspace
            workspace.rootPath?.let { path ->
                UIStateManager.setWorkspacePath(path)
                onWorkspacePathChanged(path)
            }
        }
    }

    // 同步全局状态到回调（供 Desktop 窗口使用）

    // 同步全局状态到回调（供 Desktop 窗口使用）
    LaunchedEffect(showSessionSidebar) {
        onSidebarVisibilityChanged(showSessionSidebar)
    }

    LaunchedEffect(messages.size) {
        val hasHistory = messages.isNotEmpty()
        UIStateManager.setHasHistory(hasHistory)
        onHasHistoryChanged(hasHistory)
    }

    LaunchedEffect(isTreeViewVisible) {
        onTreeViewVisibilityChanged(isTreeViewVisible)
    }

    LaunchedEffect(Unit) {
        // 初始化全局 UI 状态
        UIStateManager.setTreeViewVisible(initialTreeViewVisible)
        UIStateManager.setSessionSidebarVisible(true)

        if (!WorkspaceManager.hasActiveWorkspace()) {
            // Try to load last workspace first
            val lastWorkspace = try {
                ConfigManager.getLastWorkspace()
            } catch (e: Exception) {
                println("⚠️ 加载上次工作空间失败: ${e.message}")
                null
            }

            if (lastWorkspace != null) {
                val fileSystem = DefaultFileSystem(lastWorkspace.path)
                if (fileSystem.exists(lastWorkspace.path)) {
                    println("✅ 加载上次工作空间: ${lastWorkspace.name} (${lastWorkspace.path})")
                    WorkspaceManager.openWorkspace(lastWorkspace.name, lastWorkspace.path)
                } else {
                    println("⚠️ 上次工作空间不存在: ${lastWorkspace.path}")
                    // Fall through to default workspace logic
                }
            }

            // If last workspace not available or doesn't exist, use default
            if (!WorkspaceManager.hasActiveWorkspace()) {
                val defaultPath = when {
                    Platform.isAndroid -> "/storage/emulated/0/Documents"
                    Platform.isJs -> "."
                    else -> "${Platform.getUserHomeDir()}/AutoDevProjects"
                }

                val fileSystem = DefaultFileSystem(defaultPath)

                if (fileSystem.exists(defaultPath)) {
                    WorkspaceManager.openWorkspace("Default Workspace", defaultPath)
                } else {
                    when {
                        Platform.isAndroid -> {
                            val fallbackPath = "/sdcard"
                            println("⚠️ Documents 目录不存在，使用备用路径: $fallbackPath")
                            WorkspaceManager.openWorkspace("Default Workspace", fallbackPath)
                        }

                        Platform.isJs -> {
                            println("⚠️ 使用当前工作目录")
                            WorkspaceManager.openWorkspace("Current Directory", ".")
                        }

                        else -> {
                            try {
                                fileSystem.createDirectory(defaultPath)
                                println("✅ 创建默认工作空间目录: $defaultPath")
                                WorkspaceManager.openWorkspace("Default Workspace", defaultPath)
                            } catch (e: Exception) {
                                println("⚠️ 无法创建默认目录，使用用户主目录")
                                val homeDir = Platform.getUserHomeDir()
                                WorkspaceManager.openWorkspace("Home Directory", homeDir)
                            }
                        }
                    }
                }
            }
        } else {
            println("✅ 已有活动工作空间: ${WorkspaceManager.currentWorkspace?.rootPath}")
        }
    }

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
                if (!Platform.isWasm) {
                    showConfigWarning = true
                }
            }

            selectedAgentType = when (initialMode) {
                "remote", "session" -> AgentType.REMOTE
                "local" -> AgentType.LOCAL_CHAT
                else -> wrapper.getAgentType()
            }

            useSessionManagement = (initialMode == "session")

            val remoteConfig = wrapper.getRemoteServer()
            serverUrl = remoteConfig.url
            useServerConfig = remoteConfig.useServerConfig
        } catch (e: Exception) {
            e.printStackTrace()
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
                messages = messages + userMsg
            },
            onStreamingOutput = { output ->
                currentStreamingOutput = output
            },
            onAssistantMessage = { assistantMsg ->
                messages = messages + assistantMsg
                currentStreamingOutput = ""
            },
            onProcessingChange = { isLLMProcessing = it },
            onError = {
                errorMessage = it
                showErrorDialog = true
            },
            onConfigWarning = { showModelConfigDialog = true }
        )

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

                    // Save the last workspace to config
                    try {
                        ConfigManager.saveLastWorkspace(projectName, path)
                        println("✅ 已保存工作空间到配置")
                    } catch (e: Exception) {
                        println("⚠️ 保存工作空间配置失败: ${e.message}")
                    }
                } catch (e: Exception) {
                    errorMessage = "切换工作空间失败: ${e.message}"
                    showErrorDialog = true
                }
            }
        }
    }

    LaunchedEffect(triggerFileChooser) {
        if (triggerFileChooser) {
            openDirectoryChooser()
            onFileChooserHandled()
        }
    }

    if (useSessionManagement || Platform.isAndroid) {
        UnifiedAppContent(
            serverUrl = serverUrl,
            onOpenLocalChat = if (Platform.isJvm) {
                {
                    useSessionManagement = false
                    selectedAgentType = AgentType.LOCAL_CHAT
                }
            } else null
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Always show sidebar, but control its expanded state
            SessionSidebar(
                chatHistoryManager = chatHistoryManager,
                currentSessionId = chatHistoryManager.getCurrentSession().id,
                isExpanded = showSessionSidebar,
                onSessionSelected = { sessionId ->
                    if (agentSessionSelectedHandler != null) {
                        agentSessionSelectedHandler?.invoke(sessionId)
                    } else {
                        chatHistoryManager.switchSession(sessionId)
                        messages = chatHistoryManager.getMessages()
                        currentStreamingOutput = ""
                    }
                },
                onNewChat = {
                    if (agentNewChatHandler != null) {
                        agentNewChatHandler?.invoke()
                    } else {
                        chatHistoryManager.createSession()
                        messages = emptyList()
                        currentStreamingOutput = ""
                    }
                },
                onRenameSession = { sessionId, newTitle ->
                    chatHistoryManager.renameSession(sessionId, newTitle)
                },
                onNavigateToDocuments = {
                    handleAgentTypeChange(AgentType.DOCUMENT_READER)
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val shouldShowTopBar = !showTopBarInContent

                if (shouldShowTopBar) {
                    TopBarMenu(
                        hasHistory = messages.isNotEmpty(),
                        hasDebugInfo = compilerOutput.isNotEmpty(),
                        currentModelConfig = currentModelConfig,
                        selectedAgent = selectedAgent,
                        availableAgents = availableAgents,
                        isTreeViewVisible = isTreeViewVisible,
                        currentAgentType = selectedAgentType,
                        onAgentTypeChange = { type ->
                            handleAgentTypeChange(type)
                        },
                        useSessionManagement = useSessionManagement,
                        showSessionSidebar = showSessionSidebar,
                        onToggleSidebar = { UIStateManager.toggleSessionSidebar() },
                        onOpenDirectory = { openDirectoryChooser() },
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
                                    println("✅ 切换模型: ${config.provider.displayName} / ${config.modelName}")
                                } catch (e: Exception) {
                                    println("❌ 切换模型失败: ${e.message}")
                                }
                            }
                        },
                        onAgentChange = { agent ->
                            selectedAgent = agent
                        },
                        onToggleTreeView = { UIStateManager.toggleTreeView() },
                        onConfigureRemote = { showRemoteConfigDialog = true },
                        onSessionManagementToggle = {
                            useSessionManagement = !useSessionManagement
                            println("🔄 切换 Session Management: $useSessionManagement")
                        },
                        onShowModelConfig = { showModelConfigDialog = true },
                        onShowToolConfig = { showToolConfigDialog = true },
                        onShowGitClone = {
                            showWasmGitDialog = true
                        },
                        modifier =
                            Modifier
                                .statusBarsPadding() // 添加状态栏边距
                    )
                }

                val isAndroid = Platform.isAndroid
                if (isAndroid) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .imePadding()
                                .padding(16.dp),
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
                } else {
                    AgentInterfaceRouter(
                        llmService = llmService,
                        isTreeViewVisible = isTreeViewVisible,
                        onConfigWarning = { showModelConfigDialog = true },
                        onToggleTreeView = { /* 不需要，由全局状态管理 */ },
                        chatHistoryManager = chatHistoryManager,
                        selectedAgentType = selectedAgentType,
                        onAgentTypeChange = { type ->
                            handleAgentTypeChange(type)
                        },
                        onSessionSelected = { sessionId ->
                            messages = chatHistoryManager.getMessages()
                            currentStreamingOutput = ""
                        },
                        onNewChat = {
                            messages = emptyList()
                            currentStreamingOutput = ""
                        },
                        onInternalSessionSelected = { handler ->
                            agentSessionSelectedHandler = handler
                        },
                        onInternalNewChat = { handler ->
                            agentNewChatHandler = handler
                        },
                        hasHistory = messages.isNotEmpty(),
                        hasDebugInfo = compilerOutput.isNotEmpty(),
                        currentModelConfig = currentModelConfig,
                        selectedAgent = selectedAgent,
                        availableAgents = availableAgents,
                        onOpenDirectory = { openDirectoryChooser() },
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
                                    println("✅ 切换模型: ${config.provider.displayName} / ${config.modelName}")
                                } catch (e: Exception) {
                                    println("❌ 切换模型失败: ${e.message}")
                                }
                            }
                        },
                        onAgentChange = { agent ->
                            selectedAgent = agent
                        },
                        onConfigureRemote = { showRemoteConfigDialog = true },
                        onShowModelConfig = { showModelConfigDialog = true },
                        onShowToolConfig = { showToolConfigDialog = true },
                        serverUrl = serverUrl,
                        useServerConfig = useServerConfig,
                        projectId = remoteProjectId,
                        gitUrl = remoteGitUrl,
                        onProjectChange = { projectId ->
                            remoteProjectId = projectId
                            println("📁 Project ID: $projectId")
                        },
                        onGitUrlChange = { url ->
                            remoteGitUrl = url
                            println("📦 Git URL: $url")
                        },
                        onNotification = onNotification,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (showModelConfigDialog) {
            cc.unitmesh.devins.ui.compose.editor.ModelConfigDialog(
                currentConfig = currentModelConfig ?: ModelConfig(),
                currentConfigName = null, // Will prompt for new name
                onDismiss = { showModelConfigDialog = false },
                onSave = { configName, newConfig ->
                    currentModelConfig = newConfig
                    if (newConfig.isValid()) {
                        try {
                            // 保存配置到文件
                            scope.launch {
                                try {
                                    // 创建 NamedModelConfig 对象以便保存
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
                                    println("✅ 模型配置已保存到磁盘: $configName")
                                } catch (e: Exception) {
                                    println("⚠️ 保存配置到磁盘失败: ${e.message}")
                                }
                            }

                            llmService = KoogLLMService.create(newConfig)
                            println("✅ 模型配置已应用: $configName")
                        } catch (e: Exception) {
                            println("❌ 配置 LLM 服务失败: ${e.message}")
                            llmService = null
                        }
                    }
                    showModelConfigDialog = false
                }
            )
        }    // Tool Config Dialog
        if (showToolConfigDialog) {
            cc.unitmesh.devins.ui.compose.config.ToolConfigDialog(
                onDismiss = { showToolConfigDialog = false },
                onSave = { newConfig ->
                    println("✅ 工具配置已保存")
                    println("   内置工具: 始终启用 (全部)")
                    println("   启用的 MCP 工具: ${newConfig.enabledMcpTools.size}")
                    showToolConfigDialog = false
                },
                llmService = llmService
            )
        }

        // Remote Server Config Dialog
        if (showRemoteConfigDialog) {
            cc.unitmesh.devins.ui.compose.config.RemoteServerConfigDialog(
                currentConfig = cc.unitmesh.devins.ui.compose.config.RemoteServerConfig(
                    serverUrl = serverUrl,
                    useServerConfig = useServerConfig,
                    selectedProjectId = "",
                    defaultGitUrl = remoteGitUrl
                ),
                onDismiss = { showRemoteConfigDialog = false },
                onSave = { newConfig ->
                    serverUrl = newConfig.serverUrl
                    useServerConfig = newConfig.useServerConfig
                    // If a Git URL is provided, propagate it to remote state so UI reacts immediately
                    if (newConfig.defaultGitUrl.isNotBlank()) {
                        remoteGitUrl = newConfig.defaultGitUrl
                        println("📦 Remote Git URL set from dialog: ${newConfig.defaultGitUrl}")
                    }

                    // 保存云端服务器配置到文件
                    scope.launch {
                        try {
                            ConfigManager.saveRemoteServer(
                                cc.unitmesh.devins.ui.config.RemoteServerConfig(
                                    url = newConfig.serverUrl,
                                    enabled = true, // 保存配置后，标记为已启用
                                    useServerConfig = newConfig.useServerConfig
                                )
                            )

                            // 重要：保存 Remote 配置后，自动切换 Agent Type 为 REMOTE
                            cc.unitmesh.devins.ui.config.saveAgentTypePreference("Remote")
                            selectedAgentType = AgentType.REMOTE
                        } catch (e: Exception) {
                            println("⚠️ 保存云端配置失败: ${e.message}")
                            errorMessage = "保存云端配置失败: ${e.message}"
                            showErrorDialog = true
                        }
                    }

                    showRemoteConfigDialog = false
                }
            )
        }

        if (showWasmGitDialog) {
            cc.unitmesh.devins.ui.wasm.WasmGitCloneScreen(
                onClose = { showWasmGitDialog = false }
            )
        }

        if (showConfigWarning) {
            AlertDialog(
                onDismissRequest = { showConfigWarning = false },
                title = {
                    Text(Strings.modelConfigNotConfigured)
                },
                text = {
                    Column {
                        Text(Strings.modelConfigNotConfiguredMessage)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "点击下方按钮打开配置界面。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showConfigWarning = false
                        showModelConfigDialog = true
                    }) {
                        Text("配置模型")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfigWarning = false }) {
                        Text("稍后")
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
                        }
                    ) {
                        Text("重新配置")
                    }
                }
            )
        }
    }
}
