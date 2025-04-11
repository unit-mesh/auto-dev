package cc.unitmesh.devti.mcp.editor

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder
import java.util.regex.Pattern

class McpResultPanel : JPanel(BorderLayout()) {
    private val rawResultTextArea = JTextArea().apply {
        isEditable = false
        wrapStyleWord = true
        lineWrap = true
        background = UIUtil.getPanelBackground()
        border = JBUI.Borders.empty(4)
    }

    private val toolsPanel = JPanel(GridBagLayout()).apply {
        background = UIUtil.getPanelBackground()
        border = JBUI.Borders.empty(8)
    }

    private val tabbedPane = JBTabbedPane().apply {
        addTab("Response", JBScrollPane(rawResultTextArea).apply {
            border = BorderFactory.createEmptyBorder()
        })
        
        addTab("Tools", JBScrollPane(toolsPanel).apply {
            border = BorderFactory.createEmptyBorder()
        })
    }

    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41) // Equivalent to Tailwind gray-200

    init {
        background = UIUtil.getPanelBackground()
        add(tabbedPane, BorderLayout.CENTER)
    }

    fun setText(text: String) {
        rawResultTextArea.text = text
        parseAndShowTools(text)
    }

    private fun parseAndShowTools(text: String) {
        toolsPanel.removeAll()

        val toolCalls = extractToolCalls(text)
        if (toolCalls.isEmpty()) {
            val noToolsLabel = JBLabel("No tool calls found in the response").apply {
                foreground = JBColor(0x6B7280, 0x9DA0A8)  // Gray text
                horizontalAlignment = SwingConstants.CENTER
            }
            
            val gbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 1.0
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            }
            toolsPanel.add(noToolsLabel, gbc)
        } else {
            var gridY = 0
            
            toolCalls.forEach { toolCall ->
                val toolPanel = createToolCallPanel(toolCall)
                
                val gbc = GridBagConstraints().apply {
                    gridx = 0
                    gridy = gridY++
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    insets = JBUI.insetsBottom(10)
                }
                
                toolsPanel.add(toolPanel, gbc)
            }
            
            // Add empty filler panel at the end
            val fillerPanel = JPanel()
            fillerPanel.isOpaque = false
            
            val gbc = GridBagConstraints().apply {
                gridx = 0
                gridy = gridY
                weightx = 1.0
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            }
            
            toolsPanel.add(fillerPanel, gbc)
        }
        
        toolsPanel.revalidate()
        toolsPanel.repaint()
        
        if (toolCalls.isNotEmpty()) {
            tabbedPane.selectedIndex = 1
        }
    }

    private fun extractToolCalls(text: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()
        
        val codeBlockPattern = Pattern.compile("```(?:xml|)\\s*devins:function_calls\\s*(.*?)\\s*```", Pattern.DOTALL)
        val matcher = codeBlockPattern.matcher(text)
        
        while (matcher.find()) {
            val xmlContent = matcher.group(1)
            
            val invokePattern = Pattern.compile("<devins:invoke\\s+name=\"([^\"]+)\">\\s*(.*?)\\s*</devins:invoke>", Pattern.DOTALL)
            val invokeMatcher = invokePattern.matcher(xmlContent)
            
            while (invokeMatcher.find()) {
                val toolName = invokeMatcher.group(1)
                val paramsXml = invokeMatcher.group(2)
                
                val params = mutableMapOf<String, String>()
                val paramPattern = Pattern.compile("<devins:parameter\\s+name=\"([^\"]+)\">(.*?)</devins:parameter>", Pattern.DOTALL)
                val paramMatcher = paramPattern.matcher(paramsXml)
                
                while (paramMatcher.find()) {
                    val paramName = paramMatcher.group(1)
                    val paramValue = paramMatcher.group(2)
                    params[paramName] = paramValue
                }
                
                toolCalls.add(ToolCall(toolName, params))
            }
        }
        
        return toolCalls
    }

    private fun createToolCallPanel(toolCall: ToolCall): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground().brighter()
            border = CompoundBorder(
                MatteBorder(1, 1, 1, 1, borderColor),
                EmptyBorder(JBUI.insets(10))
            )
        }
        
        val titleLabel = JBLabel(toolCall.name).apply {
            font = JBUI.Fonts.label(14f).asBold()
            border = JBUI.Borders.emptyBottom(8)
        }
        
        val paramsPanel = JPanel(GridBagLayout()).apply {
            isOpaque = false
        }
        
        var paramGridY = 0
        toolCall.parameters.forEach { (name, value) ->
            val nameLabel = JBLabel(name + ":").apply {
                font = JBUI.Fonts.label(12f).asBold()
                border = JBUI.Borders.emptyRight(8)
            }
            
            val valueLabel = JTextArea(value).apply {
                isEditable = false
                wrapStyleWord = true
                lineWrap = true
                background = UIUtil.getPanelBackground().brighter()
                border = null
                margin = JBUI.insets(0)
            }
            
            val nameGbc = GridBagConstraints().apply {
                gridx = 0
                gridy = paramGridY
                anchor = GridBagConstraints.NORTHWEST
                insets = JBUI.insets(2)
            }
            
            val valueGbc = GridBagConstraints().apply {
                gridx = 1
                gridy = paramGridY++
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                insets = JBUI.insets(2)
            }
            
            paramsPanel.add(nameLabel, nameGbc)
            paramsPanel.add(valueLabel, valueGbc)
        }
        
        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(paramsPanel, BorderLayout.CENTER)
        
        return panel
    }

    data class ToolCall(
        val name: String,
        val parameters: Map<String, String>
    )
}
