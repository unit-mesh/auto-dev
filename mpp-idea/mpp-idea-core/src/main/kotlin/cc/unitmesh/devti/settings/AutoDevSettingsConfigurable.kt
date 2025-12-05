package cc.unitmesh.devti.settings

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nullable
import javax.swing.JComponent

class AutoDevSettingsConfigurable : Configurable {
    private var component: SimplifiedLLMSettingComponent? = null

    @Nls
    override fun getDisplayName(): String = AutoDevBundle.message("name")

    override fun apply() {
        val settings = AutoDevSettingsState.getInstance()
        component?.exportSettings(settings)
    }

    override fun reset() {
        val settings = AutoDevSettingsState.getInstance()
        component?.applySettings(settings, true)
    }

    override fun getPreferredFocusedComponent(): JComponent? = component?.panel

    @Nullable
    override fun createComponent(): JComponent? {
        val settings = AutoDevSettingsState.getInstance()
        component = SimplifiedLLMSettingComponent(settings)
        return component?.panel
    }

    override fun isModified(): Boolean {
        val settings = AutoDevSettingsState.getInstance()
        return component?.isModified(settings) ?: false
    }

    override fun disposeUIResources() {
        component = null
    }
}
