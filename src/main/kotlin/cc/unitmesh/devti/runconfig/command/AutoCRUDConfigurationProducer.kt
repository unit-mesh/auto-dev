package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.language.DevtiAnnotator
import cc.unitmesh.devti.runconfig.DtRunConfiguration
import cc.unitmesh.devti.runconfig.config.DevtiAutoCRUDConfigure
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

class AutoCRUDConfigurationProducer : BaseLazyRunConfigurationProducer<DevtiAutoCRUDConfigure>() {
    init {
        registerConfigProvider { elements -> createConfigFor(elements) }
    }

    private fun createConfigFor(
        elements: List<PsiElement>
    ): DevtiAutoCRUDConfigure? {
        if (elements.isEmpty()) return null
        val comments = elements.filterIsInstance<PsiComment>()
        if (comments.isEmpty()) return null

        val commentText = comments.first().text
        val storyConfig = DevtiAnnotator.matchByString(commentText) ?: return null
        return DevtiAutoCRUDConfigure.fromStoryConfig(storyConfig)
    }

    override fun isConfigurationFromContext(configuration: DtRunConfiguration, context: ConfigurationContext): Boolean {
        val config = findConfig(context.location?.psiElement?.let { listOf(it) } ?: return false) ?: return false
        configuration.name = config.configurationName + "(Create)"
        configuration.setStoryConfig(config)

        return true
    }

    override fun setupConfigurationFromContext(
        configuration: DtRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val config = findConfig(context.location?.psiElement?.let { listOf(it) } ?: return false) ?: return false
        configuration.name = config.configurationName + "(Create)"
        configuration.setStoryConfig(config)

        return true
    }

    private fun registerConfigProvider(provider: (List<PsiElement>) -> DevtiAutoCRUDConfigure?) {
        runConfigProviders.add(provider)
    }
}