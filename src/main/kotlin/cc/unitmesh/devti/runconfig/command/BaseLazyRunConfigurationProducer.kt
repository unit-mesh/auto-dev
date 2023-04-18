package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.runconfig.config.AutoCRUDConfiguration
import cc.unitmesh.devti.runconfig.AutoCRUDConfigurationType
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement


abstract class BaseLazyRunConfigurationProducer<T: BaseConfig> : LazyRunConfigurationProducer<AutoCRUDConfiguration>() {
    val runConfigProviders: MutableList<(List<PsiElement>) -> T?> = mutableListOf()

    override fun getConfigurationFactory(): ConfigurationFactory {
        return AutoCRUDConfigurationType.getInstance().factory
    }

    override fun setupConfigurationFromContext(
        configuration: AutoCRUDConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val endpoint = findIEndpointByContext(context) ?: return false
        configuration.name = endpoint.configurationName

        return true
    }

    private fun findIEndpointByContext(context: ConfigurationContext): T? {
        val elements = context.location?.psiElement?.let { listOf(it) }
        return elements?.let { findConfig(it) }
    }

    fun findConfig(elements: List<PsiElement>): T? {
        for (provider in runConfigProviders) {
            val config = provider(elements)
            if (config != null) return config
        }

        return null
    }

    override fun isConfigurationFromContext(
        configuration: AutoCRUDConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val implConfig = findIEndpointByContext(context) ?: return false
        configuration.name = implConfig.configurationName

        return true
    }
}