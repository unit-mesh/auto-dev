package cc.unitmesh.devti.settings

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nullable
import javax.swing.JComponent

class DevtiSettingsConfigurable : Configurable {
    private var mySettingsComponent: AppSettingsComponent? = null

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "Devti"
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return mySettingsComponent!!.preferredFocusedComponent
    }

    @Nullable
    override fun createComponent(): JComponent {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings: DevtiSettingsState = DevtiSettingsState.getInstance()!!
        var modified = !mySettingsComponent!!.openAiKey.equals(settings.openAiKey)
        modified = modified or (!mySettingsComponent!!.githubToken.equals(settings.githubToken))
        modified = modified or (!mySettingsComponent!!.openAiVersion.equals(settings.openAiVersion))
        modified = modified or (!mySettingsComponent!!.customOpenAiHost.equals(settings.customOpenAiHost))
        return modified
    }

    override fun apply() {
        val settings: DevtiSettingsState = DevtiSettingsState.getInstance()!!
        settings.openAiKey = mySettingsComponent!!.getOpenAiKey()
        settings.githubToken = mySettingsComponent!!.getGithubToken()
        settings.openAiVersion = mySettingsComponent!!.getOpenAiVersion()
        settings.customOpenAiHost = mySettingsComponent!!.getOpenAiHost()
    }

    override fun reset() {
        val settings: DevtiSettingsState = DevtiSettingsState.getInstance()!!
        mySettingsComponent!!.setOpenAiKey(settings.openAiKey)
        mySettingsComponent!!.setGithubToken(settings.githubToken)
        mySettingsComponent!!.setOpenAiVersion(settings.openAiVersion)
        mySettingsComponent!!.setOpenAiHost(settings.customOpenAiHost)
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
