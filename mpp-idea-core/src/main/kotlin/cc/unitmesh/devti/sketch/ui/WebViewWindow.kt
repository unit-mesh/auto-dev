package cc.unitmesh.devti.sketch.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.net.URI
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField

class WebViewWindow {
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

    private val urlField = JTextField()

    private val refreshButton = JButton(AllIcons.Actions.Refresh).apply {
        preferredSize = Dimension(32, 32)
        addActionListener {
            myBrowser.cefBrowser.reload()
        }

    }
    private val openDefaultBrowserButton = JButton(AllIcons.Xml.Browsers.Chrome).apply {
        preferredSize = Dimension(32, 32)
        addActionListener {
            openInSystemBrowser(this@WebViewWindow.urlField.text)
        }
    }

    init {
        myBrowser.component.background = JBColor.WHITE

        myViewerStateJSQuery.addHandler { s: String ->
            JBCefJSQuery.Response(null)
        }

        val myLoadHandler = object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain) {
                    urlField.text = browser.url
                }
            }
        }
        ourCefClient.addLoadHandler(myLoadHandler, myBrowser.cefBrowser)

        urlField.addActionListener {
            val url = urlField.text
            if (url.isNotEmpty()) {
                myBrowser.loadURL(url)
            }
        }

        urlField.preferredSize = Dimension(240, urlField.preferredSize.height)
    }

    val component: Component
        get() {
            val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            controlPanel.add(urlField)
            controlPanel.add(refreshButton)
            controlPanel.add(openDefaultBrowserButton)

            val mainPanel = JPanel(BorderLayout())

            mainPanel.border = BorderFactory.createTitledBorder("WebView Window")

            mainPanel.add(controlPanel, BorderLayout.NORTH)
            mainPanel.add(myBrowser.component, BorderLayout.CENTER)

            return mainPanel
        }

    fun loadHtml(html: String) {
        myBrowser.loadHTML(html)
    }

    fun loadURL(url: String) {
        myBrowser.loadURL(url)
    }

    private fun openInSystemBrowser(url: String) {
        if (url.isEmpty()) return

        try {
            Desktop.getDesktop().browse(URI(url))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}