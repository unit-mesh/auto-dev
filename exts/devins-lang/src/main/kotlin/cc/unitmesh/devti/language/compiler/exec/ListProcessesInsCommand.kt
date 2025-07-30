package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.process.ProcessStateManager
import cc.unitmesh.devti.process.ProcessStatus
import com.intellij.openapi.project.Project
import java.text.SimpleDateFormat
import java.util.*

/**
 * InsCommand implementation for listing processes
 */
class ListProcessesInsCommand(
    private val project: Project,
    private val prop: String
) : InsCommand {
    
    override val commandName: BuiltinCommand = BuiltinCommand.LIST_PROCESSES
    
    override suspend fun execute(): String? {
        val processStateManager = ProcessStateManager.getInstance(project)
        
        // Parse parameters
        val includeTerminated = prop.contains("--include-terminated") || prop.contains("--all")
        val maxResults = extractMaxResults(prop)
        
        val processes = processStateManager.getAllProcesses(includeTerminated)
            .sortedByDescending { it.startTime }
            .take(maxResults)
        
        if (processes.isEmpty()) {
            return "No processes found."
        }
        
        return formatProcessList(processes)
    }
    
    private fun extractMaxResults(prop: String): Int {
        val maxResultsRegex = "--max-results=(\\d+)".toRegex()
        val match = maxResultsRegex.find(prop)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 50
    }
    
    private fun formatProcessList(processes: List<cc.unitmesh.devti.process.ProcessInfo>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        return buildString {
            appendLine("Process List (${processes.size} processes):")
            appendLine("=" .repeat(80))
            appendLine()
            
            processes.forEach { process ->
                appendLine("Process ID: ${process.processId}")
                appendLine("Command: ${process.command}")
                appendLine("Working Directory: ${process.workingDirectory}")
                appendLine("Status: ${formatStatus(process.status)}")
                
                process.exitCode?.let { exitCode ->
                    appendLine("Exit Code: $exitCode")
                }
                
                appendLine("Start Time: ${dateFormat.format(Date(process.startTime))}")
                
                process.endTime?.let { endTime ->
                    appendLine("End Time: ${dateFormat.format(Date(endTime))}")
                    val duration = endTime - process.startTime
                    appendLine("Duration: ${formatDuration(duration)}")
                }
                
                if (process.environment.isNotEmpty()) {
                    appendLine("Environment Variables:")
                    process.environment.forEach { (key, value) ->
                        appendLine("  $key=$value")
                    }
                }
                
                if (process.waitForCompletion) {
                    appendLine("Wait for Completion: Yes (Timeout: ${process.timeoutSeconds}s)")
                }
                
                if (process.showInTerminal) {
                    appendLine("Show in Terminal: Yes")
                }
                
                appendLine("-".repeat(40))
                appendLine()
            }
            
            // Summary
            val runningCount = processes.count { it.status == ProcessStatus.RUNNING }
            val completedCount = processes.count { it.status == ProcessStatus.COMPLETED }
            val failedCount = processes.count { it.status == ProcessStatus.FAILED }
            val killedCount = processes.count { it.status == ProcessStatus.KILLED }
            val timedOutCount = processes.count { it.status == ProcessStatus.TIMED_OUT }
            
            appendLine("Summary:")
            appendLine("  Running: $runningCount")
            appendLine("  Completed: $completedCount")
            appendLine("  Failed: $failedCount")
            appendLine("  Killed: $killedCount")
            appendLine("  Timed Out: $timedOutCount")
        }
    }
    
    private fun formatStatus(status: ProcessStatus): String {
        return when (status) {
            ProcessStatus.RUNNING -> "🟢 RUNNING"
            ProcessStatus.COMPLETED -> "✅ COMPLETED"
            ProcessStatus.FAILED -> "❌ FAILED"
            ProcessStatus.KILLED -> "🛑 KILLED"
            ProcessStatus.TIMED_OUT -> "⏰ TIMED_OUT"
        }
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}
