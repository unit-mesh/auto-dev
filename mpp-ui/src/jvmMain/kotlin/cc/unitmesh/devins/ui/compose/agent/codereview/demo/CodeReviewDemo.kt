package cc.unitmesh.devins.ui.compose.agent.codereview.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.agent.CodeReviewAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer
import cc.unitmesh.devins.ui.compose.agent.codereview.*
import cc.unitmesh.devins.workspace.DefaultWorkspace
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Demo application for Code Review UI with CodeReviewAgent integration
 *
 * Usage:
 * ./gradlew :mpp-ui:runCodeReviewDemo
 * 
 * Environment Variables (optional):
 * - DEEPSEEK_API_KEY: Your DeepSeek API key (default: from ~/.autodev/config.yaml)
 * - PROJECT_PATH: Project path to review (default: /Volumes/source/ai/autocrud)
 */
fun main() {
    // Initialize logger
    AutoDevLogger.initialize()
    
    AutoDevLogger.info("CodeReviewDemo") { "🚀 Starting Code Review Demo Application" }
    AutoDevLogger.info("CodeReviewDemo") { "📁 Log files location: ${AutoDevLogger.getLogDirectory()}" }
    
    application {
        Window(
            onCloseRequest = {
                AutoDevLogger.info("CodeReviewDemo") { "👋 Code Review Demo shutting down" }
                exitApplication()
            },
            title = "Code Review Demo - AutoDev",
            state = rememberWindowState(width = 1400.dp, height = 900.dp)
        ) {
            MaterialTheme {
                CodeReviewDemoApp()
            }
        }
    }
}

