package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.process.*
import cc.unitmesh.devti.sketch.run.ShellUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * InsCommand implementation for launching processes
 */
class LaunchProcessInsCommand(
    private val project: Project,
    private val prop: String,
    private val codeContent: String?
) : InsCommand {
    
    override val commandName: BuiltinCommand = BuiltinCommand.LAUNCH_PROCESS
    private val logger = logger<LaunchProcessInsCommand>()
    
    override suspend fun execute(): String? {
        return try {
            val request = parseRequest(prop, codeContent)
            val result = launchProcess(request)
            formatResult(result)
        } catch (e: Exception) {
            logger.warn("Failed to launch process", e)
            "Error launching process: ${e.message}"
        }
    }
    
    private fun parseRequest(prop: String, codeContent: String?): LaunchProcessRequest {
        // Parse the prop string for parameters
        // Format: command [--working-dir=path] [--timeout=seconds] [--wait] [--show-terminal]
        val parts = prop.split(" ")
        val command = codeContent ?: parts.firstOrNull() ?: ""
        
        var workingDirectory = project.basePath ?: ""
        var timeoutSeconds = 30
        var waitForCompletion = false
        var showInTerminal = false
        val environment = mutableMapOf<String, String>()
        
        // Parse additional parameters
        parts.forEach { part ->
            when {
                part.startsWith("--working-dir=") -> {
                    workingDirectory = part.substringAfter("=")
                }
                part.startsWith("--timeout=") -> {
                    timeoutSeconds = part.substringAfter("=").toIntOrNull() ?: 30
                }
                part == "--wait" -> {
                    waitForCompletion = true
                }
                part == "--show-terminal" -> {
                    showInTerminal = true
                }
                part.startsWith("--env=") -> {
                    val envPart = part.substringAfter("=")
                    val (key, value) = envPart.split("=", limit = 2)
                    environment[key] = value
                }
            }
        }
        
        return LaunchProcessRequest(
            command = command,
            workingDirectory = workingDirectory,
            environment = environment,
            waitForCompletion = waitForCompletion,
            timeoutSeconds = timeoutSeconds,
            showInTerminal = showInTerminal
        )
    }
    
    private suspend fun launchProcess(request: LaunchProcessRequest): ProcessExecutionResult {
        val processStateManager = ProcessStateManager.getInstance(project)
        val processId = processStateManager.generateProcessId()
        
        return withContext(Dispatchers.IO) {
            try {
                // Create command line
                val commandLine = createCommandLine(request)
                
                // Create process info
                val processInfo = ProcessInfo(
                    processId = processId,
                    command = request.command,
                    workingDirectory = request.workingDirectory,
                    status = ProcessStatus.RUNNING,
                    startTime = System.currentTimeMillis(),
                    environment = request.environment,
                    waitForCompletion = request.waitForCompletion,
                    timeoutSeconds = request.timeoutSeconds,
                    showInTerminal = request.showInTerminal
                )
                
                if (request.waitForCompletion) {
                    // Execute and wait for completion
                    executeAndWait(commandLine, processInfo, request.timeoutSeconds)
                } else {
                    // Execute in background
                    executeInBackground(commandLine, processInfo, processStateManager)
                }
                
            } catch (e: Exception) {
                logger.warn("Failed to launch process: ${request.command}", e)
                ProcessExecutionResult(
                    processId = processId,
                    exitCode = -1,
                    stderr = "Failed to launch process: ${e.message}",
                    status = ProcessStatus.FAILED
                )
            }
        }
    }
    
    private fun createCommandLine(request: LaunchProcessRequest): GeneralCommandLine {
        val commandLine = GeneralCommandLine()
        commandLine.withCharset(StandardCharsets.UTF_8)
        commandLine.withWorkDirectory(File(request.workingDirectory))
        
        // Add environment variables
        request.environment.forEach { (key, value) ->
            commandLine.withEnvironment(key, value)
        }
        
        // Parse command into executable and arguments
        val shell = ShellUtil.detectShells().firstOrNull() ?: "bash"
        commandLine.exePath = shell
        commandLine.addParameters("--noprofile", "--norc", "-c", request.command)
        
        return commandLine
    }
    
    private suspend fun executeAndWait(
        commandLine: GeneralCommandLine,
        processInfo: ProcessInfo,
        timeoutSeconds: Int
    ): ProcessExecutionResult {
        val processStateManager = ProcessStateManager.getInstance(project)
        
        return try {
            val processHandler = CapturingProcessHandler(commandLine)
            processStateManager.registerProcess(processInfo, processHandler)
            
            // Add process listener to capture output
            val outputCapture = StringBuilder()
            val errorCapture = StringBuilder()
            
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun onTextAvailable(event: ProcessEvent, outputType: com.intellij.openapi.util.Key<*>) {
                    when (outputType) {
                        ProcessOutputTypes.STDOUT -> {
                            outputCapture.append(event.text)
                            processStateManager.appendStdout(processInfo.processId, event.text)
                        }
                        ProcessOutputTypes.STDERR -> {
                            errorCapture.append(event.text)
                            processStateManager.appendStderr(processInfo.processId, event.text)
                        }
                    }
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    val status = if (event.exitCode == 0) ProcessStatus.COMPLETED else ProcessStatus.FAILED
                    processStateManager.updateProcessStatus(processInfo.processId, status, event.exitCode)
                }
            })
            
            // Start process and wait
            val processOutput = processHandler.runProcess(timeoutSeconds * 1000)
            
            val status = when {
                processOutput.isTimeout -> ProcessStatus.TIMED_OUT
                processOutput.exitCode == 0 -> ProcessStatus.COMPLETED
                else -> ProcessStatus.FAILED
            }
            
            processStateManager.updateProcessStatus(processInfo.processId, status, processOutput.exitCode)
            
            ProcessExecutionResult(
                processId = processInfo.processId,
                exitCode = processOutput.exitCode,
                stdout = processOutput.stdout,
                stderr = processOutput.stderr,
                timedOut = processOutput.isTimeout,
                status = status
            )
            
        } catch (e: Exception) {
            processStateManager.updateProcessStatus(processInfo.processId, ProcessStatus.FAILED, -1)
            throw e
        }
    }
    
    private fun executeInBackground(
        commandLine: GeneralCommandLine,
        processInfo: ProcessInfo,
        processStateManager: ProcessStateManager
    ): ProcessExecutionResult {
        // Execute in background using application thread pool
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val processHandler = CapturingProcessHandler(commandLine)
                processStateManager.registerProcess(processInfo, processHandler, processHandler.processInput)
                
                // Add process listener
                processHandler.addProcessListener(object : ProcessAdapter() {
                    override fun onTextAvailable(event: ProcessEvent, outputType: com.intellij.openapi.util.Key<*>) {
                        when (outputType) {
                            ProcessOutputTypes.STDOUT -> {
                                processStateManager.appendStdout(processInfo.processId, event.text)
                            }
                            ProcessOutputTypes.STDERR -> {
                                processStateManager.appendStderr(processInfo.processId, event.text)
                            }
                        }
                    }
                    
                    override fun processTerminated(event: ProcessEvent) {
                        val status = if (event.exitCode == 0) ProcessStatus.COMPLETED else ProcessStatus.FAILED
                        processStateManager.updateProcessStatus(processInfo.processId, status, event.exitCode)
                    }
                })
                
                // Start process
                processHandler.startNotify()
                
                AutoDevNotifications.notify(project, "Process launched: ${processInfo.processId}")
                
            } catch (e: Exception) {
                logger.warn("Failed to start background process", e)
                processStateManager.updateProcessStatus(processInfo.processId, ProcessStatus.FAILED, -1)
            }
        }
        
        return ProcessExecutionResult(
            processId = processInfo.processId,
            exitCode = null,
            status = ProcessStatus.RUNNING
        )
    }
    
    private fun formatResult(result: ProcessExecutionResult): String {
        return buildString {
            appendLine("Process ID: ${result.processId}")
            appendLine("Status: ${result.status}")
            
            result.exitCode?.let { 
                appendLine("Exit Code: $it")
            }
            
            if (result.stdout.isNotEmpty()) {
                appendLine("Stdout:")
                appendLine(result.stdout)
            }
            
            if (result.stderr.isNotEmpty()) {
                appendLine("Stderr:")
                appendLine(result.stderr)
            }
            
            if (result.timedOut) {
                appendLine("Process timed out")
            }
        }
    }
}
