package cc.unitmesh.devti.settings.coder

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.schema.INLAY_PROMPTS_FILE_NAME
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.gui.component.JsonLanguageField
import cc.unitmesh.devti.settings.ResponseType
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import com.intellij.util.containers.toArray
import javax.swing.JCheckBox
import javax.swing.JPasswordField
import javax.swing.JTextField

class AutoDevCoderConfigurable(project: Project) : BoundConfigurable(AutoDevBundle.message("settings.autodev.coder")) {
    private val recordingInLocalCheckBox = JCheckBox()
    private val disableAdvanceContextCheckBox = JCheckBox().apply {
        toolTipText = AutoDevBundle.message("settings.autodev.coder.disableAdvanceContext.tips")
    }
    private val inEditorCompletionCheckBox = JCheckBox()
    private val noChatHistoryCheckBox = JCheckBox()

    private val explainCodeField = JTextField()
    private val refactorCodeField = JTextField()
    private val fixIssueCodeField = JTextField()

    private val useCustomAIEngineWhenInlayCodeComplete = JCheckBox()
        .apply {
            toolTipText = "You can use custom LLM to inlay complete code."
        }
    private val maxTokenLengthParam = JTextField()
    private val delaySecondsParam: JTextField = JTextField()
    private val customEngineResponseTypeParam: ComboBox<String> = ComboBox(ResponseType.values().map { it.name }.toArray(emptyArray()));
    private val customEngineResponseFormatParam = JTextField()
    private val customEngineRequestBodyFormatParam = JTextField()
    private val customEngineServerParam = JTextField()
    private val customEngineTokenParam = JPasswordField()
    private val customEnginePrompt = JsonLanguageField(project, "", "Custom your prompt here",  INLAY_PROMPTS_FILE_NAME)

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

        row(AutoDevBundle.message("settings.autodev.coder.noChatHistory")) {
            fullWidthCell(noChatHistoryCheckBox)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::noChatHistory.toMutableProperty()
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

        row(AutoDevBundle.message("settings.autodev.coder.useCustomerAgentWhenInlayCodeComplete")) {
            fullWidthCell(useCustomAIEngineWhenInlayCodeComplete)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::useCustomAIEngineWhenInlayCodeComplete.toMutableProperty()
                )
        }

        row(AutoDevBundle.message("settings.autodev.coder.delaySecondsParam")) {
            fullWidthCell(delaySecondsParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::delaySecondsParam.toMutableProperty()
                )
        }

        row(AutoDevBundle.message("settings.autodev.coder.maxTokenLengthParam")) {
            fullWidthCell(maxTokenLengthParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::maxTokenLengthParam.toMutableProperty()
                )
        }

        row(AutoDevBundle.message("settings.autodev.coder.customEngineResponseTypeParam")) {
            fullWidthCell(customEngineResponseTypeParam)
                .bind(
                    componentGet = { it.selectedItem?.toString() ?: ResponseType.SSE.name },
                    componentSet = { component, value -> component.selectedItem = value },
                    prop = state::customEngineResponseTypeParam.toMutableProperty()
                )
        }

        row(AutoDevBundle.message("settings.autodev.coder.customEngineResponseFormatParam")) {
            fullWidthCell(customEngineResponseFormatParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::customEngineResponseFormatParam.toMutableProperty()
                )
        }

        row(AutoDevBundle.message("settings.autodev.coder.customEngineRequestBodyFormatParam")) {
            fullWidthCell(customEngineRequestBodyFormatParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::customEngineRequestBodyFormatParam.toMutableProperty()
                )
        }

        row(AutoDevBundle.message("settings.autodev.coder.customEngineServerParam")) {
            fullWidthCell(customEngineServerParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::customEngineServerParam.toMutableProperty()
                )
        }
        row(AutoDevBundle.message("settings.autodev.coder.customEngineTokenParam")) {
            fullWidthCell(customEngineTokenParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::customEngineTokenParam.toMutableProperty()
                )
        }
        row(AutoDevBundle.message("settings.autodev.coder.customEnginePrompt")){}
        row() {
            fullWidthCell(customEnginePrompt)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::customEnginePrompt.toMutableProperty()
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
                it.useCustomAIEngineWhenInlayCodeComplete = state.useCustomAIEngineWhenInlayCodeComplete
                it.delaySecondsParam = state.delaySecondsParam
                it.maxTokenLengthParam = state.maxTokenLengthParam
                it.customEngineResponseTypeParam = state.customEngineResponseTypeParam
                it.customEngineResponseFormatParam = state.customEngineResponseFormatParam
                it.customEngineRequestBodyFormatParam = state.customEngineRequestBodyFormatParam
                it.customEngineServerParam = state.customEngineServerParam
                it.customEngineTokenParam = state.customEngineTokenParam
                it.customEnginePrompt = state.customEnginePrompt
                it.noChatHistory = state.noChatHistory
            }
        }
    }
}

