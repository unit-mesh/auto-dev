package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.runconfig.config.AutoCRUDConfiguration
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.ui.ExecutionUiService
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager

class AutoDevCommandRunner : GenericProgramRunner<RunnerSettings>() {
    companion object {
        const val RUNNER_ID: String = "AutoDevCommandRunner"
    }

    override fun getRunnerId() = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return !(executorId != DefaultRunExecutor.EXECUTOR_ID || profile !is AutoCRUDConfiguration)
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.runProfile !is AutoCRUDConfiguration) return null
        FileDocumentManager.getInstance().saveAllDocuments()
        return showRunContent(state.execute(environment.executor, this), environment)
    }

    @Suppress("UnstableApiUsage")
    private fun showRunContent(result: ExecutionResult?, environment: ExecutionEnvironment): RunContentDescriptor? {
        return result?.let {
            ExecutionUiService.getInstance().showRunContent(it, environment)
        }
    }
}
