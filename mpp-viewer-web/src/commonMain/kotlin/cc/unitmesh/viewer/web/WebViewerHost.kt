package cc.unitmesh.viewer.web

import cc.unitmesh.viewer.ViewerHost
import cc.unitmesh.viewer.ViewerRequest
import com.multiplatform.webview.web.WebViewNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * WebView-based implementation of ViewerHost
 */
class WebViewerHost(
    private val webViewNavigator: WebViewNavigator
) : ViewerHost {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _currentRequest = MutableStateFlow<ViewerRequest?>(null)
    private val currentRequest: StateFlow<ViewerRequest?> = _currentRequest.asStateFlow()

    private val readyCallbacks = mutableListOf<() -> Unit>()

    /**
     * Mark the viewer as ready (should be called by WebView when it's loaded)
     */
    fun markReady() {
        println("[WebViewerHost] markReady() called")
        _isReady.value = true
        println("[WebViewerHost] isReady=true, executing ${readyCallbacks.size} callbacks")
        readyCallbacks.forEach { it() }
        readyCallbacks.clear()
    }

    override suspend fun showContent(request: ViewerRequest) {
        println("[WebViewerHost] showContent() called: type=${request.type}, language=${request.language}, contentLength=${request.content.length}")
        _currentRequest.value = request

        // Wait for WebView to be ready
        if (!isReady()) {
            println("[WebViewerHost] WARNING: WebView not ready yet, content will not be shown!")
            // Store for later when ready
            return
        }

        println("[WebViewerHost] WebView is ready, sending content via JavaScript")
        // Send the request to WebView via JavaScript
        val json = Json.encodeToString(request)
        val escapedJson = json.replace("\\", "\\\\").replace("'", "\\'")
        val script = "window.showContent('$escapedJson');"

        println("[WebViewerHost] Executing JavaScript: ${script.take(100)}...")
        webViewNavigator.evaluateJavaScript(script)
        println("[WebViewerHost] JavaScript executed successfully")
    }

    override suspend fun clearContent() {
        _currentRequest.value = null
        if (isReady()) {
            webViewNavigator.evaluateJavaScript("window.clearContent();")
        }
    }

    override fun isReady(): Boolean {
        return _isReady.value
    }

    override fun onReady(callback: () -> Unit) {
        if (isReady()) {
            callback()
        } else {
            readyCallbacks.add(callback)
        }
    }

    override fun getCurrentRequest(): ViewerRequest? {
        return _currentRequest.value
    }
}

