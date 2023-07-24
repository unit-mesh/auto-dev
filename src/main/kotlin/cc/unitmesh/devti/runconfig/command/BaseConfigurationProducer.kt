package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.runconfig.AutoDevConfigurationType
import cc.unitmesh.devti.runconfig.config.AutoDevConfiguration
import cc.unitmesh.devti.prompting.model.AutoDevStory
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

abstract class BaseConfigurationProducer : BaseLazyRunConfigurationProducer<AutoDevStory, AutoDevConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return AutoDevConfigurationType.getInstance().factory
    }

    public abstract override fun setupConfigurationFromContext(
        configuration: AutoDevConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean
}