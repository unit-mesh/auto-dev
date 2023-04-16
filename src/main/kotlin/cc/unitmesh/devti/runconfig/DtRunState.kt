package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.runconfig.config.DevtiCreateStoryConfigure
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger

class DtRunState(
    val environment: ExecutionEnvironment,
    val configuration: DtRunConfiguration,
    private val aiConfig: DevtiCreateStoryConfigure
) : RunProfileState {
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        log.warn(aiConfig.toString())
        return null
    }

    companion object {
        private val log: Logger = logger<DtRunState>()
    }
}