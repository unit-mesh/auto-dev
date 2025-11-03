package cc.unitmesh.agent.tool

/**
 * Sealed class representing all available tool types with their metadata
 * 
 * This replaces the string-based tool names with a type-safe approach that includes:
 * - Tool name (for backward compatibility)
 * - Display name (human-readable)
 * - TUI emoji (for terminal rendering)
 * - Compose icon (for UI rendering)
 * - Category (for grouping)
 */
sealed class ToolType(
    val name: String,
    val displayName: String,
    val tuiEmoji: String,
    val composeIcon: String,
    val category: ToolCategory
) {
    
    // File System Tools
    data object ReadFile : ToolType(
        name = "read-file",
        displayName = "Read File",
        tuiEmoji = "📄",
        composeIcon = "file_open",
        category = ToolCategory.FileSystem
    )
    
    data object WriteFile : ToolType(
        name = "write-file",
        displayName = "Write File",
        tuiEmoji = "✏️",
        composeIcon = "edit",
        category = ToolCategory.FileSystem
    )
    
    data object ListFiles : ToolType(
        name = "list-files",
        displayName = "List Files",
        tuiEmoji = "📁",
        composeIcon = "folder",
        category = ToolCategory.FileSystem
    )
    
    data object EditFile : ToolType(
        name = "edit-file",
        displayName = "Edit File",
        tuiEmoji = "📝",
        composeIcon = "edit_note",
        category = ToolCategory.FileSystem
    )

    // Search Tools
    data object Grep : ToolType(
        name = "grep",
        displayName = "Search Content",
        tuiEmoji = "🔍",
        composeIcon = "search",
        category = ToolCategory.Search
    )
    
    data object Glob : ToolType(
        name = "glob",
        displayName = "Find Files",
        tuiEmoji = "🌐",
        composeIcon = "find_in_page",
        category = ToolCategory.Search
    )

    data object PatchFile : ToolType(
        name = "patch-file",
        displayName = "Patch File",
        tuiEmoji = "🔧",
        composeIcon = "build",
        category = ToolCategory.FileSystem
    )

    data object Shell : ToolType(
        name = "shell",
        displayName = "Shell Command",
        tuiEmoji = "💻",
        composeIcon = "terminal",
        category = ToolCategory.Execution
    )

    data object ErrorRecovery : ToolType(
        name = "error-recovery",
        displayName = "Error Recovery",
        tuiEmoji = "🚑",
        composeIcon = "healing",
        category = ToolCategory.SubAgent
    )
    
    data object LogSummary : ToolType(
        name = "log-summary",
        displayName = "Log Summary",
        tuiEmoji = "📋",
        composeIcon = "summarize",
        category = ToolCategory.SubAgent
    )
    
    data object CodebaseInvestigator : ToolType(
        name = "codebase-investigator",
        displayName = "Codebase Investigator",
        tuiEmoji = "🔬",
        composeIcon = "science",
        category = ToolCategory.SubAgent
    )
    
    companion object {
        /**
         * All available tool types
         */
        val ALL_TOOLS = listOf(
            ReadFile, WriteFile, ListFiles, EditFile, PatchFile,
            Grep, Glob,
            Shell,
            ErrorRecovery, LogSummary, CodebaseInvestigator
        )
        
        /**
         * Get tool type by name (for backward compatibility)
         */
        fun fromName(name: String): ToolType? {
            return ALL_TOOLS.find { it.name == name }
        }
        
        /**
         * Get all tools by category
         */
        fun byCategory(category: ToolCategory): List<ToolType> {
            return ALL_TOOLS.filter { it.category == category }
        }
        
        /**
         * Get all tool names (for backward compatibility)
         */
        val ALL_TOOL_NAMES = ALL_TOOLS.map { it.name }.toSet()

        /**
         * Tools that require file system access
         */
        val FILE_SYSTEM_TOOLS = byCategory(ToolCategory.FileSystem)

        /**
         * Tools that execute external commands
         */
        val EXECUTION_TOOLS = byCategory(ToolCategory.Execution)

        /**
         * SubAgent tools for specialized tasks
         */
        val SUBAGENT_TOOLS = byCategory(ToolCategory.SubAgent)

        /**
         * Check if a tool name is valid
         */
        fun isValidToolName(name: String): Boolean {
            return name in ALL_TOOL_NAMES
        }

        /**
         * Check if a tool requires file system access
         */
        fun requiresFileSystem(toolType: ToolType): Boolean {
            return toolType.category == ToolCategory.FileSystem || toolType.category == ToolCategory.Search
        }

        /**
         * Check if a tool executes external commands
         */
        fun isExecutionTool(toolType: ToolType): Boolean {
            return toolType.category == ToolCategory.Execution
        }
    }
}

/**
 * Extension functions for convenient access
 */

/**
 * Convert string tool name to ToolType (for migration)
 */
fun String.toToolType(): ToolType? = ToolType.fromName(this)

/**
 * Check if string tool name is valid
 */
fun String.isValidToolName(): Boolean = ToolType.isValidToolName(this)

/**
 * Get tool category by string name
 */
fun String.getToolCategory(): ToolCategory? = ToolType.fromName(this)?.category
