package cc.unitmesh.devti.settings.coder

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.fullWidthCell
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import javax.swing.JCheckBox

class AutoDevCoderConfigurable(project: Project) : BoundConfigurable(AutoDevBundle.message("settings.autodev.coder")) {
    private val recordingInLocalField = JCheckBox()
    private val disableAdvanceContextField = JCheckBox()

    val settings = project.service<AutoDevCoderSettingService>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
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
                it.recordingInLocal = state.recordingInLocal
                it.disableAdvanceContext
            }
        }
    }
}

