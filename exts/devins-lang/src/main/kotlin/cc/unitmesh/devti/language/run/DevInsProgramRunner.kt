package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.run.flow.DevInsFlowProcessor
import cc.unitmesh.devti.language.status.DevInsRunListener
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.runners.showRunContent
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import java.util.concurrent.atomic.AtomicReference

class DevInsProgramRunner : GenericProgramRunner<RunnerSettings>(), Disposable {
    private val RUNNER_ID: String = "DevInsProgramRunner"

    private val connection = ApplicationManager.getApplication().messageBus.connect(this)

    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile) = profile is DevInsConfiguration

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.runProfile !is DevInsConfiguration) return null
        val devInState = environment.runProfile as DevInsRunConfigurationProfileState

        FileDocumentManager.getInstance().saveAllDocuments()

        val result = AtomicReference<RunContentDescriptor>()
        connection.subscribe(DevInsRunListener.TOPIC, object : DevInsRunListener {
            override fun runFinish(string: String, event: ProcessEvent, scriptPath: String) {
                environment.project.service<DevInsFlowProcessor>().process(string, event, scriptPath)
            }
        })

        ApplicationManager.getApplication().invokeAndWait {
            val showRunContent = showRunContent(devInState.execute(environment.executor, this), environment)
            result.set(showRunContent)
        }

        return result.get()
    }

    override fun dispose() {}
}
