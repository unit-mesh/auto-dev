package cc.unitmesh.devti.language.actions

import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.run.DevInsConfiguration
import cc.unitmesh.devti.language.run.DevInsConfigurationType
import cc.unitmesh.devti.language.run.DevInsRunConfigurationProducer
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.annotations.NonNls

class DevInsRunFileAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        e.presentation.isEnabledAndVisible = file is DevInFile

        if (e.presentation.text.isNullOrBlank()) {
            e.presentation.text = "Run DevIn file: ${file.name}"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val project = file.project

        val context = ConfigurationContext.getFromContext(e.dataContext, e.place)
        val configProducer = RunConfigurationProducer.getInstance(DevInsRunConfigurationProducer::class.java)

        val runConfiguration = (configProducer.findExistingConfiguration(context)
            ?: RunManager.getInstance(project)
                .createConfiguration(file.name, DevInsConfigurationType::class.java)
                ).configuration as DevInsConfiguration

        runConfiguration.setScriptPath(file.virtualFile.path)

        val executorInstance = DefaultRunExecutor.getRunExecutorInstance()
        val builder = ExecutionEnvironmentBuilder.createOrNull(executorInstance, runConfiguration) ?: return

        ExecutionManager.getInstance(project).restartRunProfile(builder.build())
    }

    companion object {
        val ID: @NonNls String = "runDevInsFileAction"
    }

}
