package cc.unitmesh.devti.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

val OPENAI_MODEL = arrayOf("gpt-3.5-turbo", "gpt-4.0")

class AppSettingsComponent {
    val panel: JPanel
    val openAiKey = JBTextField()
    val githubToken = JBTextField()
    val customOpenAiHost = JBTextField()
    val openAiModel = ComboBox(OPENAI_MODEL)

    val aiEngine = ComboBox(arrayOf("OpenAI", "Custom"))
    val customEngineServer = JBTextField()
    val customEngineToken = JBTextField()
    val customEnginePrompt = JTextArea(20, 1)

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("GitHub Token: "), githubToken, 1, false)
            .addLabeledComponent(JBLabel("AI Engine: "), aiEngine, 1, false)
            .addLabeledComponent(JBLabel("OpenAI Key: "), openAiKey, 1, false)
            .addLabeledComponent(JBLabel("OpenAI Model: "), openAiModel, 1, false)
            .addLabeledComponent(JBLabel("Custom OpenAI Host: "), customOpenAiHost, 1, false)
            .addLabeledComponent(JBLabel("Custom Engine Server: "), customEngineServer, 1, false)
            .addLabeledComponent(JBLabel("Custom Engine Token: "), customEngineToken, 1, false)
            .addLabeledComponent(JBLabel("Custom Engine Prompt (Json): "), customEnginePrompt, 1, false)
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
