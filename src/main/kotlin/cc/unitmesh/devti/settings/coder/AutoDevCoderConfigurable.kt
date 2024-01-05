package cc.unitmesh.devti.settings.coder

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.fullWidthCell
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import javax.swing.JCheckBox
import javax.swing.JTextField

class AutoDevCoderConfigurable(project: Project) : BoundConfigurable(AutoDevBundle.message("settings.autodev.coder")) {
    private val recordingInLocalCheckBox = JCheckBox()
    private val disableAdvanceContextCheckBox = JCheckBox()
    private val inEditorCompletionCheckBox = JCheckBox()
    private val explainCodeField = JTextField()
    private val refactorCodeField = JTextField()
    private val fixIssueCodeField = JTextField()
    private val generateTestField = JTextField()

    val settings = project.service<AutoDevCoderSettingService>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row(AutoDevBundle.message("settings.autodev.coder.recordingInLocal")) {
            fullWidthCell(recordingInLocalCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::recordingInLocal.toMutableProperty()
                )
        }

        row(AutoDevBundle.message("settings.autodev.coder.disableAdvanceContext")) {
            fullWidthCell(disableAdvanceContextCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::disableAdvanceContext.toMutableProperty()
                )
        }
        row(AutoDevBundle.message("settings.autodev.coder.inEditorCompletion")) {
            fullWidthCell(inEditorCompletionCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::inEditorCompletion.toMutableProperty()
                )
        }
        row(AutoDevBundle.message("settings.autodev.coder.explainCode")) {
            fullWidthCell(explainCodeField)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::explainCode.toMutableProperty()
                )
        }
        row(AutoDevBundle.message("settings.autodev.coder.refactorCode")) {
            fullWidthCell(refactorCodeField)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::refactorCode.toMutableProperty()
                )
        }
        row(AutoDevBundle.message("settings.autodev.coder.fixIssueCode")) {
            fullWidthCell(fixIssueCodeField)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::fixIssueCode.toMutableProperty()
                )
        }
        row(AutoDevBundle.message("settings.autodev.coder.generateTest")) {
            fullWidthCell(generateTestField)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::generateTest.toMutableProperty()
                )
        }

        onApply {
            settings.modify {
                it.recordingInLocal = state.recordingInLocal
                it.disableAdvanceContext = state.disableAdvanceContext
                it.inEditorCompletion = state.inEditorCompletion
                it.explainCode = state.explainCode
                it.refactorCode = state.refactorCode
                it.fixIssueCode = state.fixIssueCode
                it.generateTest = state.generateTest
            }
        }
    }
}

