package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import cc.unitmesh.devti.mcp.ui.model.McpChatConfig
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.*
import java.io.StringReader
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder

class McpChatResultPanel(private val project: Project, val config: McpChatConfig) : JPanel(BorderLayout()) {
    private val mcpServerManager = CustomMcpServerManager.instance(project)
    private val json = Json { prettyPrint = true }

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

    private val responseScrollPane = JBScrollPane(rawResultTextArea).apply {
        border = BorderFactory.createEmptyBorder()
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
    }
    
    private val toolsScrollPane = JBScrollPane(toolsPanel).apply {
        border = BorderFactory.createEmptyBorder()
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
    }

    private val tabbedPane = JBTabbedPane().apply {
        addTab(AutoDevBundle.message("mcp.chat.result.tab.response"), responseScrollPane)
        addTab(AutoDevBundle.message("mcp.chat.result.tab.tools"), toolsScrollPane)
    }

    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41) // Equivalent to Tailwind gray-200

    private var currentHeight = 300

    init {
        background = UIUtil.getPanelBackground()
        
        val contentPanel = JPanel(BorderLayout())
        contentPanel.add(tabbedPane, BorderLayout.CENTER)
        
        add(contentPanel, BorderLayout.CENTER)
        
        tabbedPane.preferredSize = Dimension(width, currentHeight)
    }

    fun setText(text: String) {
        rawResultTextArea.text = text
        parseAndShowTools(text)
    }

    private fun parseAndShowTools(text: String) {
        toolsPanel.removeAll()

        val toolCalls = extractToolCalls(text)
        if (toolCalls.isEmpty()) {
            val noToolsLabel = JBLabel(AutoDevBundle.message("mcp.chat.result.no.tools")).apply {
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
            val nameLabel = JBLabel("$name:").apply {
                font = JBUI.Fonts.label(12f).asBold()
                border = JBUI.Borders.emptyRight(8)
            }

            val valueLabel = JTextArea(value).apply {
                isEditable = false
                wrapStyleWord = true
                lineWrap = true
                background = UIUtil.getPanelBackground().brighter()
                border = null
                margin = JBUI.emptyInsets()
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

        // Add execute button and result panel
        val executeButton = JButton(AutoDevBundle.message("mcp.chat.result.execute")).apply {
            font = JBUI.Fonts.label(12f)
            addActionListener {
                executeToolCall(toolCall, panel)
            }
        }

        val buttonsPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            isOpaque = false
            add(executeButton)
        }

        val resultPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(8)
            isVisible = false
        }

        val contentPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(paramsPanel, BorderLayout.CENTER)
            add(buttonsPanel, BorderLayout.SOUTH)
        }

        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(contentPanel, BorderLayout.CENTER)
        panel.add(resultPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun executeToolCall(toolCall: ToolCall, parentPanel: JPanel) {
        val resultPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(8)
            isVisible = false
        }

        parentPanel.add(resultPanel, BorderLayout.SOUTH)

        val loadingLabel = JBLabel(AutoDevBundle.message("mcp.chat.result.executing", toolCall.name)).apply {
            horizontalAlignment = SwingConstants.CENTER
        }
        resultPanel.add(loadingLabel, BorderLayout.CENTER)
        resultPanel.isVisible = true
        resultPanel.revalidate()
        resultPanel.repaint()

        val startTime = System.currentTimeMillis()

        SwingUtilities.invokeLater {
            val params = try {
                val jsonParams = json.encodeToString(toolCall.parameters)
                jsonParams
            } catch (e: Exception) {
                "{}"
            }

            val matchingTool = findMatchingTool(toolCall.name)

            val result = if (matchingTool != null) {
                mcpServerManager.execute(project, matchingTool, params)
            } else {
                AutoDevBundle.message("mcp.chat.result.error.tool.not.found", toolCall.name)
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            resultPanel.removeAll()
            
            val timeInfoPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                isOpaque = false
                add(JBLabel("${AutoDevBundle.message("mcp.chat.result.execution.time")}: ${executionTime}ms").apply {
                    font = JBUI.Fonts.label(12f)
                    foreground = JBColor(0x6B7280, 0x9DA0A8)
                })
            }
            
            val textArea = JTextArea(result).apply {
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                border = JBUI.Borders.empty(4)
            }

            // Use scroll pane for tool execution results to handle overflow
            val resultScrollPane = JBScrollPane(textArea).apply {
                border = BorderFactory.createEmptyBorder()
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                preferredSize = Dimension(0, 150) // Reasonable default height for results
            }

            resultPanel.add(timeInfoPanel, BorderLayout.NORTH)
            resultPanel.add(resultScrollPane, BorderLayout.CENTER)
            resultPanel.isVisible = true
            resultPanel.revalidate()
            resultPanel.repaint()
        }
    }

    private fun findMatchingTool(toolName: String): Tool? {
        for (tool in config.enabledTools) {
            if (tool.name == toolName) {
                return tool
            }
        }

        return null
    }

    data class ToolCall(
        val name: String,
        val parameters: Map<String, String>
    )

    fun extractToolCalls(text: String): List<ToolCall> {
        val toolCalls = mutableListOf<ToolCall>()

        val codeblock = CodeFence.parse(text)
        if (codeblock.originLanguage != "xml") {
            return emptyList()
        }

        try {
            val xmlFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            val builder = xmlFactory.newDocumentBuilder()
            val inputSource = org.xml.sax.InputSource(StringReader(codeblock.text))
            val document = builder.parse(inputSource)

            val invokeNodes = document.getElementsByTagName("devins:invoke")
            for (i in 0 until invokeNodes.length) {
                val invokeNode = invokeNodes.item(i) as org.w3c.dom.Element
                val toolName = invokeNode.getAttribute("name")

                val parameters = mutableMapOf<String, String>()
                val paramNodes = invokeNode.getElementsByTagName("devins:parameter")
                for (j in 0 until paramNodes.length) {
                    val paramNode = paramNodes.item(j) as org.w3c.dom.Element
                    val paramName = paramNode.getAttribute("name")
                    val paramValue = paramNode.textContent
                    parameters[paramName] = paramValue
                }

                toolCalls.add(ToolCall(toolName, parameters))
            }
        } catch (e: Exception) {
            return emptyList()
        }

        return toolCalls
    }
}
