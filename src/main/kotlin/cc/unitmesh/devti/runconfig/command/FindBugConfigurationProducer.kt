package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.runconfig.DtRunConfiguration
import cc.unitmesh.devti.runconfig.config.FindBugConfigure
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class FindBugConfigurationProducer : BaseLazyRunConfigurationProducer<FindBugConfigure>() {
    init {
        registerConfigProvider { elements -> createConfigFor(elements) }
    }

    private fun createConfigFor(
        elements: List<PsiElement>
    ): FindBugConfigure? {
        if (elements.isEmpty()) return null
        val methods = elements.filterIsInstance<PsiMethod>()
        if (methods.isEmpty()) return null

        val method = methods.first()

        return FindBugConfigure(method.name)
    }

    override fun isConfigurationFromContext(configuration: DtRunConfiguration, context: ConfigurationContext): Boolean {
        val config = findConfig(context.location?.psiElement?.let { listOf(it) } ?: return false) ?: return false
        configuration.name = config.configurationName

        return true
    }

    override fun setupConfigurationFromContext(
        configuration: DtRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val config = findConfig(context.location?.psiElement?.let { listOf(it) } ?: return false) ?: return false
        configuration.name = config.configurationName

        return true
    }

    private fun registerConfigProvider(provider: (List<PsiElement>) -> FindBugConfigure?) {
        runConfigProviders.add(provider)
    }
}