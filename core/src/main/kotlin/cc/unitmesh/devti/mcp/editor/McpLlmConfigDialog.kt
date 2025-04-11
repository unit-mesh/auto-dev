package cc.unitmesh.devti.mcp.editor

import cc.unitmesh.devti.gui.planner.MarkdownLanguageField
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent
import javax.swing.JSlider


data class McpLlmConfig(
    var temperature: Double = 0.0,
    var enabledTools: MutableList<String> = mutableListOf(),
    var systemPrompt: String = ""
)

class McpLlmConfigDialog(
    private val project: Project,
    private val config: McpLlmConfig,
    private val chatbotName: String,
    private val allTools: Map<String, List<io.modelcontextprotocol.kotlin.sdk.Tool>>
) : DialogWrapper(project) {
    private lateinit var temperatureSlider: JSlider
    private val toolCheckboxes = mutableMapOf<String, JBCheckBox>()
    private lateinit var promptField: MarkdownLanguageField

    init {
        title = "Chatbot Configuration"
        
        // Add all tools to the enabled tools list by default
        allTools.forEach { (serverName, tools) ->
            tools.forEach { tool ->
                val toolId = "${serverName}:${tool.name}"
                if (!config.enabledTools.contains(toolId)) {
                    config.enabledTools.add(toolId)
                }
            }
        }
        
        init()
    }

    override fun createCenterPanel(): JComponent {
        val systemPrompt = """
            # Configuration Instructions

            - **Tools**: Select which tools the chatbot can use
            
            You can also add specific instructions for the chatbot here.
        """.trimIndent()

        return panel {
            group("Configure $chatbotName") {
                row {
                    label("Adjust settings for the selected chatbot").applyToComponent {
                        font = JBUI.Fonts.label(14.0f)
                        foreground = UIUtil.getContextHelpForeground()
                    }
                }

                row {
                    label("Temperature: ${String.format("%.1f", config.temperature)}")
                }
                row {
                    cell(JSlider(0, 10, (config.temperature * 10).toInt()).apply {
                        temperatureSlider = this
                        background = UIUtil.getPanelBackground()
                        addChangeListener {
                            val value = temperatureSlider.value / 10.0
                            config.temperature = value
                        }
                    })
                }
                row {
                    comment("Lower values produce more focused outputs. Higher values produce more creative outputs.")
                }
                group("Enabled Tools") {
                    allTools.forEach { (serverName, tools) ->
                        tools.forEach { tool ->
                            val toolId = "${serverName}:${tool.name}"
                            row {
                                label("${tool.name} (${serverName})")
                                checkBox("").apply {
                                    component.isSelected = config.enabledTools.contains(toolId)
                                    toolCheckboxes[toolId] = component
                                    component.addActionListener {
                                        if (component.isSelected) {
                                            if (!config.enabledTools.contains(toolId)) {
                                                config.enabledTools.add(toolId)
                                            }
                                        } else {
                                            config.enabledTools.remove(toolId)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.topGap(TopGap.MEDIUM)

                group("Instructions") {
                    row {
                        cell(MarkdownLanguageField(project, systemPrompt, "Enter instructions for the chatbot", "instructions.md").apply {
                            promptField = this
                        }).resizableColumn().align(com.intellij.ui.dsl.builder.Align.FILL)
                    }
                }.topGap(TopGap.MEDIUM)
            }
        }.withPreferredSize(500, 600)
    }

    fun getConfig(): McpLlmConfig {
        config.systemPrompt = promptField.text
        return config
    }
}

