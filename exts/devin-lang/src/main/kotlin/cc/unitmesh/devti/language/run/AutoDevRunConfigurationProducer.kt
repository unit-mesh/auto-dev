package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.psi.DevInFile
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class AutoDevRunConfigurationProducer : LazyRunConfigurationProducer<AutoDevConfiguration>() {
    override fun getConfigurationFactory(): AutoDevConfigurationFactory {
        return AutoDevConfigurationFactory(AutoDevConfigurationType.getInstance())
    }

    override fun setupConfigurationFromContext(
        configuration: AutoDevConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val psiFile = sourceElement.get().containingFile as? DevInFile ?: return false
        val virtualFile = psiFile.virtualFile ?: return false

        return true
    }

    override fun isConfigurationFromContext(
        configuration: AutoDevConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val psiLocation = context.psiLocation ?: return false
        val psiFile = psiLocation.containingFile as? DevInFile ?: return false
        val virtualFile = psiFile.virtualFile ?: return false

        return true
    }

}