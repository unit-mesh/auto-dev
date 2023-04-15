package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.runconfig.DevtiConfigure
import com.intellij.psi.PsiElement

class CreateStoryConfigurationProducer : BaseLazyRunConfigurationProducer<DevtiConfigure>() {
    init {
        registerConfigProvider { elements -> createConfigFor(elements) }
    }

    private fun createConfigFor(
        elements: List<PsiElement>
    ): DevtiConfigure {
        return DevtiConfigure.getDefault()
    }

    private fun registerConfigProvider(provider: (List<PsiElement>) -> DevtiConfigure?) {
        runConfigProviders.add(provider)
    }
}