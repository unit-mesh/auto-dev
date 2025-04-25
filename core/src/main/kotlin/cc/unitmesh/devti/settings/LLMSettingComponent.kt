package cc.unitmesh.devti.settings

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.custom.schema.AUTODEV_CUSTOM_LLM_FILE
import cc.unitmesh.devti.provider.local.JsonTextProvider
import cc.unitmesh.devti.settings.locale.HUMAN_LANGUAGES
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.jBLabel
import com.intellij.lang.Language
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class LLMSettingComponent(private val settings: AutoDevSettingsState) {
    private val languageParam by LLMParam.creating({ LanguageChangedCallback.language = it }) {
        ComboBox(settings.language, HUMAN_LANGUAGES.entries.map { it.display })
    }

    private val delaySecondsParam by LLMParam.creating { Editable(settings.delaySeconds) }
    private val maxTokenLengthParam by LLMParam.creating { Editable(settings.maxTokenLength) }
    private val customModelParam: LLMParam by LLMParam.creating { Editable(settings.customModel) }
    private val customOpenAIHostParam: LLMParam by LLMParam.creating { Editable(settings.customOpenAiHost) }

    private val customEngineServerParam by LLMParam.creating { Editable(settings.customEngineServer) }
    private val customEngineTokenParam by LLMParam.creating { Password(settings.customEngineToken) }

    private val customEngineResponseFormatParam by LLMParam.creating { JsonPathEditable(settings.customEngineResponseFormat) }
    private val customEngineRequestBodyFormatParam by LLMParam.creating { JsonEditable(settings.customEngineRequestFormat) }

    val project = ProjectManager.getInstance().openProjects.firstOrNull()
    private val customLlmParam: EditorTextField by lazy {
        JsonTextProvider.create(
            project,
            settings.customLlms,
            AutoDevBundle.messageWithLanguageFromLLMSetting("autodev.custom.llms.placeholder"),
            AUTODEV_CUSTOM_LLM_FILE
        ).apply {
            LanguageChangedCallback.placeholder("autodev.custom.llms.placeholder", this, 1)
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

            LLMParam.ParamType.JsonText -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), cc.unitmesh.devti.provider.local.JsonLanguageField(
                    project, this.value, AutoDevBundle.messageWithLanguageFromLLMSetting(this.label), null, true
                ).apply {
                    addDocumentListener(object: DocumentListener {
                        override fun documentChanged(event: DocumentEvent) {
                            this@addToFormBuilder.value = this@apply.document.text
                        }
                    })
                }, 1, false)
            }

            LLMParam.ParamType.JsonPath -> {
                val language = Language.findLanguageByID("JSONPath") ?: PlainTextLanguage.INSTANCE
                formBuilder.addLabeledComponent(jBLabel(this.label), LanguageTextField(
                    language, project, value
                ).apply {
                    addDocumentListener(object: DocumentListener {
                        override fun documentChanged(event: DocumentEvent) {
                            this@addToFormBuilder.value = this@apply.document.text
                        }
                    })
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
                testLLMConnection()
                row {
                    text(AutoDevBundle.message("settings.autodev.coder.testConnectionButton.tips")).apply {
                        this.component.foreground = JBColor.RED
                    }
                }
            })
            .addSeparator()
            .addVerticalGap(2)
            .addLabeledComponent(jBLabel("settings.autodev.coder.customLlms", 1), customLlmParam, 1, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        panel.invalidate()
        panel.repaint()
    }

    fun exportSettings(destination: AutoDevSettingsState) {
        destination.apply {
            maxTokenLength = maxTokenLengthParam.value
            customModel = customModelParam.value
            customOpenAiHost = customOpenAIHostParam.value
            language = languageParam.value
            customEngineServer = customEngineServerParam.value
            customEngineToken = customEngineTokenParam.value
            customLlms = customLlmParam.text
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
                settings.customLlms != customLlmParam.text ||
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
