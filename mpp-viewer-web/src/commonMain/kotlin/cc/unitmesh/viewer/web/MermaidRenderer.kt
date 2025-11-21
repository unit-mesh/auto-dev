package cc.unitmesh.viewer.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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
 * @param modifier The modifier for layout
 * @param onRenderComplete Callback when rendering completes (success/failure)
 */
@Composable
fun MermaidRenderer(
    mermaidCode: String,
    modifier: Modifier = Modifier,
    onRenderComplete: ((success: Boolean, message: String) -> Unit)? = null
) {
    val webViewState = rememberWebViewStateWithHTMLData(
        data = getMermaidHtml()
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
    }

    LaunchedEffect(webViewState.isLoading, mermaidCode) {
        if (!webViewState.isLoading && webViewState.loadingState is com.multiplatform.webview.web.LoadingState.Finished) {
            // Small delay to ensure everything is initialized
            kotlinx.coroutines.delay(500)

            // Execute JavaScript to render the diagram
            val escapedCode = mermaidCode
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
                .replace("\n", "\\n")

            val jsCode = """
                if (typeof renderMermaid === 'function') {
                    renderMermaid(`$escapedCode`);
                }
            """.trimIndent()

            webViewNavigator.evaluateJavaScript(jsCode)
        }
    }

    WebView(
        state = webViewState,
        navigator = webViewNavigator,
        modifier = modifier.fillMaxSize(),
        captureBackPresses = false,
        webViewJsBridge = jsBridge
    )
}


