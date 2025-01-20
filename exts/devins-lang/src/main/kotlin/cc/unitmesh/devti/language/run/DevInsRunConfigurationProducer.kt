package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class DevInsRunConfigurationProducer : LazyRunConfigurationProducer<DevInsConfiguration>() {
    override fun getConfigurationFactory() = DevInsConfigurationType.getInstance()

    override fun setupConfigurationFromContext(
        configuration: DevInsConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val psiFile = sourceElement.get().containingFile as? DevInFile ?: return false
        val virtualFile = psiFile.virtualFile ?: return false

        configuration.name = virtualFile.presentableName
        configuration.setScriptPath(virtualFile.path)

        return true
    }

    override fun isConfigurationFromContext(
        configuration: DevInsConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val psiLocation = context.psiLocation ?: return false
        val psiFile = psiLocation.containingFile as? DevInFile ?: return false
        val virtualFile = psiFile.virtualFile ?: return false
        return virtualFile.path == configuration.getScriptPath()
    }

}