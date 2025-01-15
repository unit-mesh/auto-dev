package cc.unitmesh.devti.language.compiler.service

import cc.unitmesh.devti.language.run.DevInsConfiguration
import cc.unitmesh.devti.language.run.DevInsConfigurationType
import cc.unitmesh.devti.language.status.DevInsRunListener
import cc.unitmesh.devti.provider.RunService
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import java.util.concurrent.CompletableFuture

class DevInRunService : RunService {
    override fun isApplicable(project: Project, file: VirtualFile): Boolean = file.extension == "devin"

    override fun runConfigurationClass(project: Project): Class<out RunProfile> = DevInsConfiguration::class.java

    override fun createRunSettings(
        project: Project,
        file: VirtualFile,
        testElement: PsiElement?
    ): RunnerAndConfigurationSettings? {
        val settings = RunManager.getInstance(project)
            .createConfiguration(file.name, DevInsConfigurationType::class.java)

        val runConfiguration = settings
            .configuration as DevInsConfiguration

        runConfiguration.setScriptPath(file.path)
        runConfiguration.name = file.nameWithoutExtension

        settings.isTemporary = true

        return settings
    }

    override fun runFile(
        project: Project,
        virtualFile: VirtualFile,
        psiElement: PsiElement?,
        isFromToolAction: Boolean
    ): String? {
//        if (isFromToolAction) {
//            return runDevInsFile(project, virtualFile, psiElement)
//        }
        val settings = createRunSettings(project, virtualFile, psiElement) ?: return null
        val runConfiguration = settings.configuration as DevInsConfiguration

        runConfiguration.showConsole = !isFromToolAction

        val executorInstance = DefaultRunExecutor.getRunExecutorInstance()
        val builder = ExecutionEnvironmentBuilder.createOrNull(executorInstance, runConfiguration)
            ?: return null

        ExecutionManager.getInstance(project).restartRunProfile(builder.build())
        return "Running DevIn file: ${virtualFile.name}"
    }

    private fun runDevInsFile(project: Project, virtualFile: VirtualFile, psiElement: PsiElement?): String? {
        val settings = createRunSettings(project, virtualFile, psiElement) ?: return null
        val runConfiguration = settings.configuration as DevInsConfiguration

        val executorInstance = DefaultRunExecutor.getRunExecutorInstance()
        val executionEnvironment = ExecutionEnvironmentBuilder
            .createOrNull(executorInstance, runConfiguration)
            ?.build()

        if (executionEnvironment == null) {
            logger<DevInRunService>().error("Failed to create execution environment")
            return null
        }

        val future = CompletableFuture<String>()

        val hintDisposable = Disposer.newDisposable()
        val connection = ApplicationManager.getApplication().messageBus.connect(hintDisposable)
        connection.subscribe(DevInsRunListener.TOPIC, object : DevInsRunListener {
            override fun runFinish(
                string: String,
                event: ProcessEvent,
                scriptPath: String
            ) {
                future.complete(string)
                /// append to input box
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