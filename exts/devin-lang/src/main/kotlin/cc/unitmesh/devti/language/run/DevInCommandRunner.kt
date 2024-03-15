package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.runconfig.AutoDevCommandRunner
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.AsyncProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.annotations.NonNls
import org.jetbrains.concurrency.Promise

class DevInCommandRunner : AsyncProgramRunner<RunnerSettings>() {
    companion object {
        private val log: Logger = logger<DevInCommandRunner>()
        const val RUNNER_ID: String = "DevInCommandRunner"
    }

    override fun getRunnerId(): @NonNls String = AutoDevCommandRunner.RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return false
    }

    override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
        TODO("Not yet implemented")
    }
}
