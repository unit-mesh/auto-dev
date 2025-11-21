package cc.unitmesh.viewer.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cc.unitmesh.viewer.ViewerHost
import cc.unitmesh.viewer.ViewerRequest
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData

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
    val webViewState = rememberWebViewStateWithHTMLData(
        data = getViewerHtml()
    )

    val webViewNavigator = rememberWebViewNavigator()
    val viewerHost = remember {
        WebViewerHost(webViewNavigator).also {
            onHostCreated(it)
        }
    }

    LaunchedEffect(webViewState.isLoading) {
        if (!webViewState.isLoading && webViewState.loadingState is com.multiplatform.webview.web.LoadingState.Finished) {
            viewerHost.markReady()
            initialRequest?.let { request ->
                viewerHost.showContent(request)
            } ?: println("[ViewerWebView] No initial request to show")
        }
    }

    WebView(
        state = webViewState,
        navigator = webViewNavigator,
        modifier = modifier.fillMaxSize(),
        captureBackPresses = false
    )
}

expect fun getViewerHtml(): String

