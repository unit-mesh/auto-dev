package cc.unitmesh.devti.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class AppSettingsComponent {
    val panel: JPanel
    val openAiKey = JBTextField()
    val githubToken = JBTextField()
    val customOpenAiHost = JBTextField()
    val openAiVersion = ComboBox(arrayOf("gpt-3.5-turbo", "text-davinci-003", "gpt-4.0"))

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("OpenAI Key: "), openAiKey, 1, false)
            .addLabeledComponent(JBLabel("GitHub Token: "), githubToken, 1, false)
            .addLabeledComponent(JBLabel("OpenAI Version: "), openAiVersion, 1, false)
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

    fun getOpenAiVersion(): String {
        return openAiVersion.selectedItem.toString()
    }

    fun setOpenAiVersion(newText: String) {
        openAiVersion.selectedItem = newText
    }

    fun getOpenAiHost(): String {
        return customOpenAiHost.text
    }

    fun setOpenAiHost(newText: String) {
        customOpenAiHost.text = newText
    }
}
