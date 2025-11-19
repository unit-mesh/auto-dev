package cc.unitmesh.agent.tool.schema

/**
 * Categories for organizing tools
 */
enum class ToolCategory(
    val displayName: String,
    val description: String,
    val tuiEmoji: String,
    val composeIcon: String
) {
    FileSystem(
        displayName = "File System",
        description = "Tools for file and directory operations",
        tuiEmoji = "📁",
        composeIcon = "folder"
    ),

    Search(
        displayName = "Search",
        description = "Tools for searching files and content",
        tuiEmoji = "🔍",
        composeIcon = "search"
    ),

    Execution(
        displayName = "Execution",
        description = "Tools for executing commands and scripts",
        tuiEmoji = "⚡",
        composeIcon = "play_arrow"
    ),

    Information(
        displayName = "Information",
        description = "Tools for gathering system and file information",
        tuiEmoji = "ℹ️",
        composeIcon = "info"
    ),

    Utility(
        displayName = "Utility",
        description = "General utility tools",
        tuiEmoji = "🛠️",
        composeIcon = "build"
    ),

    SubAgent(
        displayName = "Sub-Agent",
        description = "Specialized sub-agents for complex tasks",
        tuiEmoji = "🤖",
        composeIcon = "smart_toy"
    ),

    Communication(
        displayName = "Communication",
        description = "Tools for inter-agent communication",
        tuiEmoji = "💬",
        composeIcon = "chat"
    );

    companion object {
        /**
         * Get category by name
         */
        fun fromName(name: String): ToolCategory? {
            return values().find { it.name.equals(name, ignoreCase = true) }
        }
    }
}