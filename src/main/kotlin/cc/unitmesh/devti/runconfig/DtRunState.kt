package cc.unitmesh.devti.runconfig

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RemoteState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner

class DtRunState(
    environment: ExecutionEnvironment,
    configuration: DtCommandConfiguration,
    aiConfig: DtAiConfigure
): RemoteState {
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        return null
    }

    override fun getRemoteConnection(): RemoteConnection {
        return RemoteConnection(false, "localhost", 8080.toString(), false)
    }
}