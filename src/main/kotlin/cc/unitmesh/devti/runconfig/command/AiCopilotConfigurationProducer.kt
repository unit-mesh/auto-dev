package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.runconfig.config.AutoCRUDConfiguration
import cc.unitmesh.devti.runconfig.config.AiCopilotConfigure
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class AiCopilotConfigurationProducer : BaseLazyRunConfigurationProducer<AiCopilotConfigure>() {
    init {
        registerConfigProvider { elements -> createConfigFor(elements) }
    }

    private fun createConfigFor(
        elements: List<PsiElement>
    ): AiCopilotConfigure? {
        if (elements.isEmpty()) return null
        val identifiers = elements.filterIsInstance<PsiIdentifier>()
        if (identifiers.isEmpty()) return null

        val identifier = identifiers.first()

        val parent = identifier.parent
        if (parent !is PsiMethod) return null

        return AiCopilotConfigure(identifier.text)
    }

    override fun isConfigurationFromContext(configuration: AutoCRUDConfiguration, context: ConfigurationContext): Boolean {
        val config = findConfig(context.location?.psiElement?.let { listOf(it) } ?: return false) ?: return false
        configuration.name = config.configurationName

        return true
    }

    override fun setupConfigurationFromContext(
        configuration: AutoCRUDConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val config = findConfig(context.location?.psiElement?.let { listOf(it) } ?: return false) ?: return false
        configuration.name = config.configurationName

        return true
    }

    private fun registerConfigProvider(provider: (List<PsiElement>) -> AiCopilotConfigure?) {
        runConfigProviders.add(provider)
    }
}