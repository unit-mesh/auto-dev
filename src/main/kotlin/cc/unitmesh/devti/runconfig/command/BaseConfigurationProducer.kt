package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.runconfig.AutoCRUDConfigurationType
import cc.unitmesh.devti.runconfig.config.FeatureConfiguration
import cc.unitmesh.devti.runconfig.config.DevtiStory
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

abstract class BaseConfigurationProducer : BaseLazyRunConfigurationProducer<DevtiStory, FeatureConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return AutoCRUDConfigurationType.getInstance().factory
    }

    public abstract override fun setupConfigurationFromContext(
        configuration: FeatureConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean
}