package cc.unitmesh.devti.runconfig.config

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project

class CopilotState(
    environment: ExecutionEnvironment,
    aiCopilotConfiguration: AiCopilotConfiguration,
    project: Project
) : CommandLineState(environment) {
    override fun startProcess(): ProcessHandler {
        val commandLine = GeneralCommandLine("echo", "hello")
        val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        return processHandler
    }
}
