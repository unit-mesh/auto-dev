package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.agent.mcp.McpConfig
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.devins.ui.config.ConfigManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JTextArea

/**
 * Simplified MCP configuration editor that supports JSON editing
 * 
 * This replaces the complex McpChatConfigDialog with a simpler JSON-based editor.
 * Users can directly edit MCP server configurations in JSON format.
 * 
 * Example JSON:
 * ```json
 * {
 *   "mcpServers": {
 *     "AutoDev": {
 *       "command": "npx",
 *       "args": ["-y", "@jetbrains/mcp-proxy"],
 *       "disabled": false,
 *       "autoApprove": []
 *     }
 *   }
 * }
 * ```
 */
class McpConfigEditor(
    private val project: Project,
    private val initialConfig: Map<String, McpServerConfig>? = null
) : DialogWrapper(project) {
    
    private lateinit var jsonEditor: JTextArea
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    init {
        title = "MCP Servers Configuration"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label("Edit MCP server configurations in JSON format:")
            }
            row {
                val textArea = JTextArea().apply {
                    jsonEditor = this
                    lineWrap = true
                    wrapStyleWord = true
                    tabSize = 2
                    
                    // Load initial configuration
                    text = if (initialConfig != null && initialConfig.isNotEmpty()) {
                        json.encodeToString(McpConfig(mcpServers = initialConfig))
                    } else {
                        getDefaultTemplate()
                    }
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
                      "mcpServers": {
                        "AutoDev": {
                          "command": "npx",
                          "args": ["-y", "@jetbrains/mcp-proxy"],
                          "disabled": false,
                          "autoApprove": []
                        }
                      }
                    }
                """.trimIndent())
            }
        }.withPreferredSize(650, 500)
    }
    
    override fun doOKAction() {
        try {
            // Parse and validate JSON
            val mcpConfig = json.decodeFromString<McpConfig>(jsonEditor.text)
            
            // Validate each server configuration
            val invalidServers = mcpConfig.mcpServers.filter { !it.value.validate() }
            if (invalidServers.isNotEmpty()) {
                Messages.showErrorDialog(
                    project,
                    "Invalid server configurations: ${invalidServers.keys.joinToString(", ")}\n" +
                            "Each server must have either 'command' or 'url', but not both.",
                    "Validation Error"
                )
                return
            }
            
            // Save to ConfigManager
            runBlocking {
                try {
                    ConfigManager.saveMcpServers(mcpConfig.mcpServers)
                    Messages.showInfoMessage(
                        project,
                        "MCP configuration saved successfully!\n" +
                                "Configuration saved to: ${ConfigManager.getConfigPath()}",
                        "Success"
                    )
                } catch (e: Exception) {
                    Messages.showErrorDialog(
                        project,
                        "Failed to save configuration: ${e.message}",
                        "Error"
                    )
                    return@runBlocking
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
    
    /**
     * Get the parsed MCP configuration
     */
    fun getMcpConfig(): Map<String, McpServerConfig>? {
        return try {
            val mcpConfig = json.decodeFromString<McpConfig>(jsonEditor.text)
            mcpConfig.mcpServers
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getDefaultTemplate(): String {
        val defaultConfig = McpConfig(
            mcpServers = mapOf(
                "AutoDev" to McpServerConfig(
                    command = "npx",
                    args = listOf("-y", "@jetbrains/mcp-proxy"),
                    disabled = false,
                    autoApprove = emptyList()
                )
            )
        )
        return json.encodeToString(defaultConfig)
    }
    
    companion object {
        /**
         * Show the MCP configuration editor
         * 
         * @param project The current project
         * @return The configured MCP servers, or null if cancelled
         */
        fun show(project: Project): Map<String, McpServerConfig>? {
            // Load existing configuration
            val existingConfig = runBlocking {
                try {
                    val wrapper = ConfigManager.load()
                    wrapper.getEnabledMcpServers()
                } catch (e: Exception) {
                    emptyMap()
                }
            }
            
            val dialog = McpConfigEditor(project, existingConfig)
            return if (dialog.showAndGet()) {
                dialog.getMcpConfig()
            } else {
                null
            }
        }
    }
}

