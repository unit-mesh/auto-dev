package cc.unitmesh.devti.settings

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nullable
import javax.swing.JComponent

class AutoDevSettingsConfigurable : Configurable {
    private var component: SimplifiedLLMSettingComponent? = null

    @Nls(capitalization = Nls.Capitalization.Title)
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

        // Try to migrate legacy configuration first
        LegacyConfigMigration.migrateIfNeeded()

        component = SimplifiedLLMSettingComponent(settings)

        // Show configuration wizard if needed (only if no migration happened and no config exists)
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        LLMConfigurationWizard.showIfNeeded(project)

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
