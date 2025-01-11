package cc.unitmesh.devti.settings

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.schema.CUSTOM_AGENT_FILE_NAME
import cc.unitmesh.devti.gui.component.JsonLanguageField
import cc.unitmesh.devti.settings.LanguageChangedCallback.jBLabel
import com.intellij.ide.actions.RevealFileAction
import com.intellij.idea.LoggerFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class LLMSettingComponent(private val settings: AutoDevSettingsState) {
    // 以下 LLMParam 变量不要改名，因为这些变量名会被用作配置文件的 key
    private val languageParam by LLMParam.creating({ LanguageChangedCallback.language = it }) {
        ComboBox(settings.language, HUMAN_LANGUAGES.values().map { it.display })
    }

    private val delaySecondsParam by LLMParam.creating { Editable(settings.delaySeconds) }
    private val maxTokenLengthParam by LLMParam.creating { Editable(settings.maxTokenLength) }
    private val customModelParam: LLMParam by LLMParam.creating { Editable(settings.customModel) }
    private val customOpenAIHostParam: LLMParam by LLMParam.creating { Editable(settings.customOpenAiHost) }

    private val customEngineServerParam by LLMParam.creating { Editable(settings.customEngineServer) }
    private val customEngineTokenParam by LLMParam.creating { Password(settings.customEngineToken) }

    private val customEngineResponseFormatParam by LLMParam.creating { Editable(settings.customEngineResponseFormat) }
    private val customEngineRequestBodyFormatParam by LLMParam.creating { Editable(settings.customEngineRequestFormat) }

    val project = ProjectManager.getInstance().openProjects.firstOrNull()
    private val customEnginePrompt: EditorTextField by lazy {
        JsonLanguageField(
            project,
            settings.customPrompts,
            AutoDevBundle.messageWithLanguageFromLLMSetting("autodev.custom.prompt.placeholder"),
            CUSTOM_AGENT_FILE_NAME
        ).apply {
            LanguageChangedCallback.placeholder("autodev.custom.prompt.placeholder", this, 1)
        }
    }

    private val currentLLMParams: List<LLMParam>
        get() {
            return listOf(
                customEngineServerParam,
                customEngineTokenParam,
                customEngineResponseFormatParam,
                customEngineRequestBodyFormatParam,
            )
        }

    private fun FormBuilder.addLLMParams(llmParams: List<LLMParam>): FormBuilder = apply {
        llmParams.forEach { addLLMParam(it) }
    }

    private fun FormBuilder.addLLMParam(llmParam: LLMParam): FormBuilder = apply {
        llmParam.addToFormBuilder(this)
    }

    private fun LLMParam.addToFormBuilder(formBuilder: FormBuilder) {
        when (this.type) {
            LLMParam.ParamType.Password -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactivePasswordField(this) {
                    this.text = it.value
                    this.isEnabled = it.isEditable
                }, 1, false)
            }

            LLMParam.ParamType.Text -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactiveTextField(this) {
                    this.isEnabled = it.isEditable
                }, 1, false)
            }

            LLMParam.ParamType.ComboBox -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactiveComboBox(this), 1, false)
            }

            else -> {
                formBuilder.addSeparator()
            }
        }
    }

    private val formBuilder: FormBuilder = FormBuilder.createFormBuilder()
    val panel: JPanel get() = formBuilder.panel


    fun applySettings(settings: AutoDevSettingsState, updateParams: Boolean = false) {
        panel.removeAll()

        formBuilder
            .addLLMParam(languageParam)
            .addSeparator()
            .addLLMParams(currentLLMParams)
            .addLLMParam(maxTokenLengthParam)
            .addLLMParam(delaySecondsParam)
            .addComponent(panel {
                if (project != null) {
                    testLLMConnection(project)
                }

                row {
                    text(AutoDevBundle.message("settings.autodev.coder.testConnectionButton.tips")).apply {
                        this.component.foreground = JBColor.RED
                    }
                }
            })
            .addSeparator()
            .addVerticalGap(2)
            .addLabeledComponent(jBLabel("settings.autodev.coder.customEnginePrompt", 1), customEnginePrompt, 1, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        panel.invalidate()
        panel.repaint()
    }

    private fun updateParams(settings: AutoDevSettingsState) {
        settings.apply {
            maxTokenLengthParam.value = maxTokenLength
            customModelParam.value = customModel
            customOpenAIHostParam.value = customOpenAiHost
            customEngineServerParam.value = customEngineServer
            customEngineTokenParam.value = customEngineToken
            languageParam.value = language
            customEnginePrompt.text = customPrompts
            customEngineResponseFormatParam.value = customEngineResponseFormat
            customEngineRequestBodyFormatParam.value = customEngineRequestFormat
            delaySecondsParam.value = delaySeconds
        }
    }

    fun exportSettings(destination: AutoDevSettingsState) {
        destination.apply {
            maxTokenLength = maxTokenLengthParam.value
            customModel = customModelParam.value
            customOpenAiHost = customOpenAIHostParam.value
            language = languageParam.value
            customEngineServer = customEngineServerParam.value
            customEngineToken = customEngineTokenParam.value
            customPrompts = customEnginePrompt.text
            customEngineResponseFormat = customEngineResponseFormatParam.value
            customEngineRequestFormat = customEngineRequestBodyFormatParam.value
            delaySeconds = delaySecondsParam.value
        }
    }

    fun isModified(settings: AutoDevSettingsState): Boolean {
        return settings.maxTokenLength != maxTokenLengthParam.value ||
                settings.customModel != customModelParam.value ||
                settings.language != languageParam.value ||
                settings.customEngineServer != customEngineServerParam.value ||
                settings.customEngineToken != customEngineTokenParam.value ||
                settings.customPrompts != customEnginePrompt.text ||
                settings.customOpenAiHost != customOpenAIHostParam.value ||
                settings.customEngineResponseFormat != customEngineResponseFormatParam.value ||
                settings.customEngineRequestFormat != customEngineRequestBodyFormatParam.value ||
                settings.delaySeconds != delaySecondsParam.value
    }

    init {
        applySettings(settings)
        LanguageChangedCallback.language = AutoDevSettingsState.getInstance().language
    }
}
