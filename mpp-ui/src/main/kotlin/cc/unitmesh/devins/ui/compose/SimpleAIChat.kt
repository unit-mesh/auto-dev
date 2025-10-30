package cc.unitmesh.devins.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.compiler.DevInsCompilerFacade
import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.filesystem.DefaultFileSystem
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.ui.compose.editor.completion.CompletionManager
import cc.unitmesh.devins.ui.compose.editor.model.EditorCallbacks
import cc.unitmesh.devins.ui.compose.sketch.SketchRenderer
import cc.unitmesh.devins.llm.KoogLLMService
import cc.unitmesh.devins.llm.ModelConfig
import cc.unitmesh.devins.db.ModelConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
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
    
    // LLM 配置状态
    var currentModelConfig by remember { mutableStateOf<ModelConfig?>(null) }
    var allModelConfigs by remember { mutableStateOf<List<ModelConfig>>(emptyList()) }
    var llmService by remember { mutableStateOf<KoogLLMService?>(null) }
    var showConfigWarning by remember { mutableStateOf(false) }
    var showDebugPanel by remember { mutableStateOf(false) }
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
    
    val callbacks = object : EditorCallbacks {
        override fun onSubmit(text: String) {
            if (currentModelConfig == null || !currentModelConfig!!.isValid()) {
                showConfigWarning = true
                return
            }
            
            compileDevInsWithSpecKit(text, fileSystem, scope) { result ->
                compilerOutput = result
                isCompiling = false
            }
            
            // 发送到 LLM（带 DevIns 编译和 SpecKit 支持）
            if (llmService != null) {
                isLLMProcessing = true
                llmOutput = ""
                
                scope.launch {
                    try {
                        // 传递 fileSystem 以支持 SpecKit 命令编译
                        llmService?.streamPrompt(text, fileSystem)
                            ?.catch { e ->
                                val errorMsg = extractErrorMessage(e)
                                errorMessage = errorMsg
                                showErrorDialog = true
                                isLLMProcessing = false
                            }
                            ?.collect { chunk ->
                                llmOutput += chunk
                            }
                        isLLMProcessing = false
                    } catch (e: Exception) {
                        // 捕获其他错误
                        val errorMsg = extractErrorMessage(e)
                        errorMessage = errorMsg
                        showErrorDialog = true
                        llmOutput = ""
                        isLLMProcessing = false
                    }
                }
            }
        }
    }
    
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部工具栏（打开目录按钮）
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AutoDev - DevIn AI",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Button(
                onClick = { openDirectoryChooser() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Open Directory"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Directory")
            }
        }
        
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
                .fillMaxWidth(0.9f) // 90% 宽度，更居中
        )
        
        // 显示 LLM 输出（优先显示）- 使用 Sketch 渲染器
        if (llmOutput.isNotEmpty() || isLLMProcessing) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🤖 AI Response:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (isLLMProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 使用 SketchRenderer 渲染内容
                    if (llmOutput.isEmpty()) {
                        Text(
                            text = "Thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    } else {
                        SketchRenderer.RenderResponse(
                            content = llmOutput,
                            isComplete = !isLLMProcessing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        
        // Debug 面板 - 可折叠显示 DevIns 编译输出
        if (compilerOutput.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth(0.9f)) {
                // Debug 按钮
                OutlinedButton(
                    onClick = { showDebugPanel = !showDebugPanel },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Debug",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DevIns 调试输出")
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (showDebugPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showDebugPanel) "收起" else "展开"
                    )
                }
                
                // 可折叠的调试内容
                if (showDebugPanel) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        val scrollState = rememberScrollState()
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                        ) {
                            SelectionContainer {
                                Text(
                                    text = compilerOutput,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 显示项目路径提示
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (projectPath != null) "📁 Project: $projectPath" else "⚠️ No project selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            
            if (projectPath != null) {
                // 显示 SpecKit 命令数量
                val commandCount = remember(fileSystem) {
                    try {
                        cc.unitmesh.devins.command.SpecKitCommand.loadAll(fileSystem).size
                    } catch (e: Exception) {
                        0
                    }
                }
                
                Text(
                    text = "✨ $commandCount SpecKit commands",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
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

/**
 * 提取错误信息
 */
private fun extractErrorMessage(e: Throwable): String {
    val message = e.message ?: "Unknown error"

    return when {
        message.contains("DeepSeekLLMClient API") -> {
            val parts = message.split("API: ")
            if (parts.size > 1) {
                "=== DeepSeek API 错误 ===\n\n" +
                "API 返回：\n${parts[1]}\n\n" +
                "完整错误信息：\n$message"
            } else {
                "=== DeepSeek API 错误 ===\n\n$message"
            }
        }
        
        message.contains("OpenAI") -> {
            "=== OpenAI API 错误 ===\n\n$message"
        }
        
        message.contains("Anthropic") -> {
            "=== Anthropic API 错误 ===\n\n$message"
        }
        
        message.contains("Connection") || message.contains("timeout") -> {
            "=== 网络连接错误 ===\n\n$message"
        }
        
        message.contains("401") || message.contains("Unauthorized") -> {
            "=== 认证失败 (401 Unauthorized) ===\n\n$message"
        }
        
        message.contains("400") || message.contains("Bad Request") -> {
            "=== 请求错误 (400 Bad Request) ===\n\n$message"
        }
        
        message.contains("429") || message.contains("rate limit") -> {
            "=== 请求限流 (429 Too Many Requests) ===\n\n$message"
        }
        
        message.contains("500") || message.contains("Internal Server Error") -> {
            "=== 服务器错误 (500) ===\n\n$message"
        }
        
        else -> {
            "=== 错误详情 ===\n\n" +
            "错误类型：${e::class.simpleName}\n\n" +
            "错误消息：\n$message"
        }
    }
}

/**
 * 分析 DevIn 输入
 */
private fun analyzeDevInInput(text: String): String {
    val analysis = mutableListOf<String>()
    
    // 检测 Agent
    val agents = Regex("@(\\w+)").findAll(text).map { it.groupValues[1] }.toList()
    if (agents.isNotEmpty()) {
        analysis.add("检测到 Agents: ${agents.joinToString(", ")}")
    }
    
    // 检测 Command
    val commands = Regex("/(\\w+):").findAll(text).map { it.groupValues[1] }.toList()
    if (commands.isNotEmpty()) {
        analysis.add("检测到 Commands: ${commands.joinToString(", ")}")
    }
    
    // 检测 Variable
    val variables = Regex("\\$(\\w+)").findAll(text).map { it.groupValues[1] }.toList()
    if (variables.isNotEmpty()) {
        analysis.add("检测到 Variables: ${variables.joinToString(", ")}")
    }
    
    // 检测 FrontMatter
    if (text.contains("---")) {
        analysis.add("包含 FrontMatter 配置")
    }
    
    // 检测代码块
    val codeBlocks = Regex("```(\\w*)").findAll(text).map { it.groupValues[1].ifEmpty { "plain" } }.toList()
    if (codeBlocks.isNotEmpty()) {
        analysis.add("包含代码块: ${codeBlocks.joinToString(", ")}")
    }
    
    return if (analysis.isNotEmpty()) {
        analysis.joinToString("\n• ", "• ")
    } else {
        "纯文本输入"
    }
}

/**
 * 编译 DevIns 代码并支持 SpecKit 命令
 */
private fun compileDevInsWithSpecKit(
    text: String,
    fileSystem: ProjectFileSystem,
    scope: CoroutineScope,
    onResult: (String) -> Unit
) {
    scope.launch {
        try {
            val result = withContext(Dispatchers.IO) {
                val context = CompilerContext().apply {
                    this.fileSystem = fileSystem
                }
                
                // 使用 DevInsCompilerFacade 编译
                DevInsCompilerFacade.compile(text, context)
            }
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess()) {
                    onResult(buildString {
                        appendLine("✅ 编译成功!")
                        appendLine()
                        appendLine("输出:")
                        appendLine(result.output)
                        appendLine()
                        appendLine("统计:")
                        appendLine("- 变量: ${result.statistics.variableCount}")
                        appendLine("- 命令: ${result.statistics.commandCount}")
                        appendLine("- Agent: ${result.statistics.agentCount}")
                    })
                } else {
                    onResult("❌ 编译失败: ${result.errorMessage}")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult("❌ 异常: ${e.message}\n${e.stackTraceToString()}")
            }
        }
    }
}
