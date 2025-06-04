package cc.unitmesh.devti.settings

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

/**
 * Simplified AutoDev Configurable that provides a cleaner, more user-friendly interface
 * for LLM configuration. This replaces the complex original LLMSettingComponent with
 * a streamlined approach.
 */
class SimplifiedAutoDevConfigurable : Configurable {
    private var settingsComponent: SimplifiedLLMSettingComponent? = null

    override fun getDisplayName(): String {
        return AutoDevBundle.message("settings.autodev.name")
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return settingsComponent?.panel
    }

    override fun createComponent(): JComponent? {
        val settings = AutoDevSettingsState.getInstance()
        settingsComponent = SimplifiedLLMSettingComponent(settings)
        return settingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val settings = AutoDevSettingsState.getInstance()
        return settingsComponent?.isModified(settings) ?: false
    }

    override fun apply() {
        val settings = AutoDevSettingsState.getInstance()
        settingsComponent?.exportSettings(settings)
    }

    override fun reset() {
        val settings = AutoDevSettingsState.getInstance()
        settingsComponent?.applySettings(settings)
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
