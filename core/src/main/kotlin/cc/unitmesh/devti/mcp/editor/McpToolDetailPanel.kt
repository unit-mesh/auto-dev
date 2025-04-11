package cc.unitmesh.devti.mcp.editor

import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import cc.unitmesh.devti.mcp.client.MockDataGenerator
import cc.unitmesh.devti.provider.local.JsonLanguageField
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.border.CompoundBorder

class McpToolDetailPanel(
    private val project: Project,
    private val serverName: String,
    private val tool: Tool
) : JPanel(BorderLayout(0, 8)) {
    private val mcpServerManager = CustomMcpServerManager.instance(project)
    private val borderColor = JBColor(0xE5E7EB, 0x3C3F41)
    private val textGray = JBColor(0x6B7280, 0x9DA0A8)
    
    private val MAX_TOOL_CARD_HEIGHT = 180
    private val TOOL_CARD_WIDTH = 300
    
    private var jsonLanguageField: JsonLanguageField? = null

    init {
        buildCardUI()
    }

    private fun buildCardUI() {
        background = UIUtil.getPanelBackground()
        border = CompoundBorder(
            BorderFactory.createLineBorder(borderColor),
            JBUI.Borders.empty(12)
        )
        preferredSize = Dimension(TOOL_CARD_WIDTH, MAX_TOOL_CARD_HEIGHT)
        maximumSize = Dimension(Integer.MAX_VALUE, MAX_TOOL_CARD_HEIGHT)

        val headerPanel = JPanel(BorderLayout(8, 4)).apply {
            background = UIUtil.getPanelBackground()
        }

        val titleLabel = JBLabel(serverName + ":" + tool.name).apply {
            font = JBUI.Fonts.label(14.0f).asBold()
        }

        val titleWrapper = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            add(titleLabel, BorderLayout.CENTER)
        }

        val descriptionText = tool.description ?: "No description available"
        val descLabel = JBLabel(descriptionText).apply {
            font = JBUI.Fonts.label(12.0f)
            foreground = textGray
        }

        headerPanel.add(titleWrapper, BorderLayout.NORTH)
        headerPanel.add(descLabel, BorderLayout.CENTER)

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

    private val json = Json { prettyPrint = true }

    private fun showToolDetails() {
        val dialog = object : DialogWrapper(project) {
            init {
                title = "Tool Details"
                init()
            }

            override fun createCenterPanel(): JComponent {
                val mockData = MockDataGenerator.generateMockData(tool.inputSchema)
                val prettyJson = json.encodeToString(mockData)
                
                jsonLanguageField = JsonLanguageField(
                    project, 
                    prettyJson,
                    "Enter parameters as JSON",
                    "parameters.json"
                )
                
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
                        tool.inputSchema.properties.forEach { param: Map.Entry<String, JsonElement> ->
                            row {
                                label(param.key)
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
                    row {
                        label("Edit Parameters").applyToComponent {
                            font = JBUI.Fonts.label(14.0f).asBold()
                        }
                    }
                    row {
                        cell(jsonLanguageField!!)
                            .resizableColumn()
                            .applyToComponent {
                                preferredSize = Dimension(550, 200)
                            }
                    }
                }.withPreferredSize(600, 600)
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
        // Use the content from the jsonLanguageField
        val jsonContent = jsonLanguageField?.text ?: "{}"
        val result = mcpServerManager.execute(project, tool, jsonContent)
        JOptionPane.showMessageDialog(
            this,
            result,
            "Tool Execution Result",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
}
