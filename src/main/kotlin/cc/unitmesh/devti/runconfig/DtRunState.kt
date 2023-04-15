package cc.unitmesh.devti.runconfig

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RemoteState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger

class DtRunState(
    val environment: ExecutionEnvironment,
    val configuration: DtCommandConfiguration,
    val aiConfig: DtAiConfigure
) : RemoteState {
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        log.debug("execute")
        log.info("....................")
        log.info(aiConfig.toString())
        return null
    }

    override fun getRemoteConnection(): RemoteConnection {
        return RemoteConnection(false, "localhost", 8080.toString(), false)
    }

    companion object {
        private val log: Logger = logger<DtRunState>()
    }
}