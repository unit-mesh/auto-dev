package cc.unitmesh.devins.ui.compose.sketch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.parser.ToolCallParser
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.devins.ui.compose.agent.CombinedToolItem
import cc.unitmesh.devins.workspace.WorkspaceManager
import kotlinx.serialization.json.Json

/**
 * DevIn Block Renderer - renders devin blocks as tool calls
 * 
 * Parses devin blocks (language id = "devin") and renders them as CombinedToolItem
 * when the block is complete.
 */
@Composable
fun DevInBlockRenderer(
    devinContent: String,
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isComplete) {
            // Parse the devin block to extract tool calls
            val parser = ToolCallParser()
            val wrappedContent = "<devin>\n$devinContent\n</devin>"
            val toolCalls = parser.parseToolCalls(wrappedContent)

            if (toolCalls.isNotEmpty()) {
                // Get workspace root path for resolving relative paths
                val workspaceRoot = WorkspaceManager.currentWorkspace?.rootPath
                
                toolCalls.forEach { toolCall ->
                    // Extract details for rendering
                    val toolName = toolCall.toolName
                    val params = toolCall.params
                    
                    // Format details string (for display)
                    val details = formatToolCallDetails(toolName, params)
                    
                    // Format full params as JSON (for expansion)
                    val fullParams = formatParamsAsJson(params)
                    
                    // Extract file path if available (for ReadFile/WriteFile)
                    // Resolve relative path to absolute path using workspace root
                    val relativePath = params["path"] as? String
                    val filePath = resolveAbsolutePath(relativePath, workspaceRoot)
                    
                    // Map tool name to ToolType
                    val toolType = ToolType.fromName(toolName)
                    
                    CombinedToolItem(
                        toolName = toolName,
                        details = details,
                        fullParams = fullParams,
                        filePath = filePath,
                        toolType = toolType,
                        success = null, // null means still executing or waiting
                        summary = null,
                        output = null,
                        fullOutput = null,
                        executionTimeMs = null
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                // If no tool calls found, render as code block
                CodeBlockRenderer(
                    code = devinContent,
                    language = "devin",
                    displayName = "DevIn"
                )
            }
        } else {
            // If not complete, show as code block (streaming)
            CodeBlockRenderer(
                code = devinContent,
                language = "devin",
                displayName = "DevIn"
            )
        }
    }
}

/**
 * Resolve relative path to absolute path using workspace root
 */
private fun resolveAbsolutePath(relativePath: String?, workspaceRoot: String?): String? {
    if (relativePath == null) return null
    if (workspaceRoot == null) return relativePath
    
    // If already an absolute path, return as-is
    if (relativePath.startsWith("/") || relativePath.matches(Regex("^[A-Za-z]:.*"))) {
        return relativePath
    }
    
    // Combine workspace root with relative path
    val separator = if (workspaceRoot.endsWith("/") || workspaceRoot.endsWith("\\")) "" else "/"
    return "$workspaceRoot$separator$relativePath"
}

/**
 * Format tool call parameters as a human-readable details string
 */
private fun formatToolCallDetails(toolName: String, params: Map<String, String>): String {
    return params.entries.joinToString(", ") { (key, value) ->
        "$key=${truncateValue(value)}"
    }
}

/**
 * Truncate long values for display
 */
private fun truncateValue(value: String, maxLength: Int = 100): String {
    return if (value.length > maxLength) {
        value.take(maxLength) + "..."
    } else {
        value
    }
}

/**
 * Format parameters as JSON string for full display
 */
private fun formatParamsAsJson(params: Map<String, String>): String {
    return try {
        Json {
            prettyPrint = true
        }.encodeToString(
            kotlinx.serialization.serializer(),
            params
        )
    } catch (e: Exception) {
        // Fallback to manual formatting
        buildString {
            appendLine("{")
            params.entries.forEachIndexed { index, (key, value) ->
                append("  \"$key\": ")
                append("\"${value.replace("\"", "\\\"")}\"")
                if (index < params.size - 1) {
                    appendLine(",")
                } else {
                    appendLine()
                }
            }
            append("}")
        }
    }
}

