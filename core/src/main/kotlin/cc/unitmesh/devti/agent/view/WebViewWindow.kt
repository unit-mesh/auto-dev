package cc.unitmesh.devti.agent.view

import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.Component

/**
 * WebViewWindow is a class that provides a custom webview functionality. It allows developers to
 * create a custom webview within their IntelliJ-based applications. This class is designed to be
 * used in conjunction with the JCEF (JetBrains CEF) plugin, which is a wrapper around the Chromium Embedded Framework.
 *
 */
class WebViewWindow {
    // official doc: https://plugins.jetbrains.com/docs/intellij/jcef.html#executing-javascript
    private val ourCefClient = JBCefApp.getInstance().createClient()
    private val myBrowser: JBCefBrowser = try {
        JBCefBrowser.createBuilder()
            .setClient(ourCefClient)
            .setOffScreenRendering(SystemInfo.isLinux)
            .setEnableOpenDevToolsMenuItem(true)
            .build()
    } catch (e: Exception) {
        JBCefBrowser()
    }
    private val myViewerStateJSQuery: JBCefJSQuery = JBCefJSQuery.create(myBrowser as JBCefBrowserBase)

    init {
        myBrowser.component.background = JBColor.WHITE

        myViewerStateJSQuery.addHandler { s: String ->
            JBCefJSQuery.Response(null)
        }

        var myLoadHandler = object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain) {
                    // todo add some event maybe ?
                }
            }
        }
        ourCefClient.addLoadHandler(myLoadHandler, myBrowser.cefBrowser)
    }

    val component: Component = myBrowser.component

    fun loadHtml(html: String) {
        myBrowser.loadHTML(html)
    }
}