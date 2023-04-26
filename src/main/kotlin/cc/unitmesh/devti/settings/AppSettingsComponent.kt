package cc.unitmesh.devti.settings

import com.intellij.json.JsonLanguage
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import java.awt.FontMetrics
import javax.swing.JComponent
import javax.swing.JPanel

val OPENAI_MODEL = arrayOf("gpt-3.5-turbo", "gpt-4.0")

val DEFAULT_PROMPTS = """
{
    "auto_complete": {
        "instruction": "",
        "input": ""
    },
    "auto_comment": {
        "instruction": "",
        "input": ""
    }
}
    """

val AI_ENGINES = arrayOf("OpenAI", "Custom")
val DEFAULT_AI_ENGINE = AI_ENGINES[0]

class AppSettingsComponent {
    val panel: JPanel
    val openAiKey = JBTextField()
    val githubToken = JBTextField()
    val customOpenAiHost = JBTextField()
    val openAiModel = ComboBox(OPENAI_MODEL)

    val aiEngine = ComboBox(AI_ENGINES)
    val customEngineServer = JBTextField()
    val customEngineToken = JBTextField()
    var customEnginePrompt = LanguageTextField(JsonLanguage.INSTANCE, null, DEFAULT_PROMPTS.trimIndent())

    init {
        val metrics: FontMetrics = customEnginePrompt.getFontMetrics(customEnginePrompt.font)
        val columnWidth = metrics.charWidth('m')
        customEnginePrompt.setOneLineMode(false)
        customEnginePrompt.preferredSize = Dimension(25 * columnWidth, 16 * metrics.height)

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("AI Engine: "), aiEngine, 1, false)
            .addSeparator()
            .addTooltip("GitHub Token is for AutoCurd")
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
        return openAiModel.selectedItem?.toString() ?: "gpt-3.5-turbo"
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
}
