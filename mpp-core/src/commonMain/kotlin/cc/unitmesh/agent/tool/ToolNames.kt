package cc.unitmesh.agent.tool

/**
 * Constants for tool names, similar to Gemini CLI's tool-names.ts
 */
object ToolNames {
    // File system tools
    const val READ_FILE = "read-file"
    const val WRITE_FILE = "write-file"
    const val LIST_FILES = "list-files"
    
    // Search tools
    const val GREP = "grep"
    const val GLOB = "glob"
    
    // Execution tools
    const val SHELL = "shell"
    const val EXEC = "exec"
    
    // Text processing tools
    const val EDIT_FILE = "edit-file"
    const val PATCH_FILE = "patch-file"
    
    // Information tools
    const val FILE_INFO = "file-info"
    const val DIR_INFO = "dir-info"
    
    // Utility tools
    const val HELP = "help"
    const val VERSION = "version"
    
    /**
     * All available tool names
     */
    val ALL_TOOLS = setOf(
        READ_FILE, WRITE_FILE, LIST_FILES,
        GREP, GLOB,
        SHELL, EXEC,
        EDIT_FILE, PATCH_FILE,
        FILE_INFO, DIR_INFO,
        HELP, VERSION
    )
    
    /**
     * Tools that require file system access
     */
    val FILE_SYSTEM_TOOLS = setOf(
        READ_FILE, WRITE_FILE, LIST_FILES,
        GREP, GLOB,
        EDIT_FILE, PATCH_FILE,
        FILE_INFO, DIR_INFO
    )
    
    /**
     * Tools that execute external commands
     */
    val EXECUTION_TOOLS = setOf(
        SHELL, EXEC
    )
    
    /**
     * Check if a tool name is valid
     */
    fun isValidToolName(name: String): Boolean {
        return name in ALL_TOOLS
    }
    
    /**
     * Check if a tool requires file system access
     */
    fun requiresFileSystem(toolName: String): Boolean {
        return toolName in FILE_SYSTEM_TOOLS
    }
    
    /**
     * Check if a tool executes external commands
     */
    fun isExecutionTool(toolName: String): Boolean {
        return toolName in EXECUTION_TOOLS
    }
}
