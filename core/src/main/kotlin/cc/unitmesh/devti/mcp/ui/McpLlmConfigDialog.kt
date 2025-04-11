package cc.unitmesh.devti.mcp.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align.Companion.FILL
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.sun.java.accessibility.util.AWTEventMonitor.addActionListener
import io.modelcontextprotocol.kotlin.sdk.Tool
import javax.swing.JComponent
import javax.swing.JSlider
import cc.unitmesh.devti.util.parser.CodeFence

class McpLlmConfigDialog(
    private val project: Project,
    private val config: McpLlmConfig,
    private val allTools: Map<String, List<Tool>>
) : DialogWrapper(project) {
    private lateinit var temperatureSlider: JSlider
    private val toolCheckboxes = mutableMapOf<String, JBCheckBox>()
    private lateinit var promptField: LanguageTextField

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
        val language = CodeFence.findLanguage("markdown")
        promptField = LanguageTextField(language, project, systemPrompt).also {
            it.preferredSize = JBUI.size(640, 320)
        }

        return panel {
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
