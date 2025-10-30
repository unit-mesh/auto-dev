package cc.unitmesh.devins.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.filesystem.DefaultFileSystem
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.ui.compose.editor.completion.CompletionManager
import cc.unitmesh.devins.ui.compose.chat.*
import cc.unitmesh.devins.llm.KoogLLMService
import cc.unitmesh.devins.llm.ModelConfig
import cc.unitmesh.devins.llm.ChatHistoryManager
import cc.unitmesh.devins.db.ModelConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser

/**
 * 简洁 AI 聊天界面
 * 顶部输入框 + 底部工具栏
 * 
 * 支持 SpecKit 命令，可以打开目录选择项目
 * 支持 LLM 交互（通过 Koog 框架）
 */
@Composable
fun AutoDevInput() {
    val scope = rememberCoroutineScope()
    var compilerOutput by remember { mutableStateOf("") }
    var llmOutput by remember { mutableStateOf("") }
    var isCompiling by remember { mutableStateOf(false) }
    var isLLMProcessing by remember { mutableStateOf(false) }
    
    // 聊天历史管理
    val chatHistoryManager = remember { ChatHistoryManager.getInstance() }
    
    // LLM 配置状态
    var currentModelConfig by remember { mutableStateOf<ModelConfig?>(null) }
    var allModelConfigs by remember { mutableStateOf<List<ModelConfig>>(emptyList()) }
    var llmService by remember { mutableStateOf<KoogLLMService?>(null) }
    var showConfigWarning by remember { mutableStateOf(false) }
    var showDebugPanel by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // 项目路径状态（默认路径）
    var projectPath by remember { mutableStateOf<String?>("/Users/phodal/IdeaProjects/untitled") }
    var fileSystem by remember { mutableStateOf<ProjectFileSystem>(
        projectPath?.let { DefaultFileSystem(it) } ?: EmptyFileSystem()
    ) }
    
    // CompletionManager 状态
    var completionManager by remember { mutableStateOf(CompletionManager(fileSystem)) }
    
    // 初始化数据库和仓库
    val repository = remember {
        ModelConfigRepository.getInstance()
    }
    
    // 启动时加载已保存的配置
    LaunchedEffect(Unit) {
        try {
            val savedConfigs = withContext(Dispatchers.IO) {
                repository.getAllConfigs()
            }
            
            // 保存所有配置到状态
            allModelConfigs = savedConfigs
            
            if (savedConfigs.isNotEmpty()) {
                val defaultConfig = withContext(Dispatchers.IO) {
                    repository.getDefaultConfig()
                }
                val configToUse = defaultConfig ?: savedConfigs.first()
                
                currentModelConfig = configToUse
                if (configToUse.isValid()) {
                    llmService = KoogLLMService.create(configToUse)
                }
            }
        } catch (e: Exception) {
            println("⚠️ 加载配置失败: ${e.message}")
        }
    }
    
    val callbacks = createChatCallbacks(
        fileSystem = fileSystem,
        llmService = llmService,
        chatHistoryManager = chatHistoryManager,
        scope = scope,
        onCompilerOutput = { compilerOutput = it },
        onLLMOutput = { llmOutput = it },
        onProcessingChange = { isLLMProcessing = it },
        onError = { 
            errorMessage = it
                                showErrorDialog = true
        },
        onConfigWarning = { showConfigWarning = true }
    )
    
    // 打开目录选择器
    fun openDirectoryChooser() {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Project Directory"
            currentDirectory = projectPath?.let { File(it) } ?: File(System.getProperty("user.home"))
        }
        
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val selectedPath = fileChooser.selectedFile.absolutePath
            projectPath = selectedPath
            fileSystem = DefaultFileSystem(selectedPath)
            
            // 刷新 CompletionManager
            completionManager = CompletionManager(fileSystem)
            
            println("📁 已切换项目路径: $selectedPath")
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部工具栏
            ChatTopBar(
                hasHistory = chatHistoryManager.getMessages().isNotEmpty(),
                hasDebugInfo = compilerOutput.isNotEmpty(),
                onOpenDirectory = { openDirectoryChooser() },
                onClearHistory = { 
                    chatHistoryManager.clearCurrentSession()
                    llmOutput = ""
                    println("🗑️ [SimpleAIChat] 聊天历史已清空")
                },
                onShowDebug = { showDebugDialog = true }
            )
            
            // 判断是否应该显示紧凑布局（AI 正在处理或有输出）
            val isCompactMode = isLLMProcessing || llmOutput.isNotEmpty()
            
            if (isCompactMode) {
                // 紧凑模式：先显示 AI 输出，输入框在底部
                ChatOutputSection(
                    llmOutput = llmOutput,
                    isLLMProcessing = isLLMProcessing,
                    projectPath = projectPath,
                    fileSystem = fileSystem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                
                // 底部输入框 - 紧凑模式（一行）
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    tonalElevation = 2.dp
                ) {
                    DevInEditorInput(
                        initialText = "",
                        placeholder = "Continue conversation...",
                        callbacks = callbacks,
                        completionManager = completionManager,
                        initialModelConfig = currentModelConfig,
                        availableConfigs = allModelConfigs,
                        isCompactMode = true,
                        onModelConfigChange = { config ->
                            currentModelConfig = config
                            if (config.isValid()) {
                                try {
                                    llmService = KoogLLMService.create(config)
                                    println("✅ LLM 服务已配置: ${config.provider.displayName} / ${config.modelName}")
                                    
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val existingConfigs = repository.getAllConfigs()
                                            val existingConfig = existingConfigs.find { 
                                                it.provider == config.provider && 
                                                it.modelName == config.modelName &&
                                                it.apiKey == config.apiKey 
                                            }
                                            
                                            if (existingConfig == null) {
                                                repository.saveConfig(config, setAsDefault = true)
                                                println("✅ 新配置已保存到数据库")
                                                allModelConfigs = repository.getAllConfigs()
                                            } else {
                                                println("✅ 切换到已有配置")
                                            }
                                        } catch (e: Exception) {
                                            println("⚠️ 保存配置失败: ${e.message}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    println("❌ 配置 LLM 服务失败: ${e.message}")
                                    llmService = null
                                }
                            } else {
                                llmService = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            } else {
                // 默认模式：输入框居中显示
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
        // 完整的输入组件（包含底部工具栏）
        DevInEditorInput(
            initialText = "",
            placeholder = "Plan, @ for context, / for commands (try /speckit.*)",
            callbacks = callbacks,
            completionManager = completionManager,
            initialModelConfig = currentModelConfig,
            availableConfigs = allModelConfigs,
            onModelConfigChange = { config ->
                currentModelConfig = config
                if (config.isValid()) {
                    try {
                        llmService = KoogLLMService.create(config)
                        println("✅ LLM 服务已配置: ${config.provider.displayName} / ${config.modelName}")
                        
                        // 保存配置到数据库
                        scope.launch(Dispatchers.IO) {
                            try {
                                // 检查配置是否已存在
                                val existingConfigs = repository.getAllConfigs()
                                val existingConfig = existingConfigs.find { 
                                    it.provider == config.provider && 
                                    it.modelName == config.modelName &&
                                    it.apiKey == config.apiKey 
                                }
                                
                                if (existingConfig == null) {
                                    // 新配置，保存并设为默认
                                    repository.saveConfig(config, setAsDefault = true)
                                    println("✅ 新配置已保存到数据库")
                                    
                                    // 重新加载所有配置
                                    allModelConfigs = repository.getAllConfigs()
                                } else {
                                    // 已存在的配置，设为默认
                                    println("✅ 切换到已有配置")
                                }
                            } catch (e: Exception) {
                                println("⚠️ 保存配置失败: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ 配置 LLM 服务失败: ${e.message}")
                        llmService = null
                    }
                } else {
                    llmService = null
                }
            },
            modifier = Modifier
                            .fillMaxWidth(0.9f)
                    )
        
                }
            }
        }
        
        // Debug Dialog
        if (showDebugDialog) {
            DebugDialog(
                compilerOutput = compilerOutput,
                onDismiss = { showDebugDialog = false }
            )
        }
        
        // 配置警告弹窗
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
        
        // 错误提示弹窗
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
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            SelectionContainer {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
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
}

