package cc.unitmesh.devti.runconfig.command

import cc.unitmesh.devti.runconfig.DevtiConfigure
import cc.unitmesh.devti.runconfig.DtCommandConfiguration
import cc.unitmesh.devti.runconfig.DtCommandConfigurationType
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class CreateStoryConfigurationProducer : BaseLazyRunConfigurationProducer<DevtiConfigure>() {
    val configurationName: String = "DevTi Create Story"
    override fun getConfigurationFactory(): ConfigurationFactory {
        return DtCommandConfigurationType.getInstance().factory
    }

    override fun isConfigurationFromContext(
        configuration: DtCommandConfiguration,
        context: ConfigurationContext
    ): Boolean {
        return true
    }

    override fun setupConfigurationFromContext(
        configuration: DtCommandConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        return true
    }
}