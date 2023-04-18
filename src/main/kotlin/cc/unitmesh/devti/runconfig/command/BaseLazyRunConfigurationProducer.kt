package cc.unitmesh.devti.runconfig.command

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.psi.PsiElement


abstract class BaseLazyRunConfigurationProducer<T: BaseConfig, C: RunConfigurationBase<out ModuleBasedConfigurationOptions>> :
    LazyRunConfigurationProducer<C>() {
    val runConfigProviders: MutableList<(List<PsiElement>) -> T?> = mutableListOf()

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
}