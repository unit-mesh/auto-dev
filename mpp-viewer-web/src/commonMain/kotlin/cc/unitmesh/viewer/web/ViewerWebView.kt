package cc.unitmesh.viewer.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cc.unitmesh.viewer.ViewerHost
import cc.unitmesh.viewer.ViewerRequest
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.multiplatform.webview.web.rememberWebViewNavigator

/**
 * Create a WebViewerHost instance
 */
@Composable
fun createWebViewerHost(): ViewerHost {
    val webViewNavigator = rememberWebViewNavigator()
    return remember { WebViewerHost(webViewNavigator) }
}

/**
 * Composable WebView for displaying content
 *
 * @param initialRequest Optional initial request to display when ready
 * @param modifier The modifier for layout
 * @param onHostCreated Callback when the viewer host is created
 */
@Composable
fun ViewerWebView(
    initialRequest: ViewerRequest? = null,
    modifier: Modifier = Modifier,
    onHostCreated: (WebViewerHost) -> Unit = {}
) {
    println("[ViewerWebView] Composing ViewerWebView with initialRequest: type=${initialRequest?.type}, language=${initialRequest?.language}")
    
    val webViewState = rememberWebViewStateWithHTMLData(
        data = getViewerHtml(),
        baseUrl = null,
        encoding = "utf-8",
        mimeType = null,
        historyUrl = null
    )

    println("[ViewerWebView] WebViewState created, isLoading=${webViewState.isLoading}, loadingState=${webViewState.loadingState}")

    val webViewNavigator = rememberWebViewNavigator()
    val viewerHost = remember { 
        println("[ViewerWebView] Creating WebViewerHost")
        WebViewerHost(webViewNavigator).also { 
            println("[ViewerWebView] Calling onHostCreated callback")
            onHostCreated(it) 
        }
    }

    // Monitor WebView ready state
    LaunchedEffect(webViewState.isLoading) {
        println("[ViewerWebView] LaunchedEffect triggered: isLoading=${webViewState.isLoading}, loadingState=${webViewState.loadingState}")
        if (!webViewState.isLoading && webViewState.loadingState is com.multiplatform.webview.web.LoadingState.Finished) {
            println("[ViewerWebView] WebView finished loading, marking ready")
            viewerHost.markReady()

            // Show initial request if provided
            initialRequest?.let { request ->
                println("[ViewerWebView] Showing initial request: ${request.type}")
                viewerHost.showContent(request)
            } ?: println("[ViewerWebView] No initial request to show")
        }
    }

    println("[ViewerWebView] Rendering WebView component")
    WebView(
        state = webViewState,
        navigator = webViewNavigator,
        modifier = modifier.fillMaxSize(),
        captureBackPresses = false
    )
}

/**
 * Get the viewer HTML content from resources
 *
 * This should be implemented in platform-specific code to load from resources
 */
expect fun getViewerHtml(): String

