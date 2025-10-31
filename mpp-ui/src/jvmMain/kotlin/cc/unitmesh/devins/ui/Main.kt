package cc.unitmesh.devins.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.ui.compose.AutoDevApp
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme

/**
 * DevIn AI Assistant 主应用入口
 * 简洁的 AI 对话界面，重点测试语法高亮功能
 */
fun main() = application {
    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "AutoDev Desktop",
        state = windowState
    ) {
        AutoDevTheme {
            AutoDevApp()
        }
    }
}

