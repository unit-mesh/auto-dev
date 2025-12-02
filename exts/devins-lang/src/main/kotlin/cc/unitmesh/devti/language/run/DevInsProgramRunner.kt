package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.run.flow.DevInsProcessProcessor
import cc.unitmesh.devti.language.run.runner.ShireConsoleView
import cc.unitmesh.devti.language.status.DevInsRunListener
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.runners.showRunContent
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.MessageBusConnection
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class DevInsProgramRunner : GenericProgramRunner<RunnerSettings>() {
    private val RUNNER_ID: String = "DevInsProgramRunner"

    // Use lazy initialization to avoid memory leak - connection is created per execution
    // and tied to the project's lifecycle, not the runner's lifecycle
    private var connection: MessageBusConnection? = null
    private var isSubscribed = false

    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile) = profile is DevInsConfiguration

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.runProfile !is DevInsConfiguration) return null
        val shireState = state as DevInsRunConfigurationProfileState

        var executeResult: ExecutionResult?

        val result = AtomicReference<RunContentDescriptor>()

        ApplicationManager.getApplication().invokeAndWait {
            if (!isSubscribed) {
                // Connect to project's message bus instead of application's
                // This ensures proper disposal when the project is closed
                val projectConnection = environment.project.messageBus.connect()
                connection = projectConnection

                // Register for disposal with the project
                Disposer.register(environment.project, projectConnection)

                projectConnection.subscribe(DevInsRunListener.TOPIC, object : DevInsRunListener {
                    override fun runFinish(
                        allOutput: String,
                        llmOutput: String,
                        event: ProcessEvent,
                        scriptPath: String,
                        consoleView: ShireConsoleView?,
                    ) {
                        AutoDevCoroutineScope.scope(environment.project).launch {
                            environment.project.getService(DevInsProcessProcessor::class.java)
                                .process(allOutput, event, scriptPath, consoleView)
                        }
                    }
                })

                isSubscribed = true
            }

            executeResult = shireState.execute(environment.executor, this)

            if (shireState.isShowRunContent) {
                result.set(showRunContent(executeResult, environment))
            }
        }

        return result.get()
    }
}
