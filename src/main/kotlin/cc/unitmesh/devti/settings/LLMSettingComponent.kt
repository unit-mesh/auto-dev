package cc.unitmesh.devti.settings

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.json.JsonLanguage
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import java.awt.FontMetrics
import javax.swing.JPanel

class LLMSettingComponent(private val settings: AutoDevSettingsState) {

    // 以下 LLMParam 变量不要改名，因为这些变量名会被用作配置文件的 key
    private val languageParam by LLMParam.creating { ComboBox(settings.language, HUMAN_LANGUAGES.toList()) }
    private val aiEngineParam by LLMParam.creating(onChange = { onSelectedEngineChanged() }) {
        ComboBox(settings.aiEngine, AIEngines.values().toList().map { it.name })
    }
    private val delaySecondsParam by LLMParam.creating { Editable(settings.delaySeconds) }
    private val maxTokenLengthParam by LLMParam.creating { Editable(settings.maxTokenLength) }
    private val openAIModelsParam by LLMParam.creating { ComboBox(settings.openAiModel, OPENAI_MODEL.toList()) }
    private val openAIKeyParam by LLMParam.creating { Password(settings.openAiKey) }
    private val customOpenAIHostParam: LLMParam by LLMParam.creating { Editable(settings.customOpenAiHost) }

    private val gitTypeParam: LLMParam by LLMParam.creating { ComboBox(settings.gitType, GIT_TYPE.toList()) }
    private val gitLabUrlParam: LLMParam by LLMParam.creating { Editable(settings.gitlabUrl) }
    private val gitLabTokenParam: LLMParam by LLMParam.creating { Password(settings.gitlabToken) }

    private val gitHubTokenParam by LLMParam.creating { Password(settings.githubToken) }
    private val customEngineServerParam by LLMParam.creating { Editable(settings.customEngineServer) }
    private val customEngineTokenParam by LLMParam.creating { Password(settings.customEngineToken) }
    private val xingHuoAppIDParam by LLMParam.creating { Editable(settings.xingHuoAppId) }
    private val xingHuoApiKeyParam by LLMParam.creating { Password(settings.xingHuoApiKey) }
    private val xingHuoApiSecretParam by LLMParam.creating { Password(settings.xingHuoApiSecrect) }
    private val customEngineResponseFormatParam by LLMParam.creating { Editable(settings.customEngineResponseFormat) }
    private val customEngineRequestBodyFormatParam by LLMParam.creating { Editable(settings.customEngineRequestBodyFormat) }
    private val customEngineRequestHeaderFormatParam by LLMParam.creating { Editable(settings.customEngineRequestHeaderFormat) }


    val project = ProjectManager.getInstance().openProjects.firstOrNull()
    private val customEnginePrompt by lazy {
        object : LanguageTextField(JsonLanguage.INSTANCE, project, settings.customPrompts) {
            override fun createEditor(): EditorEx {

                return super.createEditor().apply {
                    setShowPlaceholderWhenFocused(true)
                    setHorizontalScrollbarVisible(false)
                    setVerticalScrollbarVisible(true)
                    setPlaceholder(AutoDevBundle.message("autodev.custom.prompt.placeholder"))


                    val scheme = EditorColorsUtil.getColorSchemeForBackground(this.colorsScheme.defaultBackground)
                    this.colorsScheme = this.createBoundColorSchemeDelegate(scheme)
                }
            }
        }.apply {
            val metrics: FontMetrics = getFontMetrics(font)
            val columnWidth = metrics.charWidth('m')
            setOneLineMode(false)
            preferredSize = Dimension(25 * columnWidth, 25 * metrics.height)
        }
    }

    private val llmGroups = mapOf<AIEngines, List<LLMParam>>(
            AIEngines.Azure to listOf(
                    openAIModelsParam,
                    openAIKeyParam,
                    customOpenAIHostParam,
            ),
            AIEngines.OpenAI to listOf(
                    openAIModelsParam,
                    openAIKeyParam,
                    customOpenAIHostParam,
            ),
            AIEngines.Custom to listOf(
                    customEngineServerParam,
                    customEngineTokenParam,
                    customEngineResponseFormatParam,
                    customEngineRequestBodyFormatParam,
                    customEngineRequestHeaderFormatParam,
            ),
            AIEngines.XingHuo to listOf(
                    xingHuoAppIDParam,
                    xingHuoApiKeyParam,
                    xingHuoApiSecretParam,
            ),
    )


    private val onSelectedEngineChanged: () -> Unit = {
        applySettings(settings, updateParams = false)
    }
    private val _currentSelectedEngine: AIEngines
        get() = AIEngines.values().first { it.name.lowercase() == aiEngineParam.value.lowercase() }

    private val currentLLMParams: List<LLMParam>
        get() {
            return llmGroups[_currentSelectedEngine]
                    ?: throw IllegalStateException("Unknown engine: ${settings.aiEngine}")
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
                formBuilder.addLabeledComponent(JBLabel(this.label), ReactivePasswordField(this) {
                    this.text = it.value
                    this.isEnabled = it.isEditable
                }, 1, false)
            }

            LLMParam.ParamType.Text -> {
                formBuilder.addLabeledComponent(JBLabel(this.label), ReactiveTextField(this) {
                    this.isEnabled = it.isEditable
                }, 1, false)
            }

