package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.mcp.ui.model.McpChatConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JTextArea

/**
 * Simplified MCP Chat Configuration Dialog with JSON editing
 * 
 * Community best practice: Direct JSON editing for configuration
 * - Easy to copy/paste configurations
 * - Version control friendly
 * - Clear and explicit
 * - No complex UI to maintain
 */
class McpChatConfigDialog(
    private val project: Project,
    private val config: McpChatConfig,
    private val allTools: Map<String, List<Tool>>
) : DialogWrapper(project) {
    
    private lateinit var jsonEditor: JTextArea
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    @Serializable
    data class ChatConfigJson(
        val temperature: Double = 0.7,
        val enabledTools: List<String> = emptyList(),
        val systemPrompt: String = ""
    )
    
    init {
        title = AutoDevBundle.message("mcp.chat.config.dialog.title")
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label("Edit chat configuration in JSON format:")
            }
            row {
                val textArea = JTextArea().apply {
                    jsonEditor = this
                    lineWrap = true
                    wrapStyleWord = true
                    tabSize = 2
                    
                    // Load initial configuration
                    text = serializeChatConfig()
                }
                
                cell(JBScrollPane(textArea))
                    .resizableColumn()
                    .applyToComponent {
                        preferredSize = Dimension(600, 400)
                    }
            }
            row {
                comment("""
                    Example configuration:
                    {
                      "temperature": 0.7,
                      "enabledTools": ["tool1", "tool2"],
                      "systemPrompt": "You are a helpful assistant..."
                    }
                    
                    Available tools: ${allTools.values.flatten().joinToString(", ") { it.name }}
                """.trimIndent())
            }
        }.withPreferredSize(650, 500)
    }
    
    override fun doOKAction() {
        try {
            // Parse and validate JSON
            val chatConfigJson = json.decodeFromString<ChatConfigJson>(jsonEditor.text)
            
            // Validate tools exist
            val availableToolNames = allTools.values.flatten().map { it.name }.toSet()
            val invalidTools = chatConfigJson.enabledTools.filter { it !in availableToolNames }
            
            if (invalidTools.isNotEmpty()) {
                Messages.showWarningDialog(
                    project,
                    "Warning: The following tools are not available: ${invalidTools.joinToString(", ")}\n" +
                            "They will be ignored.",
                    "Unknown Tools"
                )
            }
            
            // Update config
            config.temperature = chatConfigJson.temperature
            config.systemPrompt = chatConfigJson.systemPrompt
            
            // Update enabled tools
            config.enabledTools.clear()
            allTools.values.flatten().forEach { tool ->
                if (tool.name in chatConfigJson.enabledTools) {
                    config.enabledTools.add(tool)
                }
            }
            
            super.doOKAction()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Invalid JSON format: ${e.message}",
                "Parse Error"
            )
        }
    }
    
    private fun serializeChatConfig(): String {
        val configJson = ChatConfigJson(
            temperature = config.temperature,
            enabledTools = config.enabledTools.map { it.name },
            systemPrompt = config.systemPrompt ?: config.createSystemPrompt()
        )
        return json.encodeToString(configJson)
    }
    
    fun getConfig(): McpChatConfig {
        return config
    }
}
