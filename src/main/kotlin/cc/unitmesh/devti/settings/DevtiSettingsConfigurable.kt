package cc.unitmesh.devti.settings

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nullable
import javax.swing.JComponent

class DevtiSettingsConfigurable : Configurable {
    private var component: AppSettingsComponent? = null

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "Devti"
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return component!!.preferredFocusedComponent
    }

    @Nullable
    override fun createComponent(): JComponent {
        component = AppSettingsComponent()
        return component!!.panel
    }

    override fun isModified(): Boolean {
        val settings: DevtiSettingsState = DevtiSettingsState.getInstance()!!
        var modified = !component!!.openAiKey.equals(settings.openAiKey)
        modified = modified or (!component!!.githubToken.equals(settings.githubToken))
        modified = modified or (!component!!.openAiModel.equals(settings.openAiModel))
        modified = modified or (!component!!.customOpenAiHost.equals(settings.customOpenAiHost))
        modified = modified or (!component!!.aiEngine.equals(settings.aiEngine))
        modified = modified or (!component!!.customEngineServer.equals(settings.customEngineServer))
        modified = modified or (!component!!.customEngineToken.equals(settings.customEngineToken))
        modified = modified or (!component!!.customEnginePrompt.equals(settings.customEnginePrompt))
        return modified
    }

    override fun apply() {
        val settings: DevtiSettingsState = DevtiSettingsState.getInstance()!!
        settings.openAiKey = component!!.getOpenAiKey()
        settings.githubToken = component!!.getGithubToken()
        settings.openAiModel = component!!.getOpenAiModel()
        settings.customOpenAiHost = component!!.getOpenAiHost()
        settings.aiEngine = component!!.getAiEngine()
        settings.customEngineServer = component!!.getCustomEngineServer()
        settings.customEngineToken = component!!.getCustomEngineToken()
        settings.customEnginePrompt = component!!.getCustomEnginePrompt()
    }

    override fun reset() {
        val settings: DevtiSettingsState = DevtiSettingsState.getInstance()!!
        component!!.setOpenAiKey(settings.openAiKey)
        component!!.setGithubToken(settings.githubToken)
        component!!.setOpenAiModel(settings.openAiModel)
        component!!.setOpenAiHost(settings.customOpenAiHost)
        component!!.setAiEngine(settings.aiEngine)
        component!!.setCustomEngineServer(settings.customEngineServer)
        component!!.setCustomEngineToken(settings.customEngineToken)
        component!!.setCustomEnginePrompt(settings.customEnginePrompt)
    }

    override fun disposeUIResources() {
        component = null
    }
}