            LLMParam.ParamType.ComboBox -> {
                formBuilder.addLabeledComponent(JBLabel(this.label), ReactiveComboBox(this), 1, false)
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
        if (updateParams) {
            updateParams(settings)
        }

        formBuilder
                .addLLMParam(languageParam)
                .addSeparator()
                .addTooltip("For Custom LLM, config Custom Engine Server & Custom Engine Token & Custom Response Format")
                .addLLMParam(aiEngineParam)
                .addLLMParam(maxTokenLengthParam)
                .addLLMParam(delaySecondsParam)
            .addSeparator()
                .addTooltip("Select Git Type")
            .addLLMParam(gitTypeParam)
            .addTooltip("GitHub Token is for AutoCRUD Model")
                .addLLMParam(gitHubTokenParam)
            .addTooltip("GitLab options is for AutoCRUD Model")
            .addLLMParam(gitLabUrlParam)
            .addLLMParam(gitLabTokenParam)
                .addSeparator()
                .addLLMParams(currentLLMParams)
                .addVerticalGap(2)
                .addSeparator()
                .addLabeledComponent(JBLabel("Custom Engine Prompt (Json): "), customEnginePrompt, 1, true)
                .addComponentFillVertically(JPanel(), 0)
                .panel

        panel.invalidate()
        panel.repaint()
    }

    private fun updateParams(settings: AutoDevSettingsState) {
        settings.apply {
            maxTokenLengthParam.value = maxTokenLength
            gitTypeParam.value = gitType
            gitHubTokenParam.value = githubToken
            gitLabTokenParam.value = gitlabToken
            gitLabUrlParam.value = gitlabUrl
            openAIKeyParam.value = openAiKey
            customOpenAIHostParam.value = customOpenAiHost
            customEngineServerParam.value = customEngineServer
            customEngineTokenParam.value = customEngineToken
            openAIModelsParam.value = openAiModel
            xingHuoAppIDParam.value = xingHuoAppId
            xingHuoApiKeyParam.value = xingHuoApiKey
            xingHuoApiSecretParam.value = xingHuoApiSecrect
            languageParam.value = language
            aiEngineParam.value = aiEngine
            customEnginePrompt.text = customPrompts
            customEngineResponseFormatParam.value = customEngineResponseFormat
            customEngineRequestBodyFormatParam.value = customEngineRequestBodyFormat
            customEngineRequestHeaderFormatParam.value = customEngineRequestHeaderFormat
            delaySecondsParam.value = delaySeconds
        }
    }

    fun exportSettings(destination: AutoDevSettingsState) {
        destination.apply {
            maxTokenLength = maxTokenLengthParam.value
            gitType = gitTypeParam.value
            githubToken = gitHubTokenParam.value
            gitlabUrl = gitLabUrlParam.value
            gitlabToken = gitLabTokenParam.value
            openAiKey = openAIKeyParam.value
            customOpenAiHost = customOpenAIHostParam.value
            xingHuoApiSecrect = xingHuoApiSecretParam.value
            xingHuoAppId = xingHuoAppIDParam.value
            xingHuoApiKey = xingHuoApiKeyParam.value
            aiEngine = aiEngineParam.value
            language = languageParam.value
            customEngineServer = customEngineServerParam.value
            customEngineToken = customEngineTokenParam.value
            customPrompts = customEnginePrompt.text
            openAiModel = openAIModelsParam.value
            customEngineResponseFormat = customEngineResponseFormatParam.value
            customEngineRequestBodyFormat = customEngineRequestBodyFormatParam.value
            customEngineRequestHeaderFormat = customEngineRequestHeaderFormatParam.value
            delaySeconds = delaySecondsParam.value
        }
    }

    fun isModified(settings: AutoDevSettingsState): Boolean {
        return settings.maxTokenLength != maxTokenLengthParam.value ||
                settings.gitType != gitTypeParam.value ||
                settings.githubToken != gitHubTokenParam.value ||
                settings.gitlabUrl != gitLabUrlParam.value ||
                settings.gitlabToken != gitLabTokenParam.value ||
                settings.openAiKey != openAIKeyParam.value ||
                settings.xingHuoApiSecrect != xingHuoApiSecretParam.value ||
                settings.xingHuoAppId != xingHuoAppIDParam.value ||
                settings.xingHuoApiKey != xingHuoApiKeyParam.value ||
                settings.aiEngine != aiEngineParam.value ||
                settings.language != languageParam.value ||
                settings.customEngineServer != customEngineServerParam.value ||
                settings.customEngineToken != customEngineTokenParam.value ||
                settings.customPrompts != customEnginePrompt.text ||
                settings.openAiModel != openAIModelsParam.value ||
                settings.customOpenAiHost != customOpenAIHostParam.value ||
                settings.customEngineResponseFormat != customEngineResponseFormatParam.value ||
                settings.customEngineRequestBodyFormat != customEngineRequestBodyFormatParam.value ||
                settings.customEngineRequestHeaderFormat != customEngineRequestHeaderFormatParam.value ||
                settings.delaySeconds != delaySecondsParam.value
    }

    init {
        applySettings(settings)
    }
}


