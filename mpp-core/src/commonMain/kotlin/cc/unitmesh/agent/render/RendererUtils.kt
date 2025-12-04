package cc.unitmesh.agent.render

import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.toToolType

/**
 * Shared utility functions for Renderer implementations.
 * Used by both ComposeRenderer and JewelRenderer.
 */
object RendererUtils {

    /**
     * Format tool call for display in UI.
     */
    fun formatToolCallDisplay(toolName: String, paramsStr: String): ToolCallDisplayInfo {
        val params = parseParamsString(paramsStr)
        val toolType = toolName.toToolType()

        return when (toolType) {
            ToolType.ReadFile -> ToolCallDisplayInfo(
                toolName = toolType.displayName,
                description = "file reader",
                details = "Reading file: ${params["path"] ?: "unknown"}"
            )

            ToolType.WriteFile -> ToolCallDisplayInfo(
                toolName = toolType.displayName,
                description = "file writer",
                details = "Writing to file: ${params["path"] ?: "unknown"}"
            )

            ToolType.Glob -> ToolCallDisplayInfo(
                toolName = toolType.displayName,
                description = "pattern matcher",
                details = "Searching for files matching pattern: ${params["pattern"] ?: "*"}"
            )

            ToolType.Shell -> ToolCallDisplayInfo(
                toolName = toolType.displayName,
                description = "command executor",
                details = "Executing: ${params["command"] ?: params["cmd"] ?: "unknown command"}"
            )

            else -> ToolCallDisplayInfo(
                toolName = if (toolName == "docql") "DocQL" else toolName,
                description = "tool execution",
                details = paramsStr
            )
        }
    }

    /**
     * Format tool result summary for display.
     */
    fun formatToolResultSummary(toolName: String, success: Boolean, output: String?): String {
        if (!success) return "Failed"

        val toolType = toolName.toToolType()
        return when (toolType) {
            ToolType.ReadFile -> {
                val lines = output?.lines()?.size ?: 0
                "Read $lines lines"
            }

            ToolType.WriteFile -> "File written successfully"

            ToolType.Glob -> {
                val firstLine = output?.lines()?.firstOrNull() ?: ""
                when {
                    firstLine.contains("Found ") && firstLine.contains(" files matching") -> {
                        val count = firstLine.substringAfter("Found ").substringBefore(" files").toIntOrNull() ?: 0
                        "Found $count files"
                    }
                    output?.contains("No files found") == true -> "No files found"
                    else -> "Search completed"
                }
            }

            ToolType.Shell -> {
                val lines = output?.lines()?.size ?: 0
                if (lines > 0) "Executed ($lines lines output)" else "Executed successfully"
            }

            else -> "Success"
        }
    }

    /**
     * Parse parameter string into a map.
     * Handles both JSON format and key=value format.
     */
    fun parseParamsString(paramsStr: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val trimmed = paramsStr.trim()

        // Try JSON format first (starts with { and ends with })
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                // Simple JSON parsing for flat objects with string values
                val jsonContent = trimmed.substring(1, trimmed.length - 1)
                // Match "key": "value" or "key": number patterns
                val jsonRegex = Regex(""""(\w+)"\s*:\s*(?:"([^"\\]*(?:\\.[^"\\]*)*)"|(\d+(?:\.\d+)?)|(\w+))""")
                jsonRegex.findAll(jsonContent).forEach { match ->
                    val key = match.groups[1]?.value
                    // Value can be: quoted string, number, or unquoted word (like true/false)
                    val value = match.groups[2]?.value
                        ?: match.groups[3]?.value
                        ?: match.groups[4]?.value
                    if (key != null && value != null) {
                        params[key] = value
                    }
                }
                if (params.isNotEmpty()) {
                    return params
                }
            } catch (_: Exception) {
                // Fall through to key=value parsing
            }
        }

        // Fallback to key=value format
        val regex = Regex("""(\w+)="([^"]*)"|\s*(\w+)=([^\s]+)""")
        regex.findAll(paramsStr).forEach { match ->
            val key = match.groups[1]?.value ?: match.groups[3]?.value
            val value = match.groups[2]?.value ?: match.groups[4]?.value
            if (key != null && value != null) {
                params[key] = value
            }
        }
        return params
    }

    /**
     * Convert ToolCallDisplayInfo to ToolCallInfo.
     */
    fun toToolCallInfo(displayInfo: ToolCallDisplayInfo): ToolCallInfo {
        return ToolCallInfo(
            toolName = displayInfo.toolName,
            description = displayInfo.description,
            details = displayInfo.details
        )
    }
}

