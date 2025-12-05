package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import cc.unitmesh.devti.mcp.client.MockDataGenerator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.border.CompoundBorder

class McpToolListCardPanel(
    private val project: Project,
    private val serverName: String,
    private val tool: Tool
) : JPanel(BorderLayout(0, 0)) {
    private val mcpServerManager = CustomMcpServerManager.instance(project)
    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41)
    private val textGray = JBColor(0x6B7280, 0x9DA0A8)
    private val mutedBackground = JBColor(0xF3F4F6, 0x2B2D30)

    private val MAX_TOOL_CARD_HEIGHT = 160
    private val TOOL_CARD_WIDTH = 200

    init {
        buildCardUI()
    }

    private fun buildCardUI() {
        background = UIUtil.getPanelBackground()
        border = CompoundBorder(BorderFactory.createLineBorder(borderColor), JBUI.Borders.empty(4, 8))
        preferredSize = Dimension(TOOL_CARD_WIDTH, MAX_TOOL_CARD_HEIGHT)

        val headerPanel = JPanel(BorderLayout(8, 0)).apply {
            background = UIUtil.getPanelBackground()
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
                JBUI.Borders.empty(2)
            )
        }

        val titleLabel = JBLabel(tool.name).apply {
            font = JBUI.Fonts.label(14.0f).asBold()
        }

        headerPanel.add(titleLabel, BorderLayout.CENTER)

        val contentPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
        }

        val descriptionText = tool.description ?: "No description available"
        val descLabel = JTextPane().apply {
            text = descriptionText
            font = JBUI.Fonts.label(12.0f)
            foreground = textGray
            isEditable = false
            background = null
            border = null
        }

        contentPanel.add(descLabel, BorderLayout.CENTER)

        val footerPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.emptyTop(4)
        }

        val serverLabel = JBLabel(serverName).apply {
            font = JBUI.Fonts.label(12.0f)
            foreground = textGray
            background = mutedBackground
            border = JBUI.Borders.empty(2, 5)
            isOpaque = true
        }

        val serverWrapperPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            background = UIUtil.getPanelBackground()
            add(serverLabel)
        }

        val detailsLink = HyperlinkLabel("Details").apply {
            font = JBUI.Fonts.label(12.0f)
            addHyperlinkListener { showToolDetails() }
        }

        val testLink = HyperlinkLabel("Test").apply {
            font = JBUI.Fonts.label(12.0f)
            addHyperlinkListener { testTool() }
        }

        val linkWrapperPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
            background = UIUtil.getPanelBackground()
            add(testLink)
            add(detailsLink)
        }

        footerPanel.add(serverWrapperPanel, BorderLayout.WEST)
        footerPanel.add(linkWrapperPanel, BorderLayout.EAST)

        add(headerPanel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)
        add(footerPanel, BorderLayout.SOUTH)
    }

    private fun showToolDetails() {
        val dialog = McpToolDetailDialog(project, serverName, tool, mcpServerManager)
        dialog.show()
    }

    private fun testTool() {
        val mockData = MockDataGenerator.generateMockData(tool.inputSchema)
        val json = Json { prettyPrint = true }
        val jsonContent = json.encodeToString(mockData)
        
        val result = mcpServerManager.execute(project, tool, jsonContent)
        val dialog = ToolTestResultDialog(project, tool, jsonContent, result)
        dialog.show()
    }

    private class ToolTestResultDialog(
        project: Project,
        private val tool: Tool,
        private val inputJson: String,
        private val result: String
    ) : DialogWrapper(project) {
        
        init {
            title = "Test Result: ${tool.name}"
            init()
        }
        
        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout(0, 10))
            panel.preferredSize = Dimension(600, 400)
            
            val topPanel = JPanel(BorderLayout())
            topPanel.add(JLabel("Input:"), BorderLayout.NORTH)
            
            val inputTextArea = JTextArea(inputJson).apply {
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                font = JBUI.Fonts.create("Monospaced", 12)
            }
            topPanel.add(JBScrollPane(inputTextArea), BorderLayout.CENTER)
            
            val bottomPanel = JPanel(BorderLayout())
            bottomPanel.add(JLabel("Result:"), BorderLayout.NORTH)
            
            val resultTextArea = JTextArea(result).apply {
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                font = JBUI.Fonts.create("Monospaced", 12)
            }
            bottomPanel.add(JBScrollPane(resultTextArea), BorderLayout.CENTER)
            
            panel.add(topPanel, BorderLayout.NORTH)
            panel.add(bottomPanel, BorderLayout.CENTER)
            
            return panel
        }
    }
}
