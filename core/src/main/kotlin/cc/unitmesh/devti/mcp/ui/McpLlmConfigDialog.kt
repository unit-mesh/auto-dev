package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.AutoDevSnippetFile
import cc.unitmesh.devti.sketch.ui.code.EditorUtil
import cc.unitmesh.devti.sketch.ui.code.findDocument
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
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.testFramework.LightVirtualFile

class McpLlmConfigDialog(
    private val project: Project,
    private val config: McpLlmConfig,
    private val allTools: Map<String, List<Tool>>
) : DialogWrapper(project) {
    private lateinit var temperatureSlider: JSlider
    private val toolCheckboxes = mutableMapOf<String, JBCheckBox>()
    private var markdownEditor: EditorEx?

    init {
        title = "Chatbot Configuration"
        allTools.forEach { (serverName, tools) ->
            config.enabledTools.addAll(tools)
        }

        val language = CodeFence.findLanguage("Markdown")
        val systemPrompt = config.createSystemPrompt()
        val file = LightVirtualFile(AutoDevSnippetFile.naming("md"), language, systemPrompt)
        markdownEditor = try {
            val document: Document = file.findDocument() ?: throw IllegalStateException("Document not found")
            EditorFactory.getInstance().createEditor(document, project, EditorKind.MAIN_EDITOR) as? EditorEx
        } catch (e: Throwable) {
            throw e
        }

        if (markdownEditor != null) {
            EditorUtil.configEditor(markdownEditor!!, project, file, false)
        }

        init()
    }

    /**
     * Based on https://github.com/jujumilk3/leaked-system-prompts/blob/main/anthropic-claude-api-tool-use_20250119.md
     */
    override fun createCenterPanel(): JComponent {
        val language = CodeFence.findLanguage("markdown")

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
            if (markdownEditor != null) {
                row {
                    cell(markdownEditor!!.component)
                        .resizableColumn()
                        .applyToComponent {
                            preferredSize = JBUI.size(500, 320)
                        }
                }
            }
        }.withPreferredSize(500, 600)
    }

    fun getConfig(): McpLlmConfig {
        config.systemPrompt = config.createSystemPrompt()
        return config
    }
}
