package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.language.DevtiAnnotator
import cc.unitmesh.devti.runconfig.DtRunConfiguration
import cc.unitmesh.devti.runconfig.config.DevtiCreateStoryConfigure
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class CreateStoryConfigurationProducer : BaseLazyRunConfigurationProducer<DevtiCreateStoryConfigure>() {
    init {
        registerConfigProvider { elements -> createConfigFor(elements) }
    }

    private fun createConfigFor(
        elements: List<PsiElement>
    ): DevtiCreateStoryConfigure {
        val config = DevtiCreateStoryConfigure.getDefault()
        val commentText = elements.first().text
        config.storyConfig = DevtiAnnotator.matchByString(commentText) ?: return config
        return config
    }

    override fun setupConfigurationFromContext(
        configuration: DtRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val config = findConfig(context.location?.psiElement?.let { listOf(it) } ?: return false) ?: return false
        configuration.name = config.configurationName
        configuration.setStoryConfig(config.storyConfig)

        return true
    }

    private fun registerConfigProvider(provider: (List<PsiElement>) -> DevtiCreateStoryConfigure?) {
        runConfigProviders.add(provider)
    }
}