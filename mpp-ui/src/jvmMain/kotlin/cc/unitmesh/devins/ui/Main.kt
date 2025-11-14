package cc.unitmesh.devins.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.devins.ui.compose.AutoDevApp
import cc.unitmesh.devins.ui.desktop.AutoDevMenuBar
import cc.unitmesh.devins.ui.desktop.AutoDevTray

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
