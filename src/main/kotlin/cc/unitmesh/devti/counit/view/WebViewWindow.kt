package cc.unitmesh.devti.counit.view

import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.network.CefRequest

class WebViewWindow(project: Project) {
//    fun registerAppSchemeHandler() {
//        CefApp.getInstance().registerSchemeHandlerFactory(
//            "http",
//            "myapp",
//            CustomSchemeHandlerFactory()
//        )
//    }

    private val browser: JBCefBrowser = JBCefBrowser().also {
//        it.loadURL("http://myapp/index.html")
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