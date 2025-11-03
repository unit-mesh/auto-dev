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
    const val WRITE_FILE = "write-file"

    // Search tools
    const val GREP = "grep"
    const val GLOB = "glob"

    // Execution tools
    const val SHELL = "shell"

    // SubAgent tools
    const val ERROR_RECOVERY = "error-recovery"
    const val CODEBASE_INVESTIGATOR = "codebase-investigator"
}
