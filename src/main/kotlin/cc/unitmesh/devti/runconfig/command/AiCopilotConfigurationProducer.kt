package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.runconfig.AiCopilotConfigurationType
import cc.unitmesh.devti.runconfig.config.AiCopilot
import cc.unitmesh.devti.runconfig.config.AiCopilotConfiguration
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

sealed class AiCopilotType {
    object CodeComments : AiCopilotType()
    object CodeComplete : AiCopilotType()
    object FindBugs : AiCopilotType()

    override fun toString(): String {
        return when (this) {
            is CodeComments -> "CodeComments"
            is CodeComplete -> "CodeComplete"
            is FindBugs -> "FindBugs"
        }
    }
}

class AiCopilotConfigurationProducer : BaseLazyRunConfigurationProducer<AiCopilot, AiCopilotConfiguration>() {
    init {
        registerConfigProvider { elements -> createConfigFor(elements, AiCopilotType.CodeComplete) }
        registerConfigProvider { elements -> createConfigFor(elements, AiCopilotType.CodeComments) }
    }

    override fun getConfigurationFactory(): ConfigurationFactory {
        return AiCopilotConfigurationType.getInstance().factory
    }

    private fun createConfigFor(
        elements: List<PsiElement>,
        copilotType: AiCopilotType
    ): AiCopilot? {
        if (elements.isEmpty()) return null
        val identifiers = elements.filterIsInstance<PsiIdentifier>()
        if (identifiers.isEmpty()) return null

        val identifier = identifiers.first()

        val parent = identifier.parent
        if (parent !is PsiMethod) return null

        return AiCopilot(identifier.text, copilotType)
    }

    override fun isConfigurationFromContext(
        configuration: AiCopilotConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val config = findConfig(context.location?.psiElement?.let { listOf(it) } ?: return false) ?: return false
        configuration.name = config.configurationName

        return true
    }

    override fun setupConfigurationFromContext(
        configuration: AiCopilotConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val config = findConfig(context.location?.psiElement?.let { listOf(it) } ?: return false) ?: return false
        configuration.name = config.configurationName

        return true
    }

    private fun registerConfigProvider(provider: (List<PsiElement>) -> AiCopilot?) {
        runConfigProviders.add(provider)
    }
}