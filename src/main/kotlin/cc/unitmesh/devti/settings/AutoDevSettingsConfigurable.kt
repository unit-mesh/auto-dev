package cc.unitmesh.devti.settings

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nullable
import javax.swing.JComponent

class AutoDevSettingsConfigurable : Configurable {
    private lateinit var component: AppSettingsComponent

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "AutoDev"
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return component.preferredFocusedComponent
    }

    @Nullable
    override fun createComponent(): JComponent {
        component = AppSettingsComponent()
        return component.panel
    }

    override fun isModified(): Boolean {
        val settings: AutoDevSettingsState = AutoDevSettingsState.getInstance()
        return component.isModified(settings)
    }

    override fun apply() {
        val settings: AutoDevSettingsState = AutoDevSettingsState.getInstance()
        settings.openAiKey = component.getOpenAiKey()
        settings.githubToken = component.getGithubToken()
        settings.openAiModel = component.getOpenAiModel()
        settings.customOpenAiHost = component.getOpenAiHost()
        settings.aiEngine = component.getAiEngine()
        settings.customEngineServer = component.getCustomEngineServer()
        settings.customEngineToken = component.getCustomEngineToken()
        settings.customEnginePrompts = component.getCustomEnginePrompt()
        settings.language = component.getLanguage()
        settings.maxTokenLength = component.getMaxTokenLength().ifEmpty(MAX_TOKEN_LENGTH::toString)
    }

    override fun reset() {
        val settings: AutoDevSettingsState = AutoDevSettingsState.getInstance()
        component.setLanguage(settings.language)
        component.setOpenAiKey(settings.openAiKey)
        component.setGithubToken(settings.githubToken)
        component.setOpenAiModel(settings.openAiModel)
        component.setOpenAiHost(settings.customOpenAiHost)
        component.setAiEngine(settings.aiEngine)
        component.setCustomEngineServer(settings.customEngineServer)
        component.setCustomEngineToken(settings.customEngineToken)
        component.setCustomEnginePrompt(settings.customEnginePrompts)
        component.setLanguage(settings.language)
        component.setMaxTokenLength(settings.maxTokenLength)
    }
}
