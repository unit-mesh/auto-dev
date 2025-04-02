package cc.unitmesh.devti.settings.customize

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.schema.CUSTOM_AGENT_FILE_NAME
import cc.unitmesh.devti.custom.schema.CUSTOM_PROMPTS_FILE_NAME
import cc.unitmesh.devti.custom.schema.MCP_SERVERS_FILE_NAME
import cc.unitmesh.devti.fullHeight
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import cc.unitmesh.devti.provider.local.JsonTextProvider
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.componentStateChanged
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.jBLabel
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.placeholder
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import com.intellij.ui.table.JBTable
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.table.DefaultTableModel

class CustomizeConfigurable(val project: Project) : BoundConfigurable(AutoDevBundle.message("customize.title")),
    Disposable {
    val settings = project.service<AutoDevCustomizeSettings>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row {
            cell(jBLabel("settings.autodev.coder.customEnginePrompt", 1))

            link(AutoDevBundle.message("custom.action"), {
                BrowserUtil.browse("https://ide.unitmesh.cc/custom/action")
            })
            link(AutoDevBundle.message("custom.living.documentation"), {
                BrowserUtil.browse("https://ide.unitmesh.cc/custom/living-documentation")
            })
        }
        row {
            val customPrompt = JsonTextProvider.create(
                project,
                state::customPrompts.toString(),
                AutoDevBundle.messageWithLanguageFromLLMSetting("autodev.custom.prompt.placeholder"),
                CUSTOM_PROMPTS_FILE_NAME
            ).apply {
                placeholder("autodev.custom.prompt.placeholder", this, 1)
            }

            fullWidthCell(customPrompt)
                .fullHeight()
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::customPrompts.toMutableProperty()
                )
        }

        row {
            checkBox(AutoDevBundle.message("counit.agent.enable.label")).bindSelected(state::enableCustomRag)
                .apply {
                    componentStateChanged("counit.agent.enable.label", this.component) { c, k ->
                        c.text = k
                    }
                }

            link(AutoDevBundle.message("custom.agent.open.documents"), {
                BrowserUtil.browse("https://ide.unitmesh.cc/agent/custom-ai-agent")
            })
        }

        row {
            val languageField = JsonTextProvider.create(
                project,
                state::agentJsonConfig.toString(),
                AutoDevBundle.messageWithLanguageFromLLMSetting("counit.agent.json.placeholder"),
                CUSTOM_AGENT_FILE_NAME
            ).apply {
                placeholder("counit.agent.json.placeholder", this)
            }
            fullWidthCell(languageField)
                .fullHeight()
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::agentJsonConfig.toMutableProperty()
                )
        }
        row {
            cell(jBLabel("counit.mcp.services.placeholder", 1))
            
            button(AutoDevBundle.message("settings.autodev.coder.testConnectionButton.tips")) {
                val dialog = McpServicesTestDialog(project)
                dialog.show()
            }
        }

        row {
            val mcpServices = JsonTextProvider.create(
                project,
                state::mcpServerConfig.toString(),
                AutoDevBundle.messageWithLanguageFromLLMSetting("counit.mcp.services.placeholder"),
                MCP_SERVERS_FILE_NAME
            ).apply {
                placeholder("counit.mcp.services.placeholder", this)
            }
            fullWidthCell(mcpServices)
                .fullHeight()
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::mcpServerConfig.toMutableProperty()
                )
        }

        onApply {
            settings.modify {
                it.enableCustomRag = state.enableCustomRag
                it.agentJsonConfig = state.agentJsonConfig
                it.customPrompts = state.customPrompts
                it.mcpServerConfig = state.mcpServerConfig
            }
        }
    }

    class McpServicesTestDialog(private val project: Project) : DialogWrapper(project) {
        private val loadingPanel = JBLoadingPanel(BorderLayout(), this.disposable)
        private val tableModel = DefaultTableModel(arrayOf("Server", "Tool Name", "Description"), 0)
        private val table = JBTable(tableModel)
        
        init {
            title = "MCP Services Test Results"
            init()
            loadServices()
        }
        
        override fun createCenterPanel(): JComponent {
            loadingPanel.add(table, BorderLayout.CENTER)
            loadingPanel.setLoadingText("Loading MCP services...")
            return loadingPanel
        }
        
        private fun loadServices() {
            loadingPanel.startLoading()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val serverManager = CustomMcpServerManager.instance(project)
                    val serverInfos = serverManager.collectServerInfos()
                    
                    withContext(Dispatchers.IO) {
                        updateTable(serverInfos)
                        loadingPanel.stopLoading()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.IO) {
                        tableModel.addRow(arrayOf("Error", e.message, ""))
                        loadingPanel.stopLoading()
                    }
                }
            }
        }
        
        private fun updateTable(serverInfos: Map<String, List<Tool>>) {
            tableModel.rowCount = 0
            
            if (serverInfos.isEmpty()) {
                tableModel.addRow(arrayOf("No servers found", "", ""))
                return
            }
            
            serverInfos.forEach { (server, tools) ->
                if (tools.isEmpty()) {
                    tableModel.addRow(arrayOf(server, "No tools found", ""))
                } else {
                    tools.forEach { tool ->
                        tableModel.addRow(arrayOf(server, tool.name, tool.description))
                    }
                }
            }
        }
    }

    override fun dispose() {

    }
}
