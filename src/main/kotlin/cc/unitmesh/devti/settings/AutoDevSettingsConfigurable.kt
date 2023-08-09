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
        component = AppSettingsComponent(AutoDevSettingsState.getInstance())
        return component.panel
    }

    override fun isModified(): Boolean {
        val settings: AutoDevSettingsState = AutoDevSettingsState.getInstance()
        return component.isModified(settings)
    }

    override fun apply() {
        component.exportSettings(target = AutoDevSettingsState.getInstance())
    }

    override fun reset() {
        component.applySettings(AutoDevSettingsState.getInstance())
    }
}
