package cc.unitmesh.devins.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
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
import cc.unitmesh.devins.llm.KoogLLMService
import cc.unitmesh.devins.llm.ModelConfig
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
fun SimpleAIChat() {
    val scope = rememberCoroutineScope()
    var compilerOutput by remember { mutableStateOf("") }
    var llmOutput by remember { mutableStateOf("") }
    var isCompiling by remember { mutableStateOf(false) }
    var isLLMProcessing by remember { mutableStateOf(false) }
    
    // LLM 配置状态
    var currentModelConfig by remember { mutableStateOf<ModelConfig?>(null) }
    var llmService by remember { mutableStateOf<KoogLLMService?>(null) }
    
    // 项目路径状态（默认路径）
    var projectPath by remember { mutableStateOf<String?>("/Users/phodal/IdeaProjects/untitled") }
    var fileSystem by remember { mutableStateOf<ProjectFileSystem>(
        projectPath?.let { DefaultFileSystem(it) } ?: EmptyFileSystem()
    ) }
    
    // CompletionManager 状态
    var completionManager by remember { mutableStateOf(CompletionManager(fileSystem)) }
    
    val callbacks = object : EditorCallbacks {
        override fun onSubmit(text: String) {
            println("✅ 提交内容:")
            println(text)
            println("\n📝 解析结果:")
            println(analyzeDevInInput(text))
            
            // 编译并执行 DevIns
            compileDevInsWithSpecKit(text, fileSystem, scope) { result ->
                compilerOutput = result
                isCompiling = false
            }
            
            // 如果配置了 LLM，也发送到 LLM
            if (llmService != null && currentModelConfig?.isValid() == true) {
                isLLMProcessing = true
                llmOutput = ""
                
                scope.launch {
                    try {
                        llmService?.streamPrompt(text)
                            ?.catch { e ->
                                llmOutput += "\n\n[Error: ${e.message}]"
                                isLLMProcessing = false
                            }
                            ?.collect { chunk ->
                                llmOutput += chunk
                            }
                        isLLMProcessing = false
                    } catch (e: Exception) {
                        llmOutput = "[Error: ${e.message}]"
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
            onModelConfigChange = { config ->
                currentModelConfig = config
                if (config.isValid()) {
                    try {
                        llmService = KoogLLMService.create(config)
                        println("✅ LLM 服务已配置: ${config.provider.displayName} / ${config.modelName}")
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
        
        // 显示 LLM 输出（优先显示）
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
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = if (llmOutput.isEmpty()) "Thinking..." else llmOutput,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        
        // 显示编译输出
        if (compilerOutput.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📦 DevIns 输出:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = compilerOutput,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                // 创建编译器上下文并设置文件系统
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

private fun getExamplePrompt(): String = ""