@Composable
fun CodeReviewDemoApp() {
    // Default project path - can be overridden by environment variable
    var projectPath by remember { 
        mutableStateOf(System.getenv("PROJECT_PATH") ?: "/Volumes/source/ai/autocrud")
    }
    var workspace: Workspace? by remember { mutableStateOf(null) }
    var viewModel: CodeReviewViewModel? by remember { mutableStateOf(null) }
    var isInitialized by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Initialize workspace and viewModel with CodeReviewAgent
    LaunchedEffect(projectPath) {
        if (projectPath.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    AutoDevLogger.info("CodeReviewDemo") { "=" * 60 }
                    AutoDevLogger.info("CodeReviewDemo") { "🚀 Initializing Code Review Demo" }
                    AutoDevLogger.info("CodeReviewDemo") { "📁 Project Path: $projectPath" }
                    AutoDevLogger.info("CodeReviewDemo") { "=" * 60 }

                    // Step 1: Create workspace
                    AutoDevLogger.info("CodeReviewDemo") { "📦 Creating workspace..." }
                    val ws = DefaultWorkspace.create("Demo Workspace", projectPath)
                    workspace = ws
                    AutoDevLogger.info("CodeReviewDemo") { "✅ Workspace created: ${ws.name}" }

                    // Step 2: Create LLM service
                    AutoDevLogger.info("CodeReviewDemo") { "🤖 Initializing LLM service..." }
                    val (llmService, modelConfig) = createLLMService()
                    AutoDevLogger.info("CodeReviewDemo") { "✅ LLM service initialized: ${modelConfig.modelName}" }

                    // Step 3: Create CodeReviewAgent
                    AutoDevLogger.info("CodeReviewDemo") { "🔧 Creating CodeReviewAgent..." }
                    val codeReviewAgent = createCodeReviewAgent(projectPath, llmService)
                    AutoDevLogger.info("CodeReviewDemo") { "✅ CodeReviewAgent created successfully" }

                    // Step 4: Create ViewModel with CodeReviewAgent
                    AutoDevLogger.info("CodeReviewDemo") { "🎨 Creating ViewModel..." }
                    val vm = CodeReviewViewModel(
                        workspace = ws,
                        codeReviewAgent = codeReviewAgent
                    )
                    viewModel = vm
                    isInitialized = true

                    AutoDevLogger.info("CodeReviewDemo") { "✅ Initialization complete!" }
                    AutoDevLogger.info("CodeReviewDemo") { "=" * 60 }
                } catch (e: Exception) {
                    errorMessage = "Failed to initialize: ${e.message}"
                    AutoDevLogger.error("CodeReviewDemo", e) { "❌ Initialization failed: ${e.message}" }
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Code Review Demo")
                        Text(
                            text = "Logs: ${AutoDevLogger.getLogDirectory()}",
                            style = MaterialTheme.typography.caption
                        )
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                errorMessage != null -> {
                    ErrorScreen(errorMessage!!)
                }
                !isInitialized -> {
                    LoadingScreen()
                }
                viewModel != null -> {
                    CodeReviewDemoContent(
                        viewModel = viewModel!!
                    )
                }
                else -> {
                    ErrorScreen("Failed to initialize - unknown error")
                }
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(viewModel) {
        onDispose {
            viewModel?.dispose()
        }
    }
}

@Composable
private fun CodeReviewDemoContent(viewModel: CodeReviewViewModel) {
    val state by viewModel.state.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            CodeReviewSideBySideView(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Initializing Code Review Demo...")
            Text(
                text = "Check logs at: ${AutoDevLogger.getLogDirectory()}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "❌",
                style = MaterialTheme.typography.h3
            )
            Text(
                text = message,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.error
            )
            Text(
                text = "Check logs at: ${AutoDevLogger.getLogDirectory()}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Create LLM service from environment variables or default config
 * Returns a pair of (KoogLLMService, ModelConfig) for easy access to config
 */
private fun createLLMService(): Pair<KoogLLMService, ModelConfig> {
    AutoDevLogger.info("CodeReviewDemo") { "🔍 Loading LLM configuration..." }
    
    // Try to get API key from environment variable
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: System.getenv("OPENAI_API_KEY") ?: ""
    val provider = if (System.getenv("DEEPSEEK_API_KEY") != null) {
        LLMProviderType.DEEPSEEK
    } else {
        LLMProviderType.OPENAI
    }
    
    val modelName = when (provider) {
        LLMProviderType.DEEPSEEK -> "deepseek-chat"
        LLMProviderType.OPENAI -> "gpt-4"
        else -> "deepseek-chat"
    }
    
    AutoDevLogger.info("CodeReviewDemo") { 
        "   Provider: $provider" 
    }
    AutoDevLogger.info("CodeReviewDemo") { 
        "   Model: $modelName" 
    }
    AutoDevLogger.info("CodeReviewDemo") { 
        "   API Key: ${if (apiKey.isNotEmpty()) "***${apiKey.takeLast(4)}" else "NOT SET"}" 
    }
    
    val modelConfig = ModelConfig(
        provider = provider,
        modelName = modelName,
        apiKey = apiKey,
        temperature = 0.7,
        maxTokens = 8192,
        baseUrl = ""
    )
    
    return Pair(KoogLLMService(modelConfig), modelConfig)
}

/**
 * Create CodeReviewAgent with necessary dependencies
 */
private fun createCodeReviewAgent(
    projectPath: String,
    llmService: KoogLLMService
): CodeReviewAgent {
    AutoDevLogger.info("CodeReviewDemo") { "🛠️  Initializing tool configuration..." }
    
    // Create tool configuration
    val toolConfig = ToolConfigFile.default()
    AutoDevLogger.info("CodeReviewDemo") { "   Tool config: ${toolConfig.enabledBuiltinTools.size} enabled tools" }
    AutoDevLogger.info("CodeReviewDemo") { "   Enabled tools: ${toolConfig.enabledBuiltinTools.joinToString(", ")}" }
    
    // Create MCP tool config service
    val mcpToolConfigService = McpToolConfigService(toolConfig)
    AutoDevLogger.info("CodeReviewDemo") { "   MCP tool config service initialized" }
    
    // Create renderer
    val renderer = ComposeRenderer()
    AutoDevLogger.info("CodeReviewDemo") { "   Renderer: ${renderer::class.simpleName}" }
    
    // Create CodeReviewAgent
    AutoDevLogger.info("CodeReviewDemo") { "🤖 Creating CodeReviewAgent instance..." }
    val agent = CodeReviewAgent(
        projectPath = projectPath,
        llmService = llmService,
        maxIterations = 50,
        renderer = renderer,
        mcpToolConfigService = mcpToolConfigService,
        enableLLMStreaming = true
    )
    
    AutoDevLogger.info("CodeReviewDemo") { "   Agent created: ${agent::class.simpleName}" }
    AutoDevLogger.info("CodeReviewDemo") { "   Max iterations: ${agent.maxIterations}" }
    
    return agent
}

private operator fun String.times(n: Int): String = repeat(n)
