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

class McpToolDetailDialog(
    private val project: Project,
    private val serverName: String,
    private val tool: Tool,
    private val mcpServerManager: CustomMcpServerManager
) : DialogWrapper(project) {
    private var jsonLanguageField: JsonLanguageField? = null
    private val json = Json { prettyPrint = true }
    private var resultPanel: JPanel? = null
    private var mainPanel: JPanel? = null

    init {
        title = "MCP Tool Detail - $serverName"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mockData = MockDataGenerator.generateMockData(tool.inputSchema)
        val prettyJson = json.encodeToString(mockData)

        jsonLanguageField = JsonLanguageField(project, prettyJson, "Enter parameters as JSON", "parameters.json")

        resultPanel = JPanel(BorderLayout()).apply {
            isVisible = false
        }

        mainPanel = panel {
            row {
                label(tool.name).applyToComponent {
                    font = JBUI.Fonts.label(18.0f).asBold()
                }
            }
            row {
                label("From server: $serverName").applyToComponent {
                    font = JBUI.Fonts.label(14.0f)
                    foreground = JBColor(0x6B7280, 0x9DA0A8)
                }
            }
            row {
                val textArea = JTextArea().apply {
                    text = tool.description ?: "No description available"
                    isEditable = false
                    wrapStyleWord = true
                    lineWrap = true
                    background = UIUtil.getPanelBackground()
                    border = JBUI.Borders.empty(4)
                }
                cell(textArea)
            }

            group("Parameters") {
                tool.inputSchema.properties.forEach { param: Map.Entry<String, JsonElement> ->
                    row {
                        label(param.key)
                    }
                    row {
                        label(param.value.toString())
                            .applyToComponent {
                                foreground = JBColor(0x6B7280, 0x9DA0A8)
                            }
                    }
                }
            }
            group("Verify") {
                row {
                    cell(jsonLanguageField!!)
                        .resizableColumn()
                        .applyToComponent {
                            preferredSize = Dimension(550, 200)
                        }
                }
            }

            group("Result") {
                row {
                    cell(resultPanel!!)
                        .resizableColumn()
                        .applyToComponent {
                            preferredSize = Dimension(550, 200)
                        }
                }
            }
        }.withPreferredSize(600, 600)

        return mainPanel!!
    }

    override fun createSouthPanel(): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            background = UIUtil.getPanelBackground()
        }

        val executeButton = JButton("Execute").apply {
            font = JBUI.Fonts.label(14.0f)
            addActionListener {
                onExecute()
            }
        }

        panel.add(executeButton)
        return panel
    }

    private fun onExecute() {
        val jsonContent = jsonLanguageField?.text ?: "{}"
        val result = mcpServerManager.execute(project, tool, jsonContent)

        resultPanel?.let { panel ->
            panel.removeAll()

            val textArea = JTextArea(result).apply {
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                font = JBUI.Fonts.create("Monospaced", 12)
            }

            panel.add(JBScrollPane(textArea), BorderLayout.CENTER)
            panel.isVisible = true
            panel.revalidate()
            panel.repaint()
        }

        mainPanel?.revalidate()
        mainPanel?.repaint()
    }
}
