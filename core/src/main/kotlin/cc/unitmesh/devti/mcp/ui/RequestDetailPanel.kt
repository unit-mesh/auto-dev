package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.mcp.ui.model.McpMessage
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*

/**
 * Panel for displaying request details from MCP messages
 */
class RequestDetailPanel : JPanel(BorderLayout()) {
    private val headerPanel = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(10, 10, 5, 10)
        background = JBColor(0xF8F9FA, 0x2B2D30)
    }
    
    private val toolLabel = JBLabel().apply {
        font = font.deriveFont(Font.BOLD, font.size + 2f)
    }
    
    private val parameterDisplay = ParameterDisplay()
    
    init {
        headerPanel.add(JBLabel("Tool:").apply { 
            font = font.deriveFont(Font.BOLD) 
            border = JBUI.Borders.emptyRight(8)
        }, BorderLayout.WEST)
        headerPanel.add(toolLabel, BorderLayout.CENTER)
        
        add(headerPanel, BorderLayout.NORTH)
        add(parameterDisplay, BorderLayout.CENTER)
    }
    
    fun displayMessage(message: McpMessage) {
        toolLabel.text = message.toolName ?: "Unknown Tool"
        
        val paramJson = message.parameters
        if (paramJson != null && paramJson != "{}" && paramJson.isNotBlank()) {
            try {
                val json = Json { ignoreUnknownKeys = true }
                val parsedJson = json.parseToJsonElement(paramJson).jsonObject
                parameterDisplay.displayParameters(parsedJson)
            } catch (e: Exception) {
                // If parsing fails, fall back to displaying raw JSON
                val errorPanel = JPanel(BorderLayout())
                parameterDisplay.displayParameters(null)
                
                add(JTextArea(paramJson).apply {
                    lineWrap = true
                    wrapStyleWord = true
                    isEditable = false
                }, BorderLayout.SOUTH)
            }
        } else {
            parameterDisplay.displayParameters(null)
        }
    }
}
