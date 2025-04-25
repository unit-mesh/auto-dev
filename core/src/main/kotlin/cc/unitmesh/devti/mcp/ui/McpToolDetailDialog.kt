package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import cc.unitmesh.devti.mcp.client.MockDataGenerator
import cc.unitmesh.devti.provider.local.JsonLanguageField
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
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
        title = AutoDevBundle.message("mcp.tool.detail.dialog.title", serverName)
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
                label(AutoDevBundle.message("mcp.tool.detail.dialog.from.server", serverName)).applyToComponent {
                    font = JBUI.Fonts.label(14.0f)
                    foreground = JBColor(0x6B7280, 0x9DA0A8)
                }
            }
            row {
                val descriptionText = tool.description ?: AutoDevBundle.message("mcp.tool.detail.dialog.no.description")
                val descLabel = JTextPane().apply {
                    text = descriptionText
                    font = JBUI.Fonts.label(12.0f)
                    isEditable = false
                    background = null
                    border = null
                    preferredSize = Dimension(600, preferredSize.height)
                }
                cell(descLabel).resizableColumn()
            }

            group(AutoDevBundle.message("mcp.tool.detail.dialog.parameters")) {
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
            group(AutoDevBundle.message("mcp.tool.detail.dialog.verify")) {
                row {
                    cell(jsonLanguageField!!)
                        .resizableColumn()
                        .applyToComponent {
                            preferredSize = Dimension(550, 200)
                        }
                }
            }

            group(AutoDevBundle.message("mcp.tool.detail.dialog.result")) {
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

        val executeButton = JButton(AutoDevBundle.message("mcp.tool.detail.dialog.execute")).apply {
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
