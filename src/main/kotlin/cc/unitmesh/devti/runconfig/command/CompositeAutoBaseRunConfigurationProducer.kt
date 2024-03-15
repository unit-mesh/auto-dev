package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.runconfig.config.AutoCRUDConfiguration
import com.intellij.execution.PsiLocation
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class CompositeAutoBaseRunConfigurationProducer : BaseConfigurationProducer() {
    private val producers: List<BaseConfigurationProducer> =
        listOf(AutoDevFeatureConfigurationProducer())

    override fun findExistingConfiguration(context: ConfigurationContext): RunnerAndConfigurationSettings? {
        val preferredConfig = createPreferredConfigurationFromContext(context) ?: return null
        val runManager = RunManager.getInstance(context.project)
        val configurations = getConfigurationSettingsList(runManager)
        for (configurationSettings in configurations) {
            if (preferredConfig.configuration.isSame(configurationSettings.configuration)) {
                return configurationSettings
            }
        }
        return null
    }

    private fun createPreferredConfigurationFromContext(context: ConfigurationContext): ConfigurationFromContext? =
        producers
            .mapNotNull { it.createConfigurationFromContext(context) }
            .sortedWith(ConfigurationFromContext.COMPARATOR)
            .firstOrNull()

    override fun isConfigurationFromContext(
        configuration: AutoCRUDConfiguration,
        context: ConfigurationContext
    ): Boolean = producers.any { it.isConfigurationFromContext(configuration, context) }

    override fun setupConfigurationFromContext(
        configuration: AutoCRUDConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean = producers.any { it.setupConfigurationFromContext(configuration, context, sourceElement) }

    override fun findOrCreateConfigurationFromContext(context: ConfigurationContext): ConfigurationFromContext? {
        val preferredConfig = createPreferredConfigurationFromContext(context) ?: return null
        val psiElement = preferredConfig.sourceElement
        val locationFromContext = context.location ?: return null
        val locationFromElement = PsiLocation.fromPsiElement(psiElement, locationFromContext.module)
        if (locationFromElement != null) {
            val settings = findExistingConfiguration(context)
            if (preferredConfig.configuration.isSame(settings?.configuration)) {
                preferredConfig.setConfigurationSettings(settings)
            } else {
                RunManager.getInstance(context.project).setUniqueNameIfNeeded(preferredConfig.configuration)
            }
        }
        return preferredConfig
    }


}

private fun RunConfiguration.isSame(other: RunConfiguration?): Boolean {
    return when {
        this === other -> true
        this is AutoCRUDConfiguration && other is AutoCRUDConfiguration -> {
            this.options == other.options
        }

        else -> {
            false
        }
    }
}
