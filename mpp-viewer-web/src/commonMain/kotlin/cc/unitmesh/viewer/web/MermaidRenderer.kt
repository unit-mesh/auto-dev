package cc.unitmesh.viewer.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData

/**
 * Mermaid diagram renderer using dedicated WebView with JSBridge
 *
 * This composable renders Mermaid diagrams using a dedicated mermaid.html
 * with local mermaid.js for fast rendering.
 *
 * @param mermaidCode The Mermaid diagram code to render
 * @param isDarkTheme Whether to use dark theme
 * @param modifier The modifier for layout
 * @param onRenderComplete Callback when rendering completes (success/failure)
 */
@Composable
fun MermaidRenderer(
    mermaidCode: String,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier,
    onRenderComplete: ((success: Boolean, message: String) -> Unit)? = null
) {
    // Track dynamic height from rendered content
    var webViewHeight by remember { mutableStateOf(200.dp) }
    
    val data = getMermaidHtml()
    println(data)
    val webViewState = rememberWebViewStateWithHTMLData(
        data = data
    )

    val webViewNavigator = rememberWebViewNavigator()
    val jsBridge = rememberWebViewJsBridge()

    LaunchedEffect(Unit) {
        // Handler for render callback
        jsBridge.register(object : IJsMessageHandler {
            override fun methodName(): String = "mermaidRenderCallback"

            override fun handle(
                message: JsMessage,
                navigator: WebViewNavigator?,
                callback: (String) -> Unit
            ) {
                onRenderComplete?.invoke(true, message.params)
                callback("ok")
            }
        })
        
        // Handler for height updates
        jsBridge.register(object : IJsMessageHandler {
            override fun methodName(): String = "mermaidHeightCallback"

            override fun handle(
                message: JsMessage,
                navigator: WebViewNavigator?,
                callback: (String) -> Unit
            ) {
                try {
                    val height = message.params.toDoubleOrNull()
                    if (height != null) {
                        // Apply min/max constraints for safety
                        val constrainedHeight = height.coerceIn(200.0, 2000.0)
                        webViewHeight = constrainedHeight.dp
                        println("MermaidRenderer: Updated height to ${constrainedHeight}dp")
                    }
                } catch (e: Exception) {
                    println("MermaidRenderer: Failed to parse height: ${e.message}")
                }
                callback("ok")
            }
        })
    }

    LaunchedEffect(webViewState.isLoading, mermaidCode, isDarkTheme) {
        println(webViewState.loadingState)
        if (!webViewState.isLoading && webViewState.loadingState is com.multiplatform.webview.web.LoadingState.Finished) {
            // Small delay to ensure everything is initialized
            kotlinx.coroutines.delay(500)

            // Execute JavaScript to render the diagram with theme
            val escapedCode = mermaidCode
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
                .replace("\n", "\\n")

            val theme = if (isDarkTheme) "dark" else "default"

            val jsCode = """
                if (typeof renderMermaid === 'function') {
                    renderMermaid(`$escapedCode`, '$theme');
                }
            """.trimIndent()

            webViewNavigator.evaluateJavaScript(jsCode)
        }
    }

    WebView(
        state = webViewState,
        navigator = webViewNavigator,
        modifier = modifier
            .fillMaxWidth()
            .height(webViewHeight),
        captureBackPresses = false,
        webViewJsBridge = jsBridge
    )
}
