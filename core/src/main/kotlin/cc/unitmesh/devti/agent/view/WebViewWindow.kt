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
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

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
    private val refreshButton = JButton("Refresh")

    init {
        myBrowser.component.background = JBColor.WHITE

        myViewerStateJSQuery.addHandler { s: String ->
            JBCefJSQuery.Response(null)
        }

        val myLoadHandler = object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain) {
                    // Update the URL field when the page finishes loading
                    urlField.text = browser.url
                }
            }
        }
        ourCefClient.addLoadHandler(myLoadHandler, myBrowser.cefBrowser)

        // Set up the refresh button action
        refreshButton.addActionListener {
            myBrowser.cefBrowser.reload()
        }

        // Set up the URL field action
        urlField.addActionListener {
            val url = urlField.text
            if (url.isNotEmpty()) {
                myBrowser.loadURL(url)
            }
        }

        // Set a minimum width for the URL field
        urlField.preferredSize = Dimension(400, urlField.preferredSize.height)
    }

    val component: Component
        get() {
            val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            controlPanel.add(urlField)
            controlPanel.add(refreshButton)

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
}