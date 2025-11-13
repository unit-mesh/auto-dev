package cc.unitmesh.devins.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.completion.CompletionManager
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.llm.ChatHistoryManager
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.ui.app.AndroidNavLayout
import cc.unitmesh.devins.ui.app.AppScreen
import cc.unitmesh.devins.ui.compose.agent.AgentChatInterface
import cc.unitmesh.devins.ui.compose.chat.*
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.ui.compose.editor.ModelConfigDialog
import cc.unitmesh.devins.ui.compose.editor.model.EditorCallbacks
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
 * Android 专属的 AutoDevApp 实现
 * 
 * 设计要点：
 * - BottomNavigation：Home/Chat/Tasks/Profile（4个入口）
 * - Drawer：完整导航 + 设置工具 + 用户信息
 * - TopBar：汉堡菜单 + 标题 + 操作按钮
 * - 全屏沉浸式 Chat 体验
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
        AndroidAutoDevContent(
            triggerFileChooser = triggerFileChooser,
            onFileChooserHandled = onFileChooserHandled,
            initialMode = initialMode
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AndroidAutoDevContent(
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
    var showDebugDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // 导航状态
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    
    // Session 管理（用于 Drawer 和登录）
    val sessionClient = remember { SessionClient("http://localhost:8080") }
    val sessionViewModel = remember { SessionViewModel(sessionClient) }
    
    // Agent 模式状态
    var selectedAgentType by remember { mutableStateOf("Local") }
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
        
        // 初始化工作空间
        if (!WorkspaceManager.hasActiveWorkspace()) {
            val defaultPath = "/storage/emulated/0/Documents/AutoDev"
            try {
                WorkspaceManager.openWorkspace("Default Workspace", defaultPath)
            } catch (e: Exception) {
                println("⚠️ 打开工作空间失败: ${e.message}")
            }
        }
    }
    
    LaunchedEffect(workspaceState) {
        workspaceState?.let { workspace ->
            currentWorkspace = workspace
        }
    }
    
    // Chat callbacks（需要明确类型）
    val callbacks: EditorCallbacks = createChatCallbacks(
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
        onProcessingChange = { isLLMProcessing = it },
        onError = {
            errorMessage = it
            showErrorDialog = true
        },
        onConfigWarning = { showModelConfigDialog = true }
    )
    
    // Android NavLayout（Drawer + BottomNavigation）
    AndroidNavLayout(
        currentScreen = currentScreen,
        onScreenChange = { currentScreen = it },
        sessionViewModel = sessionViewModel,
        onShowSettings = { showModelConfigDialog = true },
        onShowTools = { showToolConfigDialog = true },
        onShowDebug = { showDebugDialog = true },
        hasDebugInfo = compilerOutput.isNotEmpty(),
        actions = {
            // TopBar 右侧操作按钮
            when (currentScreen) {
                AppScreen.HOME, AppScreen.CHAT -> {
                    // 切换 Agent/Chat 模式
                    IconButton(onClick = { useAgentMode = !useAgentMode }) {
                        Icon(
                            imageVector = if (useAgentMode) Icons.Default.SmartToy else Icons.AutoMirrored.Filled.Chat,
                            contentDescription = if (useAgentMode) "Agent 模式" else "Chat 模式"
                        )
                    }
                    // TreeView 切换（仅 Agent 模式）
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
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (currentScreen) {
                AppScreen.HOME, AppScreen.CHAT -> {
                    // HOME 和 CHAT 都显示主界面（Agent/Chat）
                    if (useAgentMode) {
                        // Agent 模式（使用原来的 AgentChatInterface）
                        AgentChatInterface(
                            llmService = llmService,
                            isTreeViewVisible = isTreeViewVisible,
                            onConfigWarning = { showModelConfigDialog = true },
                            onToggleTreeView = { isTreeViewVisible = it },
                            chatHistoryManager = chatHistoryManager,
                            hasHistory = messages.isNotEmpty(),
                            hasDebugInfo = compilerOutput.isNotEmpty(),
                            currentModelConfig = currentModelConfig,
                            selectedAgent = "Default",
                            availableAgents = listOf("Default"),
                            useAgentMode = useAgentMode,
                            selectedAgentType = selectedAgentType,
                            onOpenDirectory = {},
                            onClearHistory = {
                                chatHistoryManager.clearCurrentSession()
                                messages = emptyList()
                                currentStreamingOutput = ""
                            },
                            onShowDebug = { showDebugDialog = true },
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
                            onAgentTypeChange = { type -> selectedAgentType = type },
                            onConfigureRemote = {},
                            onShowModelConfig = { showModelConfigDialog = true },
                            onShowToolConfig = { showToolConfigDialog = true },
                            showTopBar = false, // Android 使用统一的 TopBar
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Chat 模式（使用 MessageList + DevInEditorInput）
                        ChatModeScreen(
                            messages = messages,
                            currentStreamingOutput = currentStreamingOutput,
                            isLLMProcessing = isLLMProcessing,
                            callbacks = callbacks,
                            completionManager = currentWorkspace.completionManager,
                            projectPath = currentWorkspace.rootPath ?: "/",
                            fileSystem = currentWorkspace.fileSystem,
                            currentModelConfig = currentModelConfig,
                            onModelConfigChange = { config ->
                                currentModelConfig = config
                                if (config.isValid()) {
                                    try {
                                        llmService = KoogLLMService.create(config)
                                    } catch (e: Exception) {
                                        println("❌ 切换模型失败: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                }
                
                AppScreen.TASKS -> {
                    TasksPlaceholderScreen()
                }
                
                AppScreen.PROFILE -> {
                    ProfileScreen(
                        currentModelConfig = currentModelConfig,
                        onShowModelConfig = { showModelConfigDialog = true },
                        onShowToolConfig = { showToolConfigDialog = true }
                    )
                }
                
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("屏幕开发中...")
                    }
                }
            }
        }
    }
    
    // Dialogs
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
    
    if (showToolConfigDialog) {
        cc.unitmesh.devins.ui.compose.config.ToolConfigDialog(
            onDismiss = { showToolConfigDialog = false },
            onSave = { newConfig ->
                println("✅ 工具配置已保存")
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
    
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("错误") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

/**
 * Chat 模式屏幕（非 Agent 模式）
 */
@Composable
private fun ChatModeScreen(
    messages: List<Message>,
    currentStreamingOutput: String,
    isLLMProcessing: Boolean,
    callbacks: EditorCallbacks,
    completionManager: CompletionManager,
    projectPath: String,
    fileSystem: ProjectFileSystem,
    currentModelConfig: ModelConfig?,
    onModelConfigChange: (ModelConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val isCompactMode = messages.isNotEmpty() || isLLMProcessing
        
        if (isCompactMode) {
            // 有消息时显示列表
            MessageList(
                messages = messages,
                isLLMProcessing = isLLMProcessing,
                currentOutput = currentStreamingOutput,
                projectPath = projectPath,
                fileSystem = fileSystem,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            
            DevInEditorInput(
                initialText = "",
                placeholder = "输入消息...",
                callbacks = callbacks,
                completionManager = completionManager,
                isCompactMode = true,
                onModelConfigChange = onModelConfigChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        } else {
            // 空状态 - 居中显示输入框
            Box(
                modifier = Modifier.fillMaxSize().imePadding().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                DevInEditorInput(
                    initialText = "",
                    placeholder = "输入消息...",
                    callbacks = callbacks,
                    completionManager = completionManager,
                    onModelConfigChange = onModelConfigChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


/**
 * Profile 屏幕组件
 */
@Composable
private fun ProfileScreen(
    currentModelConfig: ModelConfig?,
    onShowModelConfig: () -> Unit,
    onShowToolConfig: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // 模型配置
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onShowModelConfig
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "模型配置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = currentModelConfig?.let {
                            "${it.provider.displayName} / ${it.modelName}"
                        } ?: "未配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        
        // 工具配置
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onShowToolConfig
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "工具配置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "管理 MCP 工具和内置工具",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        
        // 关于
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "关于 AutoDev",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "版本：0.1.5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "AI 驱动的开发助手",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Tasks 占位屏幕
 */
@Composable
private fun TasksPlaceholderScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "任务管理",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "即将推出",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

