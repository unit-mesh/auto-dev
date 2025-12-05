package cc.unitmesh.devti.language.actions

import cc.unitmesh.devti.language.startup.DynamicDevInsActionConfig
import cc.unitmesh.devti.devins.post.PostProcessorContext
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.run.DevInsConfiguration
import cc.unitmesh.devti.language.run.DevInsConfigurationType
import cc.unitmesh.devti.language.run.DevInsRunConfigurationProducer
import cc.unitmesh.devti.language.run.runner.ShireConsoleView
import cc.unitmesh.devti.language.status.DevInsRunListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
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
        @NonNls
        val ID: String = "runDevInsFileAction"

        fun createRunConfig(e: AnActionEvent): RunnerAndConfigurationSettings? {
            val context = ConfigurationContext.getFromContext(e.dataContext, e.place)
            return RunConfigurationProducer.getInstance(DevInsRunConfigurationProducer::class.java)
                .findExistingConfiguration(context)
        }

        /**
         * Executes a Shire file within the specified project context.
         *
         * ```kotlin
         * val project = ... // IntelliJ IDEA project
         * val config = ... // DynamicShireActionConfig object
         * val runSettings = ... // Optional RunnerAndConfigurationSettings
         * val variables = mapOf("key1" to "value1", "key2" to "value2")
         *
         * executeFile(project, config, runSettings, variables)
         * ```
         *
         * @param project The IntelliJ IDEA project in which the Shire file is to be executed.
         * @param config The configuration object containing details about the Shire file to be executed.
         * @param runSettings Optional runner and configuration settings to use for execution. If null, a new configuration will be created.
         * @param variables A map of variables to be passed to the Shire file during execution. Defaults to an empty map.
         *
         * @throws Exception If there is an error creating the run configuration or execution environment.
         */
        fun executeFile(
            project: Project,
            config: DynamicDevInsActionConfig,
            runSettings: RunnerAndConfigurationSettings?,
            variables: Map<String, String> = mapOf(),
        ) {
            val settings = try {
                runSettings ?: RunManager.getInstance(project)
                    .createConfiguration(config.name, DevInsConfigurationType::class.java)
            } catch (e: Exception) {
                logger<DevInsRunFileAction>().error("Failed to create configuration", e)
                return
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
                return
            }

            ExecutionManager.getInstance(project).restartRunProfile(executionEnvironment)
        }

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

            val config = DynamicDevInsActionConfig.from(file)

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
                override fun runFinish(
                    allOutput: String,
                    llmOutput: String,
                    event: ProcessEvent,
                    scriptPath: String,
                    consoleView: ShireConsoleView?,
                ) {
                    future.complete(llmOutput)
                    connection.disconnect()
                    Disposer.dispose(hintDisposable)
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
