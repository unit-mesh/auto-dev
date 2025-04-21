package cc.unitmesh.devti.observer

import cc.unitmesh.devti.provider.observer.AgentObserver
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.messages.MessageBusConnection


class BuiltTaskAgentObserver : AgentObserver, Disposable {
    private var connection: MessageBusConnection? = null

    override fun onRegister(project: Project) {
        connection = project.messageBus.connect()
        connection?.subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
            private var globalBuffer = StringBuilder()

            override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
                handler.addProcessListener(object : ProcessListener {
                    private val outputBuffer = StringBuilder()
                    override fun onTextAvailable(
                        event: ProcessEvent,
                        outputType: Key<*>
                    ) {
                        outputBuffer.append(event.text)
                    }

                    override fun processTerminated(event: ProcessEvent) {
                        globalBuffer = outputBuffer
                    }
                })
            }

            override fun processTerminated(
                executorId: String,
                env: ExecutionEnvironment,
                handler: ProcessHandler,
                exitCode: Int
            ) {
                val globalBuffer = globalBuffer.toString().trim()
                if (globalBuffer.isEmpty()) {
                    return
                }

                if (exitCode == 1 && globalBuffer.contains("BUILD FAILED")) {
                    val prompt = "Help Me fix follow build issue:\n```bash\n$globalBuffer\n```\n"
                    sendErrorNotification(project, prompt)
                    return
                }

                val isSpringFailureToStart =
                    globalBuffer.contains("***************************") && globalBuffer.contains("APPLICATION FAILED TO START")
                if (isSpringFailureToStart) {
                    val prompt = "Help Me fix follow build issue:\n```bash\n$globalBuffer\n```\n"
                    sendErrorNotification(project, prompt)
                }
            }
        })
    }

    override fun dispose() {
        connection?.disconnect()
    }
}
