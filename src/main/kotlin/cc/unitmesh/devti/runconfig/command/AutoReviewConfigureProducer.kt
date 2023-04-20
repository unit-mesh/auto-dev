package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.language.DevtiAnnotator
import cc.unitmesh.devti.runconfig.AutoCRUDConfigurationType
import cc.unitmesh.devti.runconfig.config.AutoCRUDConfiguration
import cc.unitmesh.devti.runconfig.config.DevtiStory
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

open class AutoReviewConfigureProducer : DevtiConfigurationProducer() {
    init {
        registerConfigProvider { elements -> createConfigFor(elements) }
    }

    override fun getConfigurationFactory(): ConfigurationFactory {
        return AutoCRUDConfigurationType.getInstance().factory
    }

    private fun createConfigFor(
        elements: List<PsiElement>
    ): DevtiStory? {
        if (elements.isEmpty()) return null
        val comments = elements.filterIsInstance<PsiComment>()
        if (comments.isEmpty()) return null

        val commentText = comments.first().text
        val storyConfig = DevtiAnnotator.matchByString(commentText) ?: return null
        return DevtiStory.fromStoryConfig(storyConfig)
    }

    override fun isConfigurationFromContext(configuration: AutoCRUDConfiguration, context: ConfigurationContext): Boolean {
        val config = findConfig(context.location?.psiElement?.let { listOf(it) } ?: return false) ?: return false
        configuration.name = config.configurationName + "(Review)"
        configuration.setStoryConfig(config)

        return true
    }

    override fun setupConfigurationFromContext(
        configuration: AutoCRUDConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val config = findConfig(context.location?.psiElement?.let { listOf(it) } ?: return false) ?: return false
        configuration.name = config.configurationName + "(Review)"
        configuration.setStoryConfig(config)

        return true
    }

    private fun registerConfigProvider(provider: (List<PsiElement>) -> DevtiStory?) {
        runConfigProviders.add(provider)
    }
}