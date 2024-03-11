package cc.unitmesh.devti.agent.configurable

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.gui.component.JsonLanguageField
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
        }

        row {
            val languageField = JsonLanguageField(
                project,
                state::agentJsonConfig.toString(),
                AutoDevBundle.message("counit.agent.json.placeholder")
            )
            fullWidthCell(languageField)
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
