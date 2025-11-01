package cc.unitmesh.devins.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.ui.compose.AutoDevApp

/**
 * DevIn AI Assistant 主应用入口
 * 简洁的 AI 对话界面，支持主题切换
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
        // AutoDevApp 内部已经包含 AutoDevTheme
        AutoDevApp()
    }
}

