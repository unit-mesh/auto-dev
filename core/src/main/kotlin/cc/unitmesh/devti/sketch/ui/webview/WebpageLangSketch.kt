package cc.unitmesh.devti.sketch.ui.webview

import cc.unitmesh.devti.agent.view.WebViewWindow
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.panels.VerticalLayout
import javax.swing.JComponent
import javax.swing.JPanel

class WebpageLangSketch(val project: Project, var htmlCode: String) : ExtensionLangSketch {
    private val mainPanel: JPanel = JPanel(VerticalLayout(5))
    private val webViewWindow = WebViewWindow()

    var loadingHtml = """
        <html>
        <head>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    font-size: 14px;
                    color: #333;
                    background-color: #f5f5f5;
                    margin: 0;
                    padding: 0;
                }
                .loading {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                }
                .loading span {
                    font-size: 24px;
                    color: #666;
                }
            </style>
        </head>
        <body>
            <div class="loading">
                <span>Loading...</span>
            </div>
        </body>
        </html>
    """.trimIndent()

    init {
        mainPanel.background = JBColor.PanelBackground
        mainPanel.add(webViewWindow.component)
        webViewWindow.loadHtml(loadingHtml)
    }

    override fun doneUpdateText(allText: String) {
        webViewWindow.loadHtml(htmlCode)
    }

    override fun getExtensionName(): String = "Webpage"
    override fun getViewText(): String = htmlCode
    override fun updateViewText(text: String) {
        this.htmlCode = text
    }

    override fun getComponent(): JComponent = mainPanel
    override fun updateLanguage(language: Language?, originLanguage: String?) {}
    override fun dispose() {}
}