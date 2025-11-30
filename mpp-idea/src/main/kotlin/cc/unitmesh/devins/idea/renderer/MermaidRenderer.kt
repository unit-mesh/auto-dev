package cc.unitmesh.devins.idea.renderer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.JComponent

/**
 * JCEF-based Mermaid diagram renderer for IntelliJ IDEA.
 * Uses embedded Chromium browser to render Mermaid diagrams.
 *
 * @param parentDisposable Parent disposable for resource cleanup
 * @param onRenderComplete Callback when rendering completes (success, message)
 */
class MermaidRenderer(
    parentDisposable: Disposable,
    private val onRenderComplete: (Boolean, String) -> Unit = { _, _ -> }
) : Disposable {

    private val browser: JBCefBrowser = JBCefBrowser()
    private val renderCallbackQuery: JBCefJSQuery
    private var isInitialized = false

    val component: JComponent get() = browser.component

    init {
        Disposer.register(parentDisposable, this)

        renderCallbackQuery = JBCefJSQuery.create(browser).apply {
            addHandler { result ->
                val success = result.startsWith("success")
                val message = result.substringAfter(":")
                onRenderComplete(success, message)
                null
            }
        }

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                if (frame?.isMain == true) {
                    isInitialized = true
                }
            }
        }, browser.cefBrowser)

        browser.loadHTML(createMermaidHtml())
    }

    fun renderMermaid(mermaidCode: String, darkTheme: Boolean = true) {
        val escapedCode = mermaidCode
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("\$", "\\\$")
            .replace("\n", "\\n")

        val theme = if (darkTheme) "dark" else "default"
        val js = """
            renderMermaid(`$escapedCode`, '$theme')
                .then(() => { ${renderCallbackQuery.inject("'success:rendered'")} })
                .catch(e => { ${renderCallbackQuery.inject("'error:' + e.message")} });
        """.trimIndent()

        browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
    }

    fun setZoomLevel(zoom: Float) {
        browser.zoomLevel = zoom.toDouble()
    }

    override fun dispose() {
        Disposer.dispose(renderCallbackQuery)
    }

    companion object {
        fun isSupported(): Boolean = JBCefApp.isSupported()
    }

    private fun createMermaidHtml(): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
            <style>
                body { margin: 0; padding: 16px; background: transparent; }
                #mermaid-container { width: 100%; }
                .dark { background: #2b2b2b; color: #a9b7c6; }
                .light { background: #ffffff; color: #000000; }
            </style>
        </head>
        <body class="dark">
            <div id="mermaid-container"></div>
            <script>
                mermaid.initialize({ startOnLoad: false, theme: 'dark' });
                
                async function renderMermaid(code, theme) {
                    document.body.className = theme === 'dark' ? 'dark' : 'light';
                    mermaid.initialize({ startOnLoad: false, theme: theme });
                    const container = document.getElementById('mermaid-container');
                    container.innerHTML = '';
                    const { svg } = await mermaid.render('mermaid-graph', code);
                    container.innerHTML = svg;
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

