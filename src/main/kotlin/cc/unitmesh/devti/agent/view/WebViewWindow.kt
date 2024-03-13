package cc.unitmesh.devti.agent.view

import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.Component

/**
 * WebViewWindow is a class that provides a custom webview functionality. It allows developers to
 * create a custom webview within their IntelliJ-based applications. This class is designed to be
 * used in conjunction with the JCEF (JetBrains CEF) plugin, which is a wrapper around the Chromium Embedded Framework.
 *
 */
class WebViewWindow {
    // official doc: https://plugins.jetbrains.com/docs/intellij/jcef.html#executing-javascript
    private val browser: JBCefBrowser

    init {
        browser = try {
            JBCefBrowser.createBuilder()
                .build()
        } catch (e: Exception) {
            JBCefBrowser()
        }

        browser.component.background = JBColor.WHITE
    }

    val component: Component = browser.component

    fun loadHtml(html: String) {
        browser.loadHTML(html)
    }
}