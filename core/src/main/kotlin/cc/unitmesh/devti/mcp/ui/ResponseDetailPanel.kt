package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.mcp.ui.model.McpMessage
import cc.unitmesh.devti.provider.local.JsonLanguageField
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JPanel

class ResponseDetailPanel : JPanel(BorderLayout()) {
    private val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        border = JBUI.Borders.empty(10)
        background = JBColor(0xF8F9FA, 0x2B2D30)
    }
    
    private val durationLabel = JBLabel().apply {
        font = font.deriveFont(Font.BOLD)
    }
    
    private val contentTextArea = JsonLanguageField(
        null, 
        "", 
        "Response content",
        "response.json"
    ).apply {
        border = JBUI.Borders.empty(10)
    }
    
    init {
        headerPanel.add(JBLabel("Response Duration:"))
        headerPanel.add(durationLabel)
        
        add(headerPanel, BorderLayout.NORTH)
        add(JBScrollPane(contentTextArea), BorderLayout.CENTER)
    }
    
    fun displayMessage(message: McpMessage) {
        durationLabel.text = message.duration?.toString()?.plus(" ms") ?: "N/A"
        contentTextArea.text = message.content
    }
}
