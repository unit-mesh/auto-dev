package cc.unitmesh.devti.language.run

import com.intellij.execution.ExecutionManager
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.runners.showRunContent
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.jetbrains.annotations.NonNls

class DevInProgramRunner : ProgramRunner<RunnerSettings> {
    companion object {
        const val RUNNER_ID: String = "DevInCommandRunner"
    }

    override fun getRunnerId(): @NonNls String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return profile is AutoDevConfiguration
    }

    override fun execute(environment: ExecutionEnvironment) {
        ExecutionManager.getInstance(environment.project).startRunProfile(environment) { state: RunProfileState ->
            FileDocumentManager.getInstance().saveAllDocuments()
            showRunContent(state.execute(environment.executor, this), environment)
        }
    }
}
