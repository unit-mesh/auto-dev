package cc.unitmesh.devti.settings.custom

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.settings.LanguageChangedCallback.jLabel
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import javax.swing.JTextField

class PromptLibraryConfigurable(project: Project) : BoundConfigurable(AutoDevBundle.message("settings.external.team.prompts.name")) {

    private val teamPromptsField = JTextField()

    val settings = project.service<TeamPromptsProjectSettingsService>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row(jLabel("settings.external.team.prompts.path")) {
            fullWidthCell(teamPromptsField)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::teamPromptsDir.toMutableProperty()
                )
        }

        onApply {
            settings.modify {
                it.teamPromptsDir = state.teamPromptsDir
            }
        }
    }
}

