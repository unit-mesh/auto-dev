package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.analysis.JavaAutoCrud
import cc.unitmesh.devti.runconfig.config.DevtiCreateStoryConfigure
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

class DtRunState(
    val environment: ExecutionEnvironment,
    private val configuration: DtRunConfiguration,
    private val createStory: DevtiCreateStoryConfigure?,
    val project: Project
) : RunProfileState {
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        val javaAuto = JavaAutoCrud(project)
        val controllerList = javaAuto.controllerList()

        log.warn(controllerList.toString())

        log.warn(configuration.toString())
        log.warn(createStory.toString())
        return null
    }

    companion object {
        private val log: Logger = logger<DtRunState>()
    }
}