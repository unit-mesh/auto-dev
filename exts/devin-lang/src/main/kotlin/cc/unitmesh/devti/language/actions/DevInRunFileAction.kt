package cc.unitmesh.devti.language.actions

import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.run.AutoDevConfiguration
import cc.unitmesh.devti.language.run.AutoDevConfigurationType
import cc.unitmesh.devti.language.run.AutoDevRunConfigurationProducer
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.annotations.NonNls

class DevInRunFileAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        e.presentation.isEnabledAndVisible = file is DevInFile
        logger<DevInRunFileAction>().warn("update: ${e.presentation.text}")

        if (e.presentation.text.isNullOrBlank()) {
            e.presentation.text = "Run DevIn file: ${file.name}"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return

        val project = file.project
        val context = ConfigurationContext.getFromContext(e.dataContext, e.place)

        val configProducer = RunConfigurationProducer.getInstance(AutoDevRunConfigurationProducer::class.java)

        val configurationSettings = configProducer.findExistingConfiguration(context)
        val runConfiguration = if (configurationSettings == null) {
            RunManager.getInstance(project).createConfiguration(
                file.name,
                AutoDevConfigurationType::class.java
            )
        } else {
            configurationSettings
        }.configuration as AutoDevConfiguration

        runConfiguration.setScriptPath(file.virtualFile.path)

        val builder =
            ExecutionEnvironmentBuilder.createOrNull(DefaultRunExecutor.getRunExecutorInstance(), runConfiguration)
        if (builder != null) {
            ExecutionManager.getInstance(project).restartRunProfile(builder.build())
        }
    }

    companion object {
        val ID: @NonNls String = "runDevInFileAction"
    }

}
