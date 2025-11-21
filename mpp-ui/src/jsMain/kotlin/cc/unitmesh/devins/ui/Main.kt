package cc.unitmesh.devins.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import cc.unitmesh.devins.ui.compose.AutoDevApp
import org.jetbrains.skiko.wasm.onWasmReady

/**
 * Markdown 渲染演示应用 - Web 版本
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
fun main() {
    onWasmReady {
        ComposeViewport {
            AutoDevApp()
        }
    }
}
