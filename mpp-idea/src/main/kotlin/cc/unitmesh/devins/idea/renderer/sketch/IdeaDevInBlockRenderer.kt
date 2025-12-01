package cc.unitmesh.devins.idea.renderer.sketch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.parser.ToolCallParser
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.devins.workspace.WorkspaceManager
import kotlinx.serialization.json.Json
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

/**
 * DevIn Block Renderer for IntelliJ IDEA with Jewel styling.
 *
 * Parses devin blocks (language id = "devin") and renders them as tool call items
 * when the block is complete. Similar to DevInBlockRenderer in mpp-ui but using
 * Jewel theming.
 */
@Composable
fun IdeaDevInBlockRenderer(
    devinContent: String,
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isComplete) {
            // Parse the devin block to extract tool calls
            val parser = remember { ToolCallParser() }
            val wrappedContent = "<devin>\n$devinContent\n</devin>"
            val toolCalls = remember(devinContent) { parser.parseToolCalls(wrappedContent) }

            if (toolCalls.isNotEmpty()) {
                // Get workspace root path for resolving relative paths
                val workspaceRoot = WorkspaceManager.currentWorkspace?.rootPath

                toolCalls.forEach { toolCall ->
                    val toolName = toolCall.toolName
                    val params = toolCall.params

                    // Format details string (for display)
                    val details = formatToolCallDetails(params)

                    // Resolve relative path to absolute path using workspace root
                    val relativePath = params["path"] as? String
                    val filePath = resolveAbsolutePath(relativePath, workspaceRoot)

                    // Map tool name to ToolType
                    val toolType = ToolType.fromName(toolName)

                    IdeaDevInToolItem(
                        toolName = toolName,
                        details = details,
                        filePath = filePath,
                        toolType = toolType,
                        params = params,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                // If no tool calls found, render as code block
                IdeaCodeBlockRenderer(
                    code = devinContent,
                    language = "devin",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // If not complete, show as code block (streaming)
            IdeaCodeBlockRenderer(
                code = devinContent,
                language = "devin",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Tool item display for DevIn block parsing results.
 * Shows tool name, type icon, and parameters in a compact expandable format.
 */
@Composable
private fun IdeaDevInToolItem(
    toolName: String,
    details: String,
    filePath: String?,
    toolType: ToolType?,
    params: Map<String, String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val hasParams = params.isNotEmpty()

    Box(
        modifier = modifier
            .background(
                color = JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        Column {
            // Header row: Tool icon + Tool name + Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { if (hasParams) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tool type icon
                Icon(
                    imageVector = IdeaComposeIcons.Build,
                    contentDescription = "Tool",
                    modifier = Modifier.size(16.dp),
                    tint = AutoDevColors.Blue.c400
                )

                // Tool name
                Text(
                    text = toolName,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )

                // Details (truncated parameters)
                if (details.isNotEmpty() && !expanded) {
                    Text(
                        text = details.take(60) + if (details.length > 60) "..." else "",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = JewelTheme.globalColors.text.info.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Expand/collapse icon
                if (hasParams) {
                    Icon(
                        imageVector = if (expanded) IdeaComposeIcons.ExpandLess else IdeaComposeIcons.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp),
                        tint = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f)
                    )
                }
            }

            // Expanded parameters section
            if (expanded && hasParams) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = formatParamsAsJson(params),
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
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
private fun formatToolCallDetails(params: Map<String, String>): String {
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

