package cc.unitmesh.devins.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.ui.compose.MarkdownDemoApp

/**
 * Markdown 渲染演示应用 - Desktop 版本
 */
fun main() = application {
    val windowState = rememberWindowState(
        width = 1400.dp,
        height = 900.dp
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Markdown Renderer Demo - Desktop",
        state = windowState
    ) {
        MarkdownDemoApp()
    }
}

