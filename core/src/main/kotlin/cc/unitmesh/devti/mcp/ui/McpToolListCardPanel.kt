package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.modelcontextprotocol.kotlin.sdk.Tool
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.border.CompoundBorder

class McpToolListCardPanel(
    private val project: Project,
    private val serverName: String,
    private val tool: Tool
) : JPanel(BorderLayout(0, 0)) {
    private val mcpServerManager = CustomMcpServerManager.instance(project)
    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41)
    private val textGray = JBColor(0x6B7280, 0x9DA0A8)

    private val MAX_TOOL_CARD_HEIGHT = 120
    private val TOOL_CARD_WIDTH = 160

    init {
        buildCardUI()
    }

    private fun buildCardUI() {
        background = UIUtil.getPanelBackground()
        border = CompoundBorder(BorderFactory.createLineBorder(borderColor), JBUI.Borders.empty(4))
        preferredSize = Dimension(TOOL_CARD_WIDTH, MAX_TOOL_CARD_HEIGHT)

        val headerPanel = JPanel(BorderLayout(4, 4)).apply {
            background = UIUtil.getPanelBackground()
        }

        val titleLabel = JBLabel(tool.name).apply {
            font = JBUI.Fonts.label(14.0f).asBold()
        }

        val titleWrapper = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            add(titleLabel, BorderLayout.CENTER)
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

        headerPanel.add(titleWrapper, BorderLayout.NORTH)
        headerPanel.add(descLabel, BorderLayout.CENTER)

        val footerPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
        }

        val detailsButton = JButton("Details").apply {
            isFocusPainted = false
            addActionListener { showToolDetails() }
        }

        footerPanel.add(detailsButton, BorderLayout.CENTER)

        add(headerPanel, BorderLayout.CENTER)
        add(footerPanel, BorderLayout.SOUTH)
    }

    private fun showToolDetails() {
        val dialog = McpToolDetailDialog(project, serverName, tool, mcpServerManager)
        dialog.show()
    }
}

