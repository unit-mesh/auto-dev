package cc.unitmesh.devti.settings.customize

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.schema.CUSTOM_AGENT_FILE_NAME
import cc.unitmesh.devti.fullHeight
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.gui.component.JsonLanguageField
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.componentStateChanged
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.jBLabel
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.placeholder
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*

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
            val customPrompt = JsonLanguageField(
                project,
                state::customPrompts.toString(),
                AutoDevBundle.messageWithLanguageFromLLMSetting("autodev.custom.prompt.placeholder"),
                CUSTOM_AGENT_FILE_NAME
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

            link(AutoDevBundle.message("open documents"), {
                BrowserUtil.browse("https://ide.unitmesh.cc/agent/custom-ai-agent")
            })
        }

        row {
            val languageField = JsonLanguageField(
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

        onApply {
            settings.modify {
                it.enableCustomRag = state.enableCustomRag
                it.agentJsonConfig = state.agentJsonConfig
                it.customPrompts = state.customPrompts
            }
        }
    }

    override fun dispose() {

    }
}
