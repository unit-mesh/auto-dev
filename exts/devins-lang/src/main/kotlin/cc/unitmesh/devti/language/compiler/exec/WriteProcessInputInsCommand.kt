package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.process.ProcessStateManager
import cc.unitmesh.devti.process.ProcessStatus
import com.intellij.openapi.project.Project

/**
 * InsCommand implementation for writing input to processes
 */
class WriteProcessInputInsCommand(
    private val project: Project,
    private val prop: String,
    private val codeContent: String?
) : InsCommand {
    
    override val commandName: BuiltinCommand = BuiltinCommand.WRITE_PROCESS_INPUT
    
    override suspend fun execute(): String? {
        val processStateManager = ProcessStateManager.getInstance(project)
        
        // Parse parameters
        val (processId, appendNewline) = parseParameters(prop)
        
        if (processId.isEmpty()) {
            return "Error: Process ID is required. Usage: /write-process-input:process_id [--no-newline]"
        }
        
        // Get input data from code content or prop
        val inputData = codeContent ?: extractInputFromProp(prop)
        
        if (inputData.isEmpty()) {
            return "Error: No input data provided. Please provide input data in a code block or after the process ID."
        }
        
        // Check if process exists
        val processInfo = processStateManager.getProcess(processId)
        if (processInfo == null) {
            return "Error: Process '$processId' not found."
        }
        
        // Check if process is running
        if (processInfo.status != ProcessStatus.RUNNING) {
            return "Error: Process '$processId' is not running (status: ${processInfo.status}). Cannot write input to terminated process."
        }
        
        // Write input to process
        val result = processStateManager.writeProcessInput(processId, inputData, appendNewline)
        
        return if (result.success) {
            val inputPreview = if (inputData.length > 50) {
                inputData.take(50) + "..."
            } else {
                inputData
            }
            "Successfully wrote input to process '$processId': \"$inputPreview\""
        } else {
            "Failed to write input to process '$processId': ${result.errorMessage}"
        }
    }
    
    private fun parseParameters(prop: String): Pair<String, Boolean> {
        val parts = prop.trim().split(" ")
        val processId = parts.firstOrNull()?.trim() ?: ""
        val appendNewline = !parts.any { it.trim() == "--no-newline" }
        
        return Pair(processId, appendNewline)
    }
    
    private fun extractInputFromProp(prop: String): String {
        // If there's content after the process ID and flags, use it as input
        val parts = prop.trim().split(" ")
        if (parts.size > 1) {
            // Skip process ID and flags, join the rest as input
            val inputParts = parts.drop(1).filter { !it.startsWith("--") }
            return inputParts.joinToString(" ")
        }
        return ""
    }
}
