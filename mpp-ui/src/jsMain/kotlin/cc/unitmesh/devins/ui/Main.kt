package cc.unitmesh.devins.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import cc.unitmesh.devins.ui.compose.AutoDevApp
import org.jetbrains.skiko.wasm.onWasmReady

/**
 * Markdown 渲染演示应用 - Web 版本
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    onWasmReady {
        CanvasBasedWindow(
            title = "Markdown Renderer Demo - Web",
            canvasElementId = "ComposeTarget"
        ) {
            AutoDevApp()
        }
    }
}


