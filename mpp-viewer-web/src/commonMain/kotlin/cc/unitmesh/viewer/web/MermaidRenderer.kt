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
    println("[MermaidRenderer] === Starting Mermaid Renderer ===")
    println("[MermaidRenderer] Code length: ${mermaidCode.length} chars")

    val webViewState = rememberWebViewStateWithHTMLData(
        data = getMermaidHtml()
    )

    val webViewNavigator = rememberWebViewNavigator()
    val jsBridge = rememberWebViewJsBridge()

    LaunchedEffect(Unit) {
        println("[MermaidRenderer] Registering JSBridge handlers...")

        // Handler for log messages
        jsBridge.register(object : IJsMessageHandler {
            override fun methodName(): String = "mermaidLog"

            override fun handle(
                message: JsMessage,
                navigator: WebViewNavigator?,
                callback: (String) -> Unit
            ) {
                val logMsg = message.params
                println("[Mermaid JSBridge Log] $logMsg")
                callback("ok")
            }
        })

        // Handler for ready state
        jsBridge.register(object : IJsMessageHandler {
            override fun methodName(): String = "mermaidReady"

            override fun handle(
                message: JsMessage,
                navigator: WebViewNavigator?,
                callback: (String) -> Unit
            ) {
                val ready = message.params
                println("[Mermaid JSBridge Ready] $ready")
                callback("ok")
            }
        })

        // Handler for render callback
        jsBridge.register(object : IJsMessageHandler {
            override fun methodName(): String = "mermaidRenderCallback"

            override fun handle(
                message: JsMessage,
                navigator: WebViewNavigator?,
                callback: (String) -> Unit
            ) {
                println("[Mermaid JSBridge Render] ${message.params}")
                // Parse success and message from params
                // Expected format: {"success": true, "message": "..."}
                onRenderComplete?.invoke(true, message.params)
                callback("ok")
            }
        })
    }

    LaunchedEffect(webViewState.isLoading, mermaidCode) {
        println("[MermaidRenderer] WebView loading state: ${webViewState.isLoading}")

        if (!webViewState.isLoading && webViewState.loadingState is com.multiplatform.webview.web.LoadingState.Finished) {
            println("[MermaidRenderer] WebView finished loading!")

            // Small delay to ensure everything is initialized
            kotlinx.coroutines.delay(500)

            // Execute JavaScript to render the diagram
            val escapedCode = mermaidCode
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
                .replace("\n", "\\n")

            val jsCode = """
                (function() {
                    console.log('[MermaidRenderer JS] Starting render...');
                    alert('Mermaid JS: Starting to render diagram');
                    
                    if (typeof renderMermaid === 'function') {
                        alert('Mermaid JS: renderMermaid function found!');
                        renderMermaid(`$escapedCode`);
                    } else {
                        alert('Mermaid JS: ERROR - renderMermaid function NOT found!');
                        console.error('[MermaidRenderer JS] renderMermaid function not found');
                    }
                })();
            """.trimIndent()

            println("[MermaidRenderer] Executing JavaScript...")
            webViewNavigator.evaluateJavaScript(jsCode) { result ->
                println("[MermaidRenderer] JavaScript execution result: $result")
            }
        }
    }

    WebView(
        rememberWebViewState("https://www.phodal.com"),
        navigator = webViewNavigator,
        modifier = modifier.fillMaxSize(),
        captureBackPresses = true,
        webViewJsBridge = jsBridge
    )
}


