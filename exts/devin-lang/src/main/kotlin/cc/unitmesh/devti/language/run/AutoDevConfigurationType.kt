package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.language.DevInIcons
import cc.unitmesh.devti.language.DevInLanguage
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue

class AutoDevConfigurationType : SimpleConfigurationType(
    "AutoDevConfigurationType",
    DevInLanguage.INSTANCE.id,
    AutoDevBundle.message("line.marker.run.0", DevInLanguage.INSTANCE.id),
    NotNullLazyValue.lazy { DevInIcons.DEFAULT }
) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        AutoDevConfiguration(project, this, "AutoDevConfiguration")

    companion object {
        fun getInstance(): AutoDevConfigurationType {
            return findConfigurationType(AutoDevConfigurationType::class.java)
        }
    }
}
