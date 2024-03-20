package cc.unitmesh.devti.language.run

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.runners.showRunContent
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager

open class DevInsProgramRunner : GenericProgramRunner<RunnerSettings>() {
    companion object {
        const val RUNNER_ID: String = "DevInsProgramRunner"
    }

    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile) = profile is DevInsConfiguration

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.runProfile !is DevInsConfiguration) return null

        FileDocumentManager.getInstance().saveAllDocuments()
        return showRunContent(state.execute(environment.executor, this), environment)
    }
}
