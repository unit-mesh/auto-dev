package cc.unitmesh.devins.ui

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.agent.tool.ToolCategory
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.devins.ui.compose.AutoDevApp
import cc.unitmesh.devins.ui.compose.agent.CodingAgentViewModel
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.desktop.AutoDevMenuBar
import cc.unitmesh.devins.ui.desktop.AutoDevTray
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * DevIn AI Assistant 主应用入口
 *
 * 支持两种模式：
 * 1. 本地 Chat 模式（默认）- 使用 AutoDevApp
 * 2. 远程 Session 模式 - 使用 UnifiedApp（通过 --remote 参数）
 */
fun main(args: Array<String>) {
    // Initialize logging system
    AutoDevLogger.initialize()

    AutoDevLogger.info("AutoDevMain") { "🚀 AutoDev Desktop starting..." }
    AutoDevLogger.info("AutoDevMain") { "📁 Log files location: ${AutoDevLogger.getLogDirectory()}" }

    val useRemoteMode = args.contains("--remote")

    application {
        var isWindowVisible by remember { mutableStateOf(true) }
        var triggerFileChooser by remember { mutableStateOf(false) }
        var showLocalChat by remember { mutableStateOf(!useRemoteMode) }

        val windowState =
            rememberWindowState(
                width = 1200.dp,
                height = 800.dp
            )

        // 系统托盘
        AutoDevTray(
            isWindowVisible = isWindowVisible,
            onShowWindow = { isWindowVisible = true },
            onExit = ::exitApplication
        )

        if (isWindowVisible) {
            Window(
                onCloseRequest = { isWindowVisible = false }, // 关闭时隐藏到托盘
                title = "AutoDev Desktop",
                state = windowState
            ) {
                // 菜单栏
                AutoDevMenuBar(
                    onOpenFile = {
                        // 触发文件选择器
                        triggerFileChooser = true
                        AutoDevLogger.info("AutoDevMain") { "Open File menu clicked" }
                    },
                    onExit = ::exitApplication
                )

                if (showLocalChat) {
                    // 本地 Chat 模式
                    AutoDevApp(
                        triggerFileChooser = triggerFileChooser,
                        onFileChooserHandled = { triggerFileChooser = false }
                    )
                } else {
                    // 远程 Session 模式
                    cc.unitmesh.devins.ui.app.UnifiedApp(
                        serverUrl = "http://localhost:8080",
                        onOpenLocalChat = {
                            showLocalChat = true
                        }
                    )
                }
            }
        }
    }
}

/**
 * 测试工具状态栏功能
 */
private fun testToolStatusBar() =
    runBlocking {
        println("🧪 开始工具状态栏自动化测试...")

        // 测试 1: ToolType 集成
        println("\n📋 测试 1: ToolType 集成")
        val allBuiltinTools = ToolType.ALL_TOOLS
        val subAgentTools = ToolType.byCategory(ToolCategory.SubAgent)
        println("   内置工具总数: ${allBuiltinTools.size}")
        println("   SubAgent 数量: ${subAgentTools.size}")
        println("   内置工具列表: ${allBuiltinTools.map { it.name }}")
        println("   SubAgent 列表: ${subAgentTools.map { it.name }}")

        // 测试 2: 配置加载
        println("\n📋 测试 2: 配置加载")
        try {
            val toolConfig = ConfigManager.loadToolConfig()
            println("   启用的内置工具: ${toolConfig.enabledBuiltinTools}")
            println("   启用的 MCP 工具: ${toolConfig.enabledMcpTools}")
            println("   MCP 服务器数量: ${toolConfig.mcpServers.size}")
            toolConfig.mcpServers.forEach { (name, config) ->
                println("   MCP 服务器: $name (disabled: ${config.disabled})")
            }
        } catch (e: Exception) {
            println("   ⚠️ 配置加载失败: ${e.message}")
        }

        // 测试 3: ViewModel 状态
        println("\n📋 测试 3: ViewModel 状态")
        val mockLLMService =
            KoogLLMService(
                ModelConfig(
                    provider = LLMProviderType.DEEPSEEK,
                    modelName = "deepseek-chat",
                    apiKey = "test-key"
                )
            )

        val viewModel =
            CodingAgentViewModel(
                llmService = mockLLMService,
                projectPath = "/test/path",
                maxIterations = 1
            )

        // 监控状态变化
        println("   开始监控状态变化...")
        for (i in 1..20) {
            val toolStatus = viewModel.getToolLoadingStatus()
            println("   第 $i 秒:")
            println("     Built-in: ${toolStatus.builtinToolsEnabled}/${toolStatus.builtinToolsTotal}")
            println("     SubAgents: ${toolStatus.subAgentsEnabled}/${toolStatus.subAgentsTotal}")
            println("     MCP Tools: ${toolStatus.mcpToolsEnabled} (servers: ${toolStatus.mcpServersLoaded}/${toolStatus.mcpServersTotal})")
            println("     Loading: ${toolStatus.isLoading}")
            println("     Message: ${viewModel.mcpPreloadingMessage}")

            if (!toolStatus.isLoading && toolStatus.mcpServersLoaded > 0) {
                println("   ✅ MCP 预加载完成!")
                break
            }

            delay(1000)
        }

        viewModel.dispose()
        println("\n✅ 工具状态栏测试完成!")
    }
