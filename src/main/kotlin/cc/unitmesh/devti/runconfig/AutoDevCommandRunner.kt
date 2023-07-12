package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.runconfig.config.AutoDevConfiguration
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.ui.ExecutionUiService
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.jetbrains.annotations.NonNls


class AutoDevCommandRunner : GenericProgramRunner<RunnerSettings>() {
    override fun getRunnerId(): @NonNls String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return !(executorId != DefaultRunExecutor.EXECUTOR_ID || profile !is AutoDevConfiguration)
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.runProfile !is AutoDevConfiguration) return null

        FileDocumentManager.getInstance().saveAllDocuments()
        return showRunContent(state.execute(environment.executor, this), environment)
    }

    @Suppress("UnstableApiUsage")
    private fun showRunContent(
        executionResult: ExecutionResult?,
        environment: ExecutionEnvironment
    ): RunContentDescriptor? {
        log.debug("showRunContent")

        return executionResult?.let {
            ExecutionUiService.getInstance().showRunContent(it, environment)
        }
    }

    companion object {
        private val log: Logger = logger<AutoDevState>()
        const val RUNNER_ID: String = "AutoDevCommandRunner"
    }
}
