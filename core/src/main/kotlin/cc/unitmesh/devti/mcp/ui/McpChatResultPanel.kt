package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import cc.unitmesh.devti.mcp.ui.eval.McpMessageLogPanel
import cc.unitmesh.devti.mcp.ui.model.McpChatConfig
import cc.unitmesh.devti.mcp.ui.model.McpMessage
import cc.unitmesh.devti.mcp.ui.model.MessageType
import cc.unitmesh.devti.mcp.ui.model.ToolCall
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
import java.time.LocalDateTime
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

    private val messageLogPanel = McpMessageLogPanel()

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
        addTab(AutoDevBundle.message("mcp.chat.result.tab.messages"), messageLogPanel)
    }

    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41) // Equivalent to Tailwind gray-200

    private var toolCalls: List<ToolCall> = emptyList()

    init {
        background = UIUtil.getPanelBackground()

        val contentPanel = JPanel(BorderLayout())
        contentPanel.add(tabbedPane, BorderLayout.CENTER)
        add(contentPanel, BorderLayout.CENTER)
    }

    fun reset() {
        rawResultTextArea.text = ""
        
        toolsPanel.removeAll()
        toolsPanel.revalidate()
        toolsPanel.repaint()
        
        messageLogPanel.clear()
        
        toolCalls = emptyList()
        
        tabbedPane.selectedIndex = 0
    }

    fun setText(text: String) {
        rawResultTextArea.text = text
    }

    fun parseAndShowTools(text: String) {
        toolsPanel.removeAll()

        toolCalls = ToolCall.fromString(text)
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

            // Add "Execute All Tools" button
            val executeAllButton = JButton(AutoDevBundle.message("mcp.chat.result.execute.all")).apply {
                font = JBUI.Fonts.label(12f).asBold()
                addActionListener {
                    executeAllTools()
                }
            }

            val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                isOpaque = false
                add(executeAllButton)
            }

            val buttonGbc = GridBagConstraints().apply {
                gridx = 0
                gridy = gridY++
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                insets = JBUI.insetsBottom(10)
            }

            toolsPanel.add(buttonPanel, buttonGbc)

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

    private fun executeAllTools() {
        if (toolCalls.isEmpty()) return

        SwingUtilities.invokeLater {
            for (toolCall in toolCalls) {
                val matchingTool = findMatchingTool(toolCall.name)
                if (matchingTool != null) {
                    val startTime = System.currentTimeMillis()

                    // Log request message
                    val params = try {
                        json.encodeToString(toolCall.parameters)
                    } catch (e: Exception) {
                        "{}"
                    }

                    val requestContent = "Tool: ${toolCall.name}\nParameters: $params"
                    val requestMessage = McpMessage(
                        type = MessageType.REQUEST,
                        method = toolCall.name,
                        timestamp = LocalDateTime.now(),
                        content = requestContent,
                        toolName = toolCall.name,
                        parameters = params
                    )
                    messageLogPanel.addMessage(requestMessage)

                    // Execute the tool
                    val result = mcpServerManager.execute(project, matchingTool, params)
                    val duration = System.currentTimeMillis() - startTime

                    // Log response message
                    val responseMessage = McpMessage(
                        type = MessageType.RESPONSE,
                        method = toolCall.name,
                        timestamp = LocalDateTime.now(),
                        duration = duration,
                        content = result
                    )
                    messageLogPanel.addMessage(responseMessage)
                }
            }

            // Switch to messages tab
            tabbedPane.selectedIndex = 2
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

            val requestContent = "Tool: ${toolCall.name}\nParameters: $params"
            val requestMessage = McpMessage(
                type = MessageType.REQUEST,
                method = toolCall.name,
                timestamp = LocalDateTime.now(),
                content = requestContent,
                toolName = toolCall.name,
                parameters = params
            )
            messageLogPanel.addMessage(requestMessage)

            val matchingTool = findMatchingTool(toolCall.name)
            val result = if (matchingTool != null) {
                mcpServerManager.execute(project, matchingTool, params)
            } else {
                AutoDevBundle.message("mcp.chat.result.error.tool.not.found", toolCall.name)
            }

            // Log response message
            val executionTime = System.currentTimeMillis() - startTime
            val responseMessage = McpMessage(
                type = MessageType.RESPONSE,
                method = toolCall.name,
                timestamp = LocalDateTime.now(),
                duration = executionTime,
                content = result
            )
            messageLogPanel.addMessage(responseMessage)

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
}
