package cc.unitmesh.agent.tool

/**
 * Constants for tool names - DEPRECATED
 *
 * Use ToolType sealed class instead for type-safe tool handling.
 * This object is kept for backward compatibility.
 *
 * @deprecated Use ToolType sealed class instead
 */
@Deprecated("Use ToolType sealed class instead", ReplaceWith("ToolType"))
object ToolNames {
    // File system tools
    const val READ_FILE = "read-file"
    const val WRITE_FILE = "write-file"
    const val LIST_FILES = "list-files"
    const val EDIT_FILE = "edit-file"
    const val PATCH_FILE = "patch-file"

    // Search tools
    const val GREP = "grep"
    const val GLOB = "glob"

    // Execution tools
    const val SHELL = "shell"
    const val EXEC = "exec"

    // Information tools
    const val FILE_INFO = "file-info"
    const val DIR_INFO = "dir-info"

    // Utility tools
    const val HELP = "help"
    const val VERSION = "version"

    // SubAgent tools
    const val ERROR_RECOVERY = "error-recovery"
    const val LOG_SUMMARY = "log-summary"
    const val CODEBASE_INVESTIGATOR = "codebase-investigator"
    
    /**
     * All available tool names - DEPRECATED
     * Use ToolType.ALL_TOOL_NAMES instead
     */
    @Deprecated("Use ToolType.ALL_TOOL_NAMES instead")
    val ALL_TOOLS = ToolType.ALL_TOOL_NAMES
    
    /**
     * Tools that require file system access - DEPRECATED
     * Use ToolType.FILE_SYSTEM_TOOLS instead
     */
    @Deprecated("Use ToolType.FILE_SYSTEM_TOOLS instead")
    val FILE_SYSTEM_TOOLS = ToolType.FILE_SYSTEM_TOOLS.map { it.name }.toSet()

    /**
     * Tools that execute external commands - DEPRECATED
     * Use ToolType.EXECUTION_TOOLS instead
     */
    @Deprecated("Use ToolType.EXECUTION_TOOLS instead")
    val EXECUTION_TOOLS = ToolType.EXECUTION_TOOLS.map { it.name }.toSet()

    /**
     * SubAgent tools for specialized tasks - DEPRECATED
     * Use ToolType.SUBAGENT_TOOLS instead
     */
    @Deprecated("Use ToolType.SUBAGENT_TOOLS instead")
    val SUBAGENT_TOOLS = ToolType.SUBAGENT_TOOLS.map { it.name }.toSet()
    
    /**
     * Check if a tool name is valid - DEPRECATED
     * Use ToolType.isValidToolName instead
     */
    @Deprecated("Use ToolType.isValidToolName instead")
    fun isValidToolName(name: String): Boolean {
        return ToolType.isValidToolName(name)
    }

    /**
     * Check if a tool requires file system access - DEPRECATED
     * Use ToolType.requiresFileSystem instead
     */
    @Deprecated("Use ToolType.requiresFileSystem instead")
    fun requiresFileSystem(toolName: String): Boolean {
        return toolName.toToolType()?.let { ToolType.requiresFileSystem(it) } ?: false
    }

    /**
     * Check if a tool executes external commands - DEPRECATED
     * Use ToolType.isExecutionTool instead
     */
    @Deprecated("Use ToolType.isExecutionTool instead")
    fun isExecutionTool(toolName: String): Boolean {
        return toolName.toToolType()?.let { ToolType.isExecutionTool(it) } ?: false
    }
}
