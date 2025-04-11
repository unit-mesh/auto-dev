package cc.unitmesh.devti.mcp.editor

import cc.unitmesh.devti.gui.planner.MarkdownLanguageField
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align.Companion.FILL
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.sun.java.accessibility.util.AWTEventMonitor.addActionListener
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.swing.JComponent
import javax.swing.JSlider

data class McpLlmConfig(
    var temperature: Double = 0.0,
    var enabledTools: MutableList<Tool> = mutableListOf(),
    var systemPrompt: String = ""
) {
    fun createSystemPrompt(): String {
        val systemPrompt = """
In this environment you have access to a set of tools you can use to answer the user's question.
You can invoke functions by writing a "<devins:function_calls>" inside markdown code-block like the following as part of your reply to the user:

```xml
<devins:function_calls>
<devins:invoke name="${'$'}FUNCTION_NAME">
<devins:parameter name="${'$'}PARAMETER_NAME">${'$'}PARAMETER_VALUE</devins:parameter>
...
</devins:invoke>
<devins:invoke name="${'$'}FUNCTION_NAME2">
...
</devins:invoke>
</devins:function_calls>
```

String and scalar parameters should be specified as is, while lists and objects should use JSON format.

Here are the functions available in JSONSchema format:
<functions>
${enabledTools.joinToString("\n") { tool -> "<function>" + Json.encodeToString(tool) } + "</function>"} }
</functions>

Answer the user's request using the relevant tool(s), if they are available. Check that all the required parameters for each tool call are provided or can reasonably be inferred from context. IF there are no relevant tools or there are missing values for required parameters, ask the user to supply these values; otherwise proceed with the tool calls. If the user provides a specific value for a parameter (for example provided in quotes), make sure to use that value EXACTLY. DO NOT make up values for or ask about optional parameters. Carefully analyze descriptive terms in the request as they may indicate required parameter values that should be included even if not explicitly quoted.

If you intend to call multiple tools and there are no dependencies between the calls, make all of the independent calls in the same <devins:function_calls></devins:function_calls> block.
""".trimIndent()
        return systemPrompt
    }
}

class McpLlmConfigDialog(
    private val project: Project,
    private val config: McpLlmConfig,
    private val allTools: Map<String, List<Tool>>
) : DialogWrapper(project) {
    private lateinit var temperatureSlider: JSlider
    private val toolCheckboxes = mutableMapOf<String, JBCheckBox>()
    private lateinit var promptField: MarkdownLanguageField

    init {
        title = "Chatbot Configuration"
        allTools.forEach { (serverName, tools) ->
            config.enabledTools.addAll(tools)
        }

        init()
    }

    /**
     * Based on https://github.com/jujumilk3/leaked-system-prompts/blob/main/anthropic-claude-api-tool-use_20250119.md
     */
    override fun createCenterPanel(): JComponent {
        val systemPrompt = config.createSystemPrompt()
        promptField = MarkdownLanguageField(project, systemPrompt, "", "instructions.md").also {
            it.preferredSize = JBUI.size(480, 540)
        }

        return panel {
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
                        row {
                            label("${tool.name} (${serverName})")
                            checkBox("").apply {
                                toolCheckboxes[tool.name] = this.component
                                this.component.isSelected = config.enabledTools.any { it.name == tool.name }
                                addActionListener {
                                    if (this.component.isSelected) {
                                        config.enabledTools.add(tool)
                                    } else {
                                        config.enabledTools.remove(tool)
                                    }
                                }
                            }
                        }
                    }
                }
            }.topGap(TopGap.MEDIUM)
            group("System Prompt") {
                row {
                    cell(promptField)
                        .align(FILL)
                        .resizableColumn()
                }
            }.topGap(TopGap.MEDIUM)
        }.withPreferredSize(500, 600)
    }

    fun getConfig(): McpLlmConfig {
        config.systemPrompt = config.createSystemPrompt()
        return config
    }
}
