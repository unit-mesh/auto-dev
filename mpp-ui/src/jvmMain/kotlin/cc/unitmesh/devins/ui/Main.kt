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
 * 默认使用 AutoDevApp，支持本地和远程两种 Agent 模式
 * 用户可以在应用内通过 UI 切换模式，配置会保存到 ~/.autodev/config.yaml
 */
fun main(args: Array<String>) {
    // Initialize logging system
    AutoDevLogger.initialize()

    AutoDevLogger.info("AutoDevMain") { "🚀 AutoDev Desktop starting..." }
    AutoDevLogger.info("AutoDevMain") { "📁 Log files location: ${AutoDevLogger.getLogDirectory()}" }

    application {
        var isWindowVisible by remember { mutableStateOf(true) }
        var triggerFileChooser by remember { mutableStateOf(false) }

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

                // 使用 AutoDevApp，支持本地和远程模式切换
                AutoDevApp(
                    triggerFileChooser = triggerFileChooser,
                    onFileChooserHandled = { triggerFileChooser = false }
                )
            }
        }
    }
}
