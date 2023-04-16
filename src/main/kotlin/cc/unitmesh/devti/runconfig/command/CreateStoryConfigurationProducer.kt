package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.language.StoryConfig
import cc.unitmesh.devti.runconfig.config.DevtiCreateStoryConfigure
import com.intellij.psi.PsiElement

class CreateStoryConfigurationProducer : BaseLazyRunConfigurationProducer<DevtiCreateStoryConfigure>() {
    init {
        registerConfigProvider { elements -> createConfigFor(elements) }
    }

    private fun createConfigFor(
        elements: List<PsiElement>
    ): DevtiCreateStoryConfigure {
        return DevtiCreateStoryConfigure.getDefault()
    }

    private fun registerConfigProvider(provider: (List<PsiElement>) -> DevtiCreateStoryConfigure?) {
        runConfigProviders.add(provider)
    }
}