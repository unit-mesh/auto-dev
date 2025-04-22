package cc.unitmesh.devti.language.actions

import cc.unitmesh.devti.language.startup.DynamicDevInActionConfig
import cc.unitmesh.devti.language.middleware.post.PostProcessorContext
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.run.DevInsConfiguration
import cc.unitmesh.devti.language.run.DevInsConfigurationType
import cc.unitmesh.devti.language.run.DevInsRunConfigurationProducer
import cc.unitmesh.devti.language.status.DevInsRunListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.NonNls
import java.util.concurrent.CompletableFuture

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

        fun suspendExecuteFile(
            project: Project,
            file: DevInFile,
            variableNames: Array<String> = arrayOf(),
            variableTable: MutableMap<String, Any?> = mutableMapOf(),
        ): String? {
            val variables: MutableMap<String, String> = mutableMapOf()
            for (i in variableNames.indices) {
                val varName = variableNames[i]
                val varValue = variableTable[varName].toString()
                variables[varName] = varValue
            }

            val config = DynamicDevInActionConfig.from(file)

            val settings = try {
                RunManager.getInstance(project)
                    .createConfiguration(config.name, DevInsConfigurationType::class.java)
            } catch (e: Exception) {
                logger<DevInsRunFileAction>().error("Failed to create configuration", e)
                return null
            }

            val runConfiguration = settings.configuration as DevInsConfiguration
            runConfiguration.setScriptPath(config.devinFile.virtualFile.path)
            if (variables.isNotEmpty()) {
                runConfiguration.setVariables(variables)
                PostProcessorContext.updateRunConfigVariables(variables)
            }

            val executorInstance = DefaultRunExecutor.getRunExecutorInstance()
            val executionEnvironment = ExecutionEnvironmentBuilder
                .createOrNull(executorInstance, runConfiguration)
                ?.build()

            if (executionEnvironment == null) {
                logger<DevInsRunFileAction>().error("Failed to create execution environment")
                return null
            }

            val future = CompletableFuture<String>()

            val hintDisposable = Disposer.newDisposable()
            val connection = ApplicationManager.getApplication().messageBus.connect(hintDisposable)
            connection.subscribe(DevInsRunListener.TOPIC, object : DevInsRunListener {
//                override fun runFinish(
//                    allOutput: String,
//                    llmOutput: String,
//                    event: ProcessEvent,
//                    scriptPath: String,
//                    consoleView: DevInConsoleView?,
//                ) {
//                    future.complete(llmOutput)
//                    connection.disconnect()
//                    Disposer.dispose(hintDisposable)
//                }
                override fun runFinish(
                    string: String,
                    event: ProcessEvent,
                    scriptPath: String
                ) {

                }
            })

            ExecutionManager.getInstance(project).restartRunProfile(
                project,
                executorInstance,
                executionEnvironment.executionTarget,
                settings,
                null
            )

            return future.get()
        }

    }

}
