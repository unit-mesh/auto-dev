package cc.unitmesh.agent.tool

import cc.unitmesh.agent.subagent.CodebaseInvestigatorSchema
import cc.unitmesh.agent.subagent.ContentHandlerSchema
import cc.unitmesh.agent.subagent.ErrorRecoverySchema
import cc.unitmesh.agent.tool.impl.*
import cc.unitmesh.agent.tool.impl.AskSubAgentSchema
import cc.unitmesh.agent.tool.schema.ToolSchema

/**
 * Sealed class representing all available tool types with their metadata
 *
 * @deprecated This class is deprecated in favor of self-describing tools via ExecutableTool.metadata.
 * Tools now carry their own metadata, eliminating the need for this parallel type system.
 * 
 * This class is kept for backward compatibility only. New code should use:
 * - ExecutableTool.metadata for tool information
 * - ToolProvider for tool discovery
 * - ToolRegistry.getAllTools() for accessing tools
 * 
 * Migration path:
 * ```
 * // Old way (deprecated):
 * val toolType = ToolType.ReadFile
 * val name = toolType.name
 * val displayName = toolType.displayName
 * 
 * // New way:
 * val tool = toolRegistry.getTool("read-file")
 * val name = tool.name
 * val displayName = tool.metadata.displayName
 * ```
 */
@Deprecated(
    message = "Use ExecutableTool.metadata instead. Tools are now self-describing.",
    replaceWith = ReplaceWith("ExecutableTool.metadata"),
    level = DeprecationLevel.WARNING
)
sealed class ToolType(
    val name: String,
    val displayName: String,
    val tuiEmoji: String,
    val composeIcon: String,
    val category: ToolCategory,
    val schema: ToolSchema
) {
    data object ReadFile : ToolType(
        name = "read-file",
        displayName = "Read File",
        tuiEmoji = "📄",
        composeIcon = "file_open",
        category = ToolCategory.FileSystem,
        schema = ReadFileSchema
    )

    data object WriteFile : ToolType(
        name = "write-file",
        displayName = "Write File",
        tuiEmoji = "✏️",
        composeIcon = "edit",
        category = ToolCategory.FileSystem,
        schema = WriteFileSchema
    )

    data object EditFile : ToolType(
        name = "edit-file",
        displayName = "Edit File",
        tuiEmoji = "🔧",
        composeIcon = "edit_note",
        category = ToolCategory.FileSystem,
        schema = EditFileSchema
    )

    data object ListFiles : ToolType(
        name = "list-files",
        displayName = "List Files",
        tuiEmoji = "📁",
        composeIcon = "folder",
        category = ToolCategory.FileSystem,
        schema = GlobSchema // ListFiles uses similar schema to Glob
    )

    data object Grep : ToolType(
        name = "grep",
        displayName = "Search Content",
        tuiEmoji = "🔍",
        composeIcon = "search",
        category = ToolCategory.Search,
        schema = GrepSchema
    )

    data object Glob : ToolType(
        name = "glob",
        displayName = "Find Files",
        tuiEmoji = "🌐",
        composeIcon = "find_in_page",
        category = ToolCategory.Search,
        schema = GlobSchema
    )

    data object Shell : ToolType(
        name = "shell",
        displayName = "Shell Command",
        tuiEmoji = "💻",
        composeIcon = "terminal",
        category = ToolCategory.Execution,
        schema = ShellSchema
    )

    data object ErrorAgent : ToolType(
        name = "error-agent",
        displayName = "Error Agent",
        tuiEmoji = "🚑",
        composeIcon = "healing",
        category = ToolCategory.SubAgent,
        schema = ErrorRecoverySchema
    )

    data object AnalysisAgent : ToolType(
        name = "analysis-agent",
        displayName = "Analysis Agent",
        tuiEmoji = "📊",
        composeIcon = "analytics",
        category = ToolCategory.SubAgent,
        schema = ContentHandlerSchema
    )

    data object CodeAgent : ToolType(
        name = "code-agent",
        displayName = "Code Agent",
        tuiEmoji = "🔬",
        composeIcon = "science",
        category = ToolCategory.SubAgent,
        schema = CodebaseInvestigatorSchema
    )

    data object AskAgent : ToolType(
        name = "ask-agent",
        displayName = "Ask Agent",
        tuiEmoji = "💬",
        composeIcon = "chat",
        category = ToolCategory.Communication,
        schema = AskSubAgentSchema
    )

    data object WebFetch : ToolType(
        name = "web-fetch",
        displayName = "Web Fetch",
        tuiEmoji = "🌐",
        composeIcon = "language",
        category = ToolCategory.Utility,
        schema = WebFetchSchema
    )

    companion object {
        /**
         * All available tool types
         */
        val ALL_TOOLS by lazy {
            listOf(
                ReadFile, WriteFile, EditFile, Grep, Glob,
                Shell,
                ErrorAgent, AnalysisAgent, CodeAgent,
                AskAgent,
                WebFetch
            )
        }

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

        val ALL_TOOL_NAMES by lazy { ALL_TOOLS.map { it.name }.toSet() }


        fun isValidToolName(name: String): Boolean = name in ALL_TOOL_NAMES

        fun requiresFileSystem(toolType: ToolType): Boolean {
            return toolType.category == ToolCategory.FileSystem || toolType.category == ToolCategory.Search
        }

        fun isExecutionTool(toolType: ToolType): Boolean {
            return toolType.category == ToolCategory.Execution
        }
    }
}

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
