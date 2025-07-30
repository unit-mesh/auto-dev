package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.process.ProcessStateManager
import com.intellij.openapi.project.Project

/**
 * InsCommand implementation for reading process output
 */
class ReadProcessOutputInsCommand(
    private val project: Project,
    private val prop: String
) : InsCommand {
    
    override val commandName: BuiltinCommand = BuiltinCommand.READ_PROCESS_OUTPUT
    
    override suspend fun execute(): String? {
        val processStateManager = ProcessStateManager.getInstance(project)
        
        // Parse parameters
        val (processId, includeStdout, includeStderr, maxBytes) = parseParameters(prop)
        
        if (processId.isEmpty()) {
            return "Error: Process ID is required. Usage: /read-process-output:process_id [--stdout] [--stderr] [--max-bytes=N]"
        }
        
        // Check if process exists
        val processInfo = processStateManager.getProcess(processId)
        if (processInfo == null) {
            return "Error: Process '$processId' not found."
        }
        
        // Read process output
        val outputResponse = processStateManager.readProcessOutput(
            processId = processId,
            includeStdout = includeStdout,
            includeStderr = includeStderr,
            maxBytes = maxBytes
        )
        
        return formatOutput(processId, processInfo, outputResponse, includeStdout, includeStderr, maxBytes)
    }
    
    private fun parseParameters(prop: String): Tuple4<String, Boolean, Boolean, Int> {
        val parts = prop.trim().split(" ")
        val processId = parts.firstOrNull()?.trim() ?: ""
        
        var includeStdout = true
        var includeStderr = true
        var maxBytes = 10000
        
        // Parse flags
        parts.forEach { part ->
            when {
                part == "--stdout-only" -> {
                    includeStdout = true
                    includeStderr = false
                }
                part == "--stderr-only" -> {
                    includeStdout = false
                    includeStderr = true
                }
                part == "--no-stdout" -> {
                    includeStdout = false
                }
                part == "--no-stderr" -> {
                    includeStderr = false
                }
                part.startsWith("--max-bytes=") -> {
                    maxBytes = part.substringAfter("=").toIntOrNull() ?: 10000
                }
            }
        }
        
        return Tuple4(processId, includeStdout, includeStderr, maxBytes)
    }
    
    private fun formatOutput(
        processId: String,
        processInfo: cc.unitmesh.devti.process.ProcessInfo,
        outputResponse: cc.unitmesh.devti.process.ReadProcessOutputResponse,
        includeStdout: Boolean,
        includeStderr: Boolean,
        maxBytes: Int
    ): String {
        return buildString {
            appendLine("Process Output for: $processId")
            appendLine("Command: ${processInfo.command}")
            appendLine("Status: ${processInfo.status}")
            processInfo.exitCode?.let { appendLine("Exit Code: $it") }
            appendLine("=" .repeat(60))
            appendLine()
            
            if (includeStdout && outputResponse.stdout.isNotEmpty()) {
                appendLine("STDOUT:")
                appendLine("-".repeat(40))
                appendLine(outputResponse.stdout)
                appendLine()
            }
            
            if (includeStderr && outputResponse.stderr.isNotEmpty()) {
                appendLine("STDERR:")
                appendLine("-".repeat(40))
                appendLine(outputResponse.stderr)
                appendLine()
            }
            
            if (outputResponse.stdout.isEmpty() && outputResponse.stderr.isEmpty()) {
                appendLine("No output available for this process.")
                appendLine()
            }
            
            if (outputResponse.hasMore) {
                appendLine("Note: Output was truncated to $maxBytes bytes. Use --max-bytes=N to increase limit.")
            }
        }
    }
    
    // Helper data class for multiple return values
    private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
