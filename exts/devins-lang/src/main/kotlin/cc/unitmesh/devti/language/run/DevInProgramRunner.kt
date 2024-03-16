package cc.unitmesh.devti.language.run

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.runners.showRunContent
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager

open class DevInProgramRunner : GenericProgramRunner<RunnerSettings>() {
    companion object {
        const val RUNNER_ID: String = "DevInCommandRunner"
    }

    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return profile is AutoDevConfiguration
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.runProfile !is AutoDevConfiguration) return null

        FileDocumentManager.getInstance().saveAllDocuments()
        return showRunContent(state.execute(environment.executor, this), environment)
    }
}
