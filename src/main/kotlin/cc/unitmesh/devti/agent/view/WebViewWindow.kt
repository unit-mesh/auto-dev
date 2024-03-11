package cc.unitmesh.devti.agent.view

import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.Component

/**
 * for custom webview can refs: https://github.com/mucharafal/jcef_example
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

    // TODO: ADD BING JS support
}