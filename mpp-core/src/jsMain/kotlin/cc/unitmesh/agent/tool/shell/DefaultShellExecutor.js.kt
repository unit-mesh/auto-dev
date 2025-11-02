package cc.unitmesh.agent.tool.shell

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * JavaScript platform implementation of shell executor
 * Uses JsShellExecutor for Node.js environment
 */
actual class DefaultShellExecutor : ShellExecutor {
    
    private val jsExecutor = JsShellExecutor()

    actual override suspend fun execute(command: String, config: ShellExecutionConfig): ShellResult {
        return jsExecutor.execute(command, config)
    }

    actual override fun isAvailable(): Boolean = jsExecutor.isAvailable()

    actual override fun getDefaultShell(): String? = jsExecutor.getDefaultShell()
}

/**
 * Node.js-based shell executor using child_process
 */
class JsShellExecutor : ShellExecutor {

    override suspend fun execute(command: String, config: ShellExecutionConfig): ShellResult {
        if (!isAvailable()) {
            throw ToolException(
                "Node.js child_process module is not available",
                ToolErrorType.NOT_SUPPORTED
            )
        }

        return executeNodeCommand(command, config).await()
    }

    override fun isAvailable(): Boolean {
        return try {
            js("typeof require !== 'undefined' && typeof require('child_process') !== 'undefined'") as Boolean
        } catch (e: Exception) {
            false
        }
    }

    override fun getDefaultShell(): String? {
        return when (js("process.platform") as String) {
            "win32" -> "cmd.exe"
            else -> "/bin/sh"
        }
    }

    private fun executeNodeCommand(command: String, config: ShellExecutionConfig): Promise<ShellResult> {
        return Promise { resolve, reject ->
            try {
                val childProcess = js("require('child_process')")
                val startTime = js("Date.now()") as Double

                val options = js("{}")
                options.shell = config.shell ?: getDefaultShell()
                config.workingDirectory?.let { options.cwd = it }
                if (config.environment.isNotEmpty()) {
                    options.env = js("{}")
                    config.environment.forEach { (key, value) ->
                        options.env[key] = value
                    }
                }
                // Convert Long to Number for JavaScript
                options.timeout = config.timeoutMs.toDouble()

                childProcess.exec(command, options) { error: dynamic, stdout: dynamic, stderr: dynamic ->
                    val endTime = js("Date.now()") as Double
                    val executionTime = (endTime - startTime).toLong()

                    val exitCode = if (error != null) {
                        (error.code as? Int) ?: 1
                    } else {
                        0
                    }

                    val result = ShellResult(
                        exitCode = exitCode,
                        stdout = stdout?.toString() ?: "",
                        stderr = stderr?.toString() ?: "",
                        command = command,
                        workingDirectory = config.workingDirectory,
                        executionTimeMs = executionTime
                    )

                    resolve(result)
                }
            } catch (e: Exception) {
                reject(e)
            }
        }
    }
}
