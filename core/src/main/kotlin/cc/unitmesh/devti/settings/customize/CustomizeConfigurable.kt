package cc.unitmesh.devti.settings.customize

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.schema.CUSTOM_AGENT_FILE_NAME
import cc.unitmesh.devti.custom.schema.CUSTOM_PROMPTS_FILE_NAME
import cc.unitmesh.devti.mcp.schema.MCP_SERVERS_FILE_NAME
import cc.unitmesh.devti.fullHeight
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.mcp.client.McpServicesTestDialog
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
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty

class CustomizeConfigurable(val project: Project) : BoundConfigurable(AutoDevBundle.message("settings.customize.title")),
    Disposable {
    val settings = project.service<AutoDevCustomizeSettings>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row {
            cell(jBLabel("settings.autodev.coder.customActions", 1))

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
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::customPrompts.toMutableProperty()
                )
        }

        row {
            checkBox(AutoDevBundle.message("counit.agent.enable.label")).bindSelected(state::enableCustomAgent)
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
            link(AutoDevBundle.message("sketch.mcp.services.docs"), {
                BrowserUtil.browse("https://ide.unitmesh.cc/mcp")
            })
            button(AutoDevBundle.message("sketch.mcp.testMcp")) {
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
                it.enableCustomAgent = state.enableCustomAgent
                it.agentJsonConfig = state.agentJsonConfig
                it.customPrompts = state.customPrompts
                it.mcpServerConfig = state.mcpServerConfig
            }
        }
    }

    override fun dispose() {

    }
}
