package cc.unitmesh.devti.agent.configurable

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.schema.CUSTOM_AGENT_FILE_NAME
import cc.unitmesh.devti.fullHeight
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.gui.component.JsonLanguageField
import cc.unitmesh.devti.settings.LanguageChangedCallback.componentStateChanged
import cc.unitmesh.devti.settings.LanguageChangedCallback.placeholder
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*

class CoUnitToolConfigurable(val project: Project) : BoundConfigurable(AutoDevBundle.message("counit.agent.name")), Disposable {
    val settings = project.service<CoUnitProjectSettingsService>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row {
            checkBox(AutoDevBundle.message("counit.agent.enable.label")).bindSelected(state::enableCustomRag)
                .apply { componentStateChanged("counit.agent.enable.label", this.component){ c,k ->
                    c.text = k} }

            link(AutoDevBundle.message("open documents"), {
                com.intellij.ide.BrowserUtil.browse("https://ide.unitmesh.cc/agent/custom-ai-agent")
            });
        }

        row {
            val languageField = JsonLanguageField(
                project,
                state::agentJsonConfig.toString(),
                AutoDevBundle.messageWithLanguageFromLLMSetting("counit.agent.json.placeholder"),
                CUSTOM_AGENT_FILE_NAME
            )
                .apply {
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
            }
        }
    }

    override fun dispose() {

    }
}
