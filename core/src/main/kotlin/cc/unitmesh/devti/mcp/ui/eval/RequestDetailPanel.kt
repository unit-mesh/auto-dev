package cc.unitmesh.devti.mcp.ui.eval

import cc.unitmesh.devti.mcp.ui.model.McpMessage
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach

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

class ParameterDisplay : JPanel(BorderLayout()) {
    private val contentPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = JBColor(0xFFFFFF, 0x2B2D30)
        border = JBUI.Borders.empty(10)
    }

    init {
        background = JBColor(0xFFFFFF, 0x2B2D30)
        add(JBScrollPane(contentPanel), BorderLayout.CENTER)
    }

    fun displayParameters(parameters: Map<String, JsonElement>?) {
        contentPanel.removeAll()

        if (parameters == null || parameters.isEmpty()) {
            contentPanel.add(JBLabel("No parameters").apply {
                foreground = JBColor.GRAY
                alignmentX = LEFT_ALIGNMENT
            })
        } else {
            contentPanel.add(JBLabel("Parameters:").apply {
                font = font.deriveFont(Font.BOLD, font.size + 1f)
                alignmentX = LEFT_ALIGNMENT
                border = JBUI.Borders.emptyBottom(10)
            })

            parameters.forEach { (key, value) ->
                val paramPanel = JPanel(BorderLayout()).apply {
                    background = contentPanel.background
                    border = JBUI.Borders.emptyBottom(5)
                    alignmentX = LEFT_ALIGNMENT
                    maximumSize = Dimension(Int.MAX_VALUE, getPreferredSize().height)
                }

                paramPanel.add(JBLabel("$key:").apply {
                    font = font.deriveFont(Font.BOLD)
                    border = JBUI.Borders.emptyRight(10)
                }, BorderLayout.WEST)

                val valueText = formatJsonValue(value)
                val valueTextArea = JTextArea(valueText).apply {
                    lineWrap = true
                    wrapStyleWord = true
                    isEditable = false
                    border = null
                    background = contentPanel.background
                }

                paramPanel.add(valueTextArea, BorderLayout.CENTER)
                contentPanel.add(paramPanel)
            }
        }

        contentPanel.add(Box.createVerticalGlue())

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun formatJsonValue(element: JsonElement): String {
        return element.toString()
    }
}
