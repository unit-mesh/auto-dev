package cc.unitmesh.devti.language.middleware.builtin.ui

import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefBrowser
import javax.swing.JComponent

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

    val component: JComponent = browser.component

    fun loadHtml(html: String) {
        browser.loadHTML(html)
    }
}