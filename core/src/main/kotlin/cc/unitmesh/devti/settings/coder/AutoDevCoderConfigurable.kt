package cc.unitmesh.devti.settings.coder

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.schema.INLAY_PROMPTS_FILE_NAME
import cc.unitmesh.devti.fullWidthCell
import cc.unitmesh.devti.gui.component.JsonLanguageField
import cc.unitmesh.devti.settings.LanguageChangedCallback.placeholder
import cc.unitmesh.devti.settings.LanguageChangedCallback.jLabel
import cc.unitmesh.devti.settings.LanguageChangedCallback.tips
import cc.unitmesh.devti.settings.ResponseType
import cc.unitmesh.devti.settings.testLLMConnection
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import com.intellij.util.containers.toArray
import javax.swing.JCheckBox
import javax.swing.JPasswordField
import javax.swing.JTextField

class AutoDevCoderConfigurable(private val project: Project) : BoundConfigurable(AutoDevBundle.message("settings.autodev.coder")) {
    private val recordingInLocalCheckBox = JCheckBox()
    private val disableAdvanceContextCheckBox = JCheckBox().apply {
        tips("settings.autodev.coder.disableAdvanceContext.tips", this)
    }
    private val inEditorCompletionCheckBox = JCheckBox()
    private val noChatHistoryCheckBox = JCheckBox()

    private val useCustomAIEngineWhenInlayCodeComplete = JCheckBox()
        .apply {
            tips("settings.autodev.coder.useCustomAIEngineWhenInlayCodeComplete.tips", this)
        }
    private val maxTokenLengthParam = JTextField()
    private val delaySecondsParam: JTextField = JTextField()
    private val customEngineResponseTypeParam: ComboBox<String> = ComboBox(ResponseType.values().map { it.name }.toArray(emptyArray()));
    private val customEngineResponseFormatParam = JTextField()
    private val customEngineRequestBodyFormatParam = JTextField()
    private val customEngineServerParam = JTextField()
    private val customEngineTokenParam = JPasswordField()
    private val customEnginePrompt = JsonLanguageField(project, "", AutoDevBundle.messageWithLanguageFromLLMSetting("autodev.custom.prompt.placeholder"),  INLAY_PROMPTS_FILE_NAME)
        .apply { placeholder("autodev.custom.prompt.placeholder", this, 2) }

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

        row(jLabel("settings.autodev.coder.useCustomAIEngineWhenInlayCodeComplete")) {
            fullWidthCell(useCustomAIEngineWhenInlayCodeComplete)
                .bind(
                    componentGet = { it.isSelected },
                    componentSet = { component, value -> component.isSelected = value },
                    prop = state::useCustomAIEngineWhenInlayCodeComplete.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.delaySecondsParam")) {
            fullWidthCell(delaySecondsParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::delaySecondsParam.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.maxTokenLengthParam")) {
            fullWidthCell(maxTokenLengthParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::maxTokenLengthParam.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.customEngineResponseTypeParam")) {
            fullWidthCell(customEngineResponseTypeParam)
                .bind(
                    componentGet = { it.selectedItem?.toString() ?: ResponseType.SSE.name },
                    componentSet = { component, value -> component.selectedItem = value },
                    prop = state::customEngineResponseTypeParam.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.customEngineResponseFormatParam")) {
            fullWidthCell(customEngineResponseFormatParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::customEngineResponseFormatParam.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.customEngineRequestBodyFormatParam")) {
            fullWidthCell(customEngineRequestBodyFormatParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::customEngineRequestBodyFormatParam.toMutableProperty()
                )
        }

        row(jLabel("settings.autodev.coder.customEngineServerParam")) {
            fullWidthCell(customEngineServerParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::customEngineServerParam.toMutableProperty()
                )
        }
        row(jLabel("settings.autodev.coder.customEngineTokenParam")) {
            fullWidthCell(customEngineTokenParam)
                .bind(
                    componentGet = { it.text },
                    componentSet = { component, value -> component.text = value },
                    prop = state::customEngineTokenParam.toMutableProperty()
                )
        }

        if (project != null) {
            testLLMConnection(project)
            row {
                text(AutoDevBundle.message("settings.autodev.coder.testConnectionButton.tips")).apply {
                    this.component.foreground = JBColor.RED
                }
            }
        }

        row(jLabel("settings.autodev.coder.customEnginePrompt", 2)) {}
        row {
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
                it.enableRenameSuggestion = state.enableRenameSuggestion
            }
        }
    }
}

