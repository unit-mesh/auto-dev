package cc.unitmesh.devti.settings.coder

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.jLabel
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.tips
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import javax.swing.JCheckBox
import javax.swing.JTextField

class AutoDevCoderConfigurable(private val project: Project) : BoundConfigurable(AutoDevBundle.message("settings.autodev.coder")) {
    private val recordingInLocalCheckBox = JCheckBox()
    private val disableAdvanceContextCheckBox = JCheckBox().apply {
        tips("settings.autodev.coder.disableAdvanceContext.tips", this)
    }
    private val inEditorCompletionCheckBox = JCheckBox()
    private val noChatHistoryCheckBox = JCheckBox()
    private val enableMcpServerCheckBox = JCheckBox()
    private val teamPromptsField = JTextField()
    private val trimCodeBeforeSend = JCheckBox()


    val settings = project.service<AutoDevCoderSettingService>()
    val state = settings.state.copy()

    override fun createPanel(): DialogPanel = panel {
        row(jLabel("settings.autodev.coder.recordingInLocal")) {
            fullWidthCell(recordingInLocalCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::recordingInLocal.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.disableAdvanceContext")) {
            fullWidthCell(disableAdvanceContextCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::disableAdvanceContext.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.noChatHistory")) {
            fullWidthCell(noChatHistoryCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::noChatHistory.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.enableMcpServer")) {
            fullWidthCell(enableMcpServerCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::enableMcpServer.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.trimCodeBeforeSend")) {
            fullWidthCell(trimCodeBeforeSend)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::trimCodeBeforeSend.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.inEditorCompletion")) {
            fullWidthCell(inEditorCompletionCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::inEditorCompletion.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.enableRenameSuggestion")) {
            fullWidthCell(JCheckBox())
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::enableRenameSuggestion.toMutableProperty()
                )
        }

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
                it.recordingInLocal = state.recordingInLocal
                it.disableAdvanceContext = state.disableAdvanceContext
                it.inEditorCompletion = state.inEditorCompletion
                it.noChatHistory = state.noChatHistory
                it.enableRenameSuggestion = state.enableRenameSuggestion
                it.trimCodeBeforeSend = state.trimCodeBeforeSend
                it.teamPromptsDir = state.teamPromptsDir
                it.enableMcpServer = state.enableMcpServer
            }
        }
    }
}

