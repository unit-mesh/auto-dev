package cc.unitmesh.devins.ui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cc.unitmesh.devins.ui.compose.HelloWorld
import cc.unitmesh.devins.ui.compose.theme.AutoDevTheme

/**
 * Simple Hello World Application for Web
 * A minimal Compose for Web application that displays "Hello, World!"
 */
fun main() = application {
    Window(
        title = "Hello World App",
        onCloseRequest = ::exitApplication
    ) {
        AutoDevTheme {
            HelloWorld()
        }
    }
}