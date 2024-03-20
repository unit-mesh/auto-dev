package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.DevInBundle
import cc.unitmesh.devti.language.DevInIcons
import cc.unitmesh.devti.language.DevInLanguage
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue

class DevInsConfigurationType : SimpleConfigurationType(
    "DevInsConfigurationType",
    DevInLanguage.INSTANCE.id,
    DevInBundle.message("devin.line.marker.run.0", DevInLanguage.INSTANCE.id),
    NotNullLazyValue.lazy { DevInIcons.DEFAULT }
) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        DevInsConfiguration(project, this, "DevInsConfiguration")

    companion object {
        fun getInstance(): DevInsConfigurationType {
            return findConfigurationType(DevInsConfigurationType::class.java)
        }
    }
}
