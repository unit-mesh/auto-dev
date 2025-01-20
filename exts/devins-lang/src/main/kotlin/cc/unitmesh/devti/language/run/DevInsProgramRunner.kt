package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.run.flow.DevInsProcessProcessor
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

    private var isSubscribed = false

    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile) = profile is DevInsConfiguration

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.runProfile !is DevInsConfiguration) return null
        val configuration = environment.runProfile as DevInsConfiguration
        val devInState = state as DevInsRunConfigurationProfileState

        FileDocumentManager.getInstance().saveAllDocuments()

        val result = AtomicReference<RunContentDescriptor>()

        //避免多次subscribe
        if (!isSubscribed) {
            connection.subscribe(DevInsRunListener.TOPIC, object : DevInsRunListener {
                override fun runFinish(string: String, event: ProcessEvent, scriptPath: String) {
                    environment.project.service<DevInsProcessProcessor>().process(string, event, scriptPath)
                }
            })

            isSubscribed = true
        }

        ApplicationManager.getApplication().invokeAndWait {
            val executionResult = devInState.execute(environment.executor, this)
            if (configuration.showConsole) {
                val showRunContent = showRunContent(executionResult, environment)
                result.set(showRunContent)
            } else {
                result.set(null)
            }
        }

        return result.get()
    }

    override fun dispose() {
        connection.disconnect()
    }
}
