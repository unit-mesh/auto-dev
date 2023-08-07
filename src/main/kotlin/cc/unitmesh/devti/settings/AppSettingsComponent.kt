package cc.unitmesh.devti.settings

import com.intellij.json.JsonLanguage
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import java.awt.FontMetrics
import javax.swing.JComponent
import javax.swing.JPanel

class AppSettingsComponent {
    val panel: JPanel
    val openAiKey = JBPasswordField()
    val githubToken = JBPasswordField()
    val customOpenAiHost = JBTextField()
    val openAiModel = ComboBox(OPENAI_MODEL)

    val aiEngine = ComboBox(AI_ENGINES)
    val customEngineServer = JBTextField()
    val customEngineToken = JBTextField()
    val language = ComboBox(HUMAN_LANGUAGES)

    private var myEditor: EditorEx? = null
    private val customEnginePrompt by lazy {
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        object : LanguageTextField(JsonLanguage.INSTANCE, project, "") {

            override fun createEditor(): EditorEx {
                myEditor = super.createEditor().apply {
                    setShowPlaceholderWhenFocused(true)
                    setHorizontalScrollbarVisible(true)
                    setVerticalScrollbarVisible(true)
                    setPlaceholder("Enter custom prompt here")

                    val scheme = EditorColorsUtil.getColorSchemeForBackground(this.colorsScheme.defaultBackground)
                    this.colorsScheme = this.createBoundColorSchemeDelegate(scheme)
                }

                return myEditor!!
            }
        }
    }

    init {
        val metrics: FontMetrics = customEnginePrompt.getFontMetrics(customEnginePrompt.font)
        val columnWidth = metrics.charWidth('m')
        customEnginePrompt.setOneLineMode(false)
        customEnginePrompt.preferredSize = Dimension(25 * columnWidth, 16 * metrics.height)

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Language: "), language, 1, false)
            .addLabeledComponent(JBLabel("AI Engine: "), aiEngine, 1, false)
            .addSeparator()
            .addTooltip("GitHub Token is for AutoDev")
            .addLabeledComponent(JBLabel("GitHub Token: "), githubToken, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("OpenAI Model: "), openAiModel, 1, false)
            .addLabeledComponent(JBLabel("OpenAI Key: "), openAiKey, 1, false)
            .addLabeledComponent(JBLabel("Custom OpenAI Host: "), customOpenAiHost, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Custom Engine Server: "), customEngineServer, 1, false)
            .addLabeledComponent(JBLabel("Custom Engine Token: "), customEngineToken, 1, false)
            .addVerticalGap(2)
            .addLabeledComponent(JBLabel("Custom Engine Prompt (Json): "), customEnginePrompt, 1, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    val preferredFocusedComponent: JComponent
        get() = openAiKey

    fun getOpenAiKey(): String {
        return openAiKey.text
    }

    fun setOpenAiKey(newText: String) {
        openAiKey.text = newText
    }

    fun getGithubToken(): String {
        return githubToken.text
    }

    fun setGithubToken(newText: String) {
        githubToken.text = newText
    }

    fun getOpenAiModel(): String {
        return openAiModel.selectedItem?.toString() ?: OPENAI_MODEL[0]
    }

    fun setOpenAiModel(newText: String) {
        openAiModel.selectedItem = newText
    }

    fun getOpenAiHost(): String {
        return customOpenAiHost.text
    }

    fun setOpenAiHost(newText: String) {
        customOpenAiHost.text = newText
    }

    fun getAiEngine(): String {
        return aiEngine.selectedItem?.toString() ?: "OpenAI"
    }

    fun setAiEngine(newText: String) {
        aiEngine.selectedItem = newText
    }

    fun getCustomEngineServer(): String {
        return customEngineServer.text
    }

    fun setCustomEngineServer(newText: String) {
        customEngineServer.text = newText
    }

    fun getCustomEngineToken(): String {
        return customEngineToken.text
    }

    fun setCustomEngineToken(newText: String) {
        customEngineToken.text = newText
    }

    fun getCustomEnginePrompt(): String {
        return customEnginePrompt.text
    }

    fun setCustomEnginePrompt(newText: String) {
        customEnginePrompt.text = newText
    }

    fun getLanguage(): String {
        return language.selectedItem?.toString() ?: HUMAN_LANGUAGES[0]
    }

    fun setLanguage(newText: String) {
        language.selectedItem = newText
    }

    fun isModified(origineSettings: AutoDevSettingsState): Boolean {
        // TODO use data class to avoid manually write this
        return origineSettings.openAiKey != getOpenAiKey() ||
                origineSettings.githubToken != getGithubToken() ||
                origineSettings.openAiModel != getOpenAiModel() ||
                origineSettings.customOpenAiHost != getOpenAiHost() ||
                origineSettings.aiEngine != getAiEngine() ||
                origineSettings.customEngineServer != getCustomEngineServer() ||
                origineSettings.customEngineToken != getCustomEngineToken() ||
                origineSettings.customEnginePrompts != getCustomEnginePrompt() ||
                origineSettings.language != getLanguage()
    }
}
