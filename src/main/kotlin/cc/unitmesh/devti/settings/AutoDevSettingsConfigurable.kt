package cc.unitmesh.devti.settings

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nullable
import javax.swing.JComponent

class AutoDevSettingsConfigurable : Configurable {
    private val component: LLMSettingComponent = LLMSettingComponent(AutoDevSettingsState.getInstance())

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String = AutoDevBundle.message("name")

    override fun apply() = component.exportSettings(AutoDevSettingsState.getInstance())

    override fun reset() = component.applySettings(AutoDevSettingsState.getInstance(), true)
    override fun getPreferredFocusedComponent(): JComponent? = null

    @Nullable
    override fun createComponent(): JComponent = component.panel

    override fun isModified(): Boolean {
        val settings: AutoDevSettingsState = AutoDevSettingsState.getInstance()
        return component.isModified(settings)
    }
}
