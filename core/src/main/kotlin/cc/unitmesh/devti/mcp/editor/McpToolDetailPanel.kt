package cc.unitmesh.devti.mcp.editor

import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonElement
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.border.CompoundBorder

/**
 * Component representing a Tool Card with details view functionality
 */
class McpToolDetailPanel(
    private val project: Project,
    private val serverName: String,
    private val tool: Tool
) : JPanel(BorderLayout(0, 8)) {
    private val mcpServerManager = CustomMcpServerManager.instance(project)
    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41)
    private val primaryBlue = JBColor(0x3B82F6, 0x589DF6)
    private val textGray = JBColor(0x6B7280, 0x9DA0A8)
    
    // Constants for UI sizing
    private val MAX_TOOL_CARD_HEIGHT = 180
    private val TOOL_CARD_WIDTH = 300

    init {
        buildCardUI()
    }

    private fun buildCardUI() {
        background = UIUtil.getPanelBackground()
        border = CompoundBorder(
            BorderFactory.createLineBorder(borderColor),
            JBUI.Borders.empty(16)
        )
        // Set preferred width and maximum height
        preferredSize = Dimension(TOOL_CARD_WIDTH, MAX_TOOL_CARD_HEIGHT)
        maximumSize = Dimension(Integer.MAX_VALUE, MAX_TOOL_CARD_HEIGHT)

        // Card header with icon placeholder and title
        val headerPanel = JPanel(BorderLayout(8, 4)).apply {
            background = UIUtil.getPanelBackground()
        }

        // Icon placeholder
        val iconPlaceholder = JPanel().apply {
            preferredSize = Dimension(20, 20)
            background = UIUtil.getPanelBackground()
            border = BorderFactory.createLineBorder(primaryBlue, 1)
        }

        val iconWrapper = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            background = UIUtil.getPanelBackground()
            add(iconPlaceholder)
        }

        val titleLabel = JBLabel(tool.name).apply {
            font = JBUI.Fonts.label(16.0f).asBold()
        }

        val titleWrapper = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            add(titleLabel, BorderLayout.CENTER)
        }

        val titleRow = JPanel(BorderLayout(8, 0)).apply {
            background = UIUtil.getPanelBackground()
            add(iconWrapper, BorderLayout.WEST)
            add(titleWrapper, BorderLayout.CENTER)
        }

        // Make description scrollable if it's too long
        val descriptionText = tool.description ?: "No description available"
        val descLabel = JBLabel("<html><body style='width: ${TOOL_CARD_WIDTH - 50}px'>$descriptionText (from $serverName)</body></html>").apply {
            font = JBUI.Fonts.label(14.0f)
            foreground = textGray
        }
        
        val descScrollPane = JBScrollPane(descLabel).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 8
            background = UIUtil.getPanelBackground()
            preferredSize = Dimension(TOOL_CARD_WIDTH - 32, 70) // Control description height
        }

        headerPanel.add(titleRow, BorderLayout.NORTH)
        headerPanel.add(descScrollPane, BorderLayout.CENTER)

        // Card footer with button
        val footerPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
        }

        val detailsButton = JButton("Details").apply {
            font = JBUI.Fonts.label(14.0f)
            isFocusPainted = false
            addActionListener { showToolDetails() }
        }

        footerPanel.add(detailsButton, BorderLayout.CENTER)

        add(headerPanel, BorderLayout.CENTER)
        add(footerPanel, BorderLayout.SOUTH)
    }

    private fun showToolDetails() {
        val dialog = object : DialogWrapper(project) {
            init {
                title = "Tool Details"
                init()
            }

            override fun createCenterPanel(): JComponent {
                return panel {
                    row {
                        label(tool.name).applyToComponent {
                            font = JBUI.Fonts.label(18.0f).asBold()
                        }
                    }
                    row {
                        label("From server: $serverName").applyToComponent {
                            font = JBUI.Fonts.label(14.0f)
                            foreground = textGray
                        }
                    }
                    row {
                        label(tool.description ?: "No description available").applyToComponent {
                            font = JBUI.Fonts.label(14.0f)
                        }
                    }

                    group("Parameters") {
                        tool.inputSchema.properties?.forEach { param: Map.Entry<String, JsonElement> ->
                            row {
                                label("${param.key}")
                                    .applyToComponent {
                                        font = JBUI.Fonts.label(14.0f)
                                    }
                            }
                            row {
                                label(param.value.toString())
                                    .applyToComponent {
                                        font = JBUI.Fonts.label(12.0f)
                                        foreground = textGray
                                    }
                            }
                        }
                    }
                }.withPreferredSize(400, 200)
            }

            override fun createSouthPanel(): JComponent {
                val panel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
                    background = UIUtil.getPanelBackground()
                }

                val executeButton = JButton("Execute").apply {
                    font = JBUI.Fonts.label(14.0f)
                    addActionListener {
                        onExecute(serverName, tool)
                        close(OK_EXIT_CODE)
                    }
                }

                panel.add(executeButton)
                return panel
            }
        }

        dialog.show()
    }

    fun onExecute(serverName: String, tool: Tool) {
        val result = mcpServerManager.execute(project, tool, "{}")
        JOptionPane.showMessageDialog(
            this,
            result,
            "Tool Execution Result",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
}
