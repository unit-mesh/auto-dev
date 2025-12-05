package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.sketch.ui.WebViewWindow
import com.intellij.execution.filters.Filter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import javax.swing.JPanel

class FrontendWebViewServerFilter(val project: Project, val mainPanel: JPanel) : Filter {
    var isAlreadyStart = false
    val regex = """Local:\s+(http://localhost:\d+)""".toRegex()

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (isAlreadyStart) return null

        if (!line.contains("Local:")) return null
        val matchResult = regex.find(line)
        if (matchResult == null) return null

        val url = matchResult.groupValues[1]
        ApplicationManager.getApplication().invokeLater {
            val webViewWindow = WebViewWindow().apply {
                loadURL(url)
            }

            val additionalPanel = JPanel(BorderLayout()).apply {
                add(webViewWindow.component, BorderLayout.CENTER)
            }

            mainPanel.add(additionalPanel)
            mainPanel.revalidate()
            mainPanel.repaint()
        }

        isAlreadyStart = true
        return null
    }
}