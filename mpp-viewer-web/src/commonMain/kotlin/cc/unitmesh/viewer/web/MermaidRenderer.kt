package cc.unitmesh.viewer.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
 * @param backgroundColor Background color for the WebView
 * @param modifier The modifier for layout
 * @param onRenderComplete Callback when rendering completes (success/failure)
 */
@Composable
fun MermaidRenderer(
    mermaidCode: String,
    isDarkTheme: Boolean = true,
    backgroundColor: Color = if (isDarkTheme) Color(0xFF171717) else Color(0xFFfafafa),
    modifier: Modifier = Modifier,
    onRenderComplete: ((success: Boolean, message: String) -> Unit)? = null
) {
    var webViewHeight by remember { mutableStateOf(200.dp) }
    var showFullscreen by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        MermaidWebView(
            mermaidCode = mermaidCode,
            isDarkTheme = isDarkTheme,
            backgroundColor = backgroundColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(webViewHeight),
            onHeightChange = { height ->
                webViewHeight = height.dp
            },
            onRenderComplete = onRenderComplete
        )
        
        IconButton(
            onClick = { showFullscreen = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = "Expand diagram",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
    
    if (showFullscreen) {
        MermaidFullscreenDialog(
            mermaidCode = mermaidCode,
            isDarkTheme = isDarkTheme,
            backgroundColor = backgroundColor,
            onDismiss = { showFullscreen = false },
            onRenderComplete = onRenderComplete
        )
    }
}


@Composable
fun MermaidFullscreenDialog(
    mermaidCode: String,
    isDarkTheme: Boolean,
    backgroundColor: Color,
    onDismiss: () -> Unit,
    onRenderComplete: ((success: Boolean, message: String) -> Unit)? = null
) {
    var zoomLevel by remember { mutableStateOf(1.0f) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { zoomLevel = (zoomLevel - 0.1f).coerceAtLeast(0.5f) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Zoom out"
                            )
                        }
                        
                        Text(
                            text = "${(zoomLevel * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.widthIn(min = 50.dp)
                        )
                        
                        IconButton(
                            onClick = { zoomLevel = (zoomLevel + 0.1f).coerceAtMost(3.0f) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom in"
                            )
                        }
                        
                        IconButton(
                            onClick = { zoomLevel = 1.0f }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitScreen,
                                contentDescription = "Reset zoom"
                            )
                        }
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                // WebView with zoom
                MermaidWebView(
                    mermaidCode = mermaidCode,
                    isDarkTheme = isDarkTheme,
                    backgroundColor = backgroundColor,
                    zoomLevel = zoomLevel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    onHeightChange = {},
                    onRenderComplete = onRenderComplete
                )
            }
        }
    }
}

@Composable
private fun MermaidWebView(
    mermaidCode: String,
    isDarkTheme: Boolean,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    zoomLevel: Float = 1.0f,
    onHeightChange: (Double) -> Unit,
    onRenderComplete: ((success: Boolean, message: String) -> Unit)?
) {
    val data = getMermaidHtml()
    val webViewState = rememberWebViewStateWithHTMLData(data = data)
    val webViewNavigator = rememberWebViewNavigator()
    val jsBridge = rememberWebViewJsBridge()

    LaunchedEffect(Unit) {
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
                        val constrainedHeight = height.coerceIn(200.0, 2000.0)
                        onHeightChange(constrainedHeight)
                        println("MermaidRenderer: Updated height to ${constrainedHeight}dp")
                    }
                } catch (e: Exception) {
                    println("MermaidRenderer: Failed to parse height: ${e.message}")
                }
                callback("ok")
            }
        })
    }

    LaunchedEffect(webViewState.isLoading, mermaidCode, isDarkTheme, zoomLevel) {
        println(webViewState.loadingState)
        if (!webViewState.isLoading && webViewState.loadingState is com.multiplatform.webview.web.LoadingState.Finished) {
            kotlinx.coroutines.delay(500)

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
                if (typeof setZoomLevel === 'function') {
                    setZoomLevel($zoomLevel);
                }
            """.trimIndent()

            webViewNavigator.evaluateJavaScript(jsCode)
        }
    }

    WebView(
        state = webViewState,
        navigator = webViewNavigator,
        modifier = modifier.background(backgroundColor),
        captureBackPresses = false,
        webViewJsBridge = jsBridge
    )
}
