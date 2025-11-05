package cc.unitmesh.devins.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.ui.compose.HelloWorld
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme

/**
 * Simple Hello World Application
 * A minimal Compose Desktop application that displays "Hello, World!"
 */
fun main() = application {
    val windowState = rememberWindowState(
        width = 800.dp,
        height = 600.dp
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Hello World App",
        state = windowState
    ) {
        AutoDevTheme {
            HelloWorld()
        }
    }
}