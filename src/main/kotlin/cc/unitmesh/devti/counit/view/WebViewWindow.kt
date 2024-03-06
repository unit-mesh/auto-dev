package cc.unitmesh.devti.counit.view

import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.network.CefRequest
import java.awt.Component

/**
 * for custom webview can refs: https://github.com/mucharafal/jcef_example
 */
class WebViewWindow {
    private val browser: JBCefBrowser = JBCefBrowser().also {}

    val component: Component = browser.component

    fun loadUrl(url: String) {
        browser.loadURL(url)
    }

    fun loadHtml(html: String) {
        browser.loadHTML(html)
    }

    fun bindJs() {
        val query = JBCefJSQuery.create(browser as JBCefBrowserBase)
        query.addHandler { arg ->
            try {
//                val requestAsJson = Json.parse(arg as String)
                JBCefJSQuery.Response("msg")
            } catch (e: Exception) {
                JBCefJSQuery.Response(null, 0, "errorMsg")
            }
        }

        browser.jbCefClient.addLoadHandler(object : CefLoadHandler {
            override fun onLoadingStateChange(
                cefBrowser: CefBrowser,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean,
            ) {
            }

            override fun onLoadStart(
                cefBrowser: CefBrowser,
                frame: CefFrame,
                transitionType: CefRequest.TransitionType,
            ) {
            }

            override fun onLoadEnd(cefBrowser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                cefBrowser.executeJavaScript("".trimMargin(), null, 0)
            }

            override fun onLoadError(
                cefBrowser: CefBrowser,
                frame: CefFrame,
                errorCode: CefLoadHandler.ErrorCode,
                errorText: String,
                failedUrl: String,
            ) {
            }
        }, browser.cefBrowser)

    }
}