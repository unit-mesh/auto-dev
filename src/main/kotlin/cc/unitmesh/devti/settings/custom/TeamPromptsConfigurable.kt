package cc.unitmesh.devti.settings.custom

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.fullWidthCell
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import javax.swing.JCheckBox
import javax.swing.JTextField

class PromptLibraryConfigurable(project: Project) : BoundConfigurable(AutoDevBundle.message("settings.external.team.prompts.name")) {

    private val teamPromptsField = JTextField()
    private val recordingInLocalField = JCheckBox()
    private val disableAdvanceContextField = JCheckBox()

    val settings = project.service<TeamPromptsProjectSettingsService>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row(AutoDevBundle.message("settings.external.team.prompts.path")) {
            // TODO: spike better way for support 213 and 221
            fullWidthCell(teamPromptsField)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::teamPromptsDir.toMutableProperty()
                )
        }
        row(AutoDevBundle.message("settings.external.team.prompts.recordingInLocal")) {
            fullWidthCell(recordingInLocalField)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::recordingInLocal.toMutableProperty()
                )
        }

        row(AutoDevBundle.message("settings.external.team.prompts.disableAdvanceContext")) {
            fullWidthCell(disableAdvanceContextField)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::disableAdvanceContext.toMutableProperty()
                )
        }

        onApply {
            settings.modify {
                it.teamPromptsDir = state.teamPromptsDir
                it.recordingInLocal = state.recordingInLocal
                it.disableAdvanceContext
            }
        }
    }
}

