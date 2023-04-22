package cc.unitmesh.devti.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

val OPENAI_MODEL = arrayOf("gpt-3.5-turbo", "gpt-4.0")

class AppSettingsComponent {
    val panel: JPanel
    val openAiKey = JBTextField()
    val githubToken = JBTextField()
    val customOpenAiHost = JBTextField()

    val openAiModel = ComboBox(OPENAI_MODEL)

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("OpenAI Key: "), openAiKey, 1, false)
            .addLabeledComponent(JBLabel("GitHub Token: "), githubToken, 1, false)
            .addLabeledComponent(JBLabel("OpenAI Model: "), openAiModel, 1, false)
            .addLabeledComponent(JBLabel("Custom OpenAI Host: "), customOpenAiHost, 1, false)
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
}
