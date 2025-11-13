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

fun main(args: Array<String>) {
    AutoDevLogger.initialize()

    AutoDevLogger.info("AutoDevMain") { "🚀 AutoDev Desktop starting..." }
    AutoDevLogger.info("AutoDevMain") { "📁 Log files location: ${AutoDevLogger.getLogDirectory()}" }

    // 支持通过命令行参数指定模式：--mode=remote 或 --mode=local
    val mode = args.find { it.startsWith("--mode=") }?.substringAfter("--mode=") ?: "auto"
    
    application {
        var isWindowVisible by remember { mutableStateOf(true) }
        var triggerFileChooser by remember { mutableStateOf(false) }

        val windowState =
            rememberWindowState(
                width = 1200.dp,
                height = 800.dp
            )

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
                AutoDevMenuBar(
                    onOpenFile = {
                        triggerFileChooser = true
                        AutoDevLogger.info("AutoDevMain") { "Open File menu clicked" }
                    },
                    onExit = ::exitApplication
                )

                AutoDevApp(
                    triggerFileChooser = triggerFileChooser,
                    onFileChooserHandled = { triggerFileChooser = false },
                    initialMode = mode
                )
            }
        }
    }
}
