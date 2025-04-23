package cc.unitmesh.devti.language.debugger

import cc.unitmesh.devti.language.run.DevInsConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager

/// refs to: https://github.com/KronicDeth/intellij-elixir/pull/643/files#diff-b1ba5c87ca6f66a455e4c1539cb2d99a62722d067a3d9e8043b290426cea5470
class ShireDebugRunner : GenericProgramRunner<RunnerSettings>() {
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return (executorId == DefaultDebugExecutor.EXECUTOR_ID) && profile is DevInsConfiguration
    }

    override fun getRunnerId(): String = RUNNER_ID

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        val xDebuggerManager = XDebuggerManager.getInstance(environment.project)
        return xDebuggerManager.startSession(environment, object : XDebugProcessStarter() {
            override fun start(session: XDebugSession): XDebugProcess {
                val shireDebugProcess = ShireDebugProcess(session, environment)
                shireDebugProcess.start()
                return shireDebugProcess
            }
        }).runContentDescriptor
    }
}