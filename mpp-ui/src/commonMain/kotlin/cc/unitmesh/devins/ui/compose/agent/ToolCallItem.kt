package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.devins.ui.compose.agent.codereview.FileViewerDialog
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.terminal.PlatformTerminalDisplay

/**
 * Combined tool call and result display - shows both in a single compact row
 * Similar to TerminalOutputItem but for general tools (ReadFile, WriteFile, Glob, etc.)
 */
@Composable
fun CombinedToolItem(
    toolName: String,
    details: String?,
    fullParams: String? = null,
    filePath: String? = null,
    toolType: ToolType? = null,
    success: Boolean? = null, // null means still executing
    summary: String? = null,
    output: String? = null,
    fullOutput: String? = null,
    executionTimeMs: Long? = null,
    onOpenFileViewer: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(success == false) } // Auto-expand on error
    var showFullParams by remember { mutableStateOf(false) }
    var showFullOutput by remember { mutableStateOf(success == false) }
    var showFileViewerDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    // Determine which params/output to display
    val displayParams = if (showFullParams) fullParams else details
    val hasFullParams = fullParams != null && fullParams != details
    val displayOutput = if (showFullOutput) fullOutput else output
    val hasFullOutput = fullOutput != null && fullOutput != output

    // Check if this is a file operation that can be viewed
    val isFileOperation =
        toolType in
            listOf(
                ToolType.ReadFile,
                ToolType.WriteFile
            )

    val isExecuting = success == null

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { if (displayParams != null || displayOutput != null) expanded = !expanded },
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        isExecuting -> AutoDevComposeIcons.PlayArrow
                        success -> AutoDevComposeIcons.CheckCircle
                        else -> AutoDevComposeIcons.Error
                    },
                    contentDescription = when {
                        isExecuting -> "Executing"
                        success -> "Success"
                        else -> "Failed"
                    },
                    tint = when {
                        isExecuting -> MaterialTheme.colorScheme.primary
                        success -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.Companion.size(16.dp)
                )

                // Tool name
                Text(
                    text = toolName,
                    fontWeight = FontWeight.Companion.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.Companion.weight(1f)
                )

                if (summary != null) {
                    Text(
                        text = "→ $summary",
                        color = when (success) {
                            true -> Color(0xFF4CAF50)
                            false -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Companion.Medium
                    )
                }

                if (executionTimeMs != null && executionTimeMs > 0) {
                    Text(
                        text = "${executionTimeMs}ms",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (isFileOperation && !filePath.isNullOrEmpty()) {
                    IconButton(
                        onClick = {
                            if (onOpenFileViewer != null) {
                                onOpenFileViewer(filePath)
                            } else {
                                showFileViewerDialog = true
                            }
                        },
                        modifier = Modifier.Companion.size(24.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Visibility,
                            contentDescription = "View File",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.Companion.size(18.dp)
                        )
                    }
                }

                if (displayParams != null || displayOutput != null) {
                    Icon(
                        imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.Companion.size(20.dp)
                    )
                }
            }

            if (expanded) {
                if (displayParams != null) {
                    Spacer(modifier = Modifier.Companion.height(8.dp))

                    Row(
                        modifier = Modifier.Companion.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Companion.Top
                    ) {
                        Column {
                            Text(
                                text = "Parameters:",
                                fontWeight = FontWeight.Companion.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (hasFullParams) {
                                TextButton(
                                    onClick = { showFullParams = !showFullParams },
                                    modifier = Modifier.Companion.height(32.dp)
                                ) {
                                    Text(
                                        text = if (showFullParams) "Show Formatted" else "Show Raw Params",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(displayParams)) },
                            modifier = Modifier.Companion.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy parameters",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.Companion.size(16.dp)
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.Companion.fillMaxWidth()
                    ) {
                        Text(
                            text = if (showFullParams) (displayParams) else formatToolParameters(displayParams),
                            modifier = Modifier.Companion.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Companion.Monospace
                        )
                    }
                }

                if (displayOutput != null) {
                    Spacer(modifier = Modifier.Companion.height(8.dp))

                    Row(
                        modifier = Modifier.Companion.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Companion.Top
                    ) {
                        Column {
                            Text(
                                text = "Output:",
                                fontWeight = FontWeight.Companion.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (hasFullOutput) {
                                TextButton(
                                    onClick = { showFullOutput = !showFullOutput },
                                    modifier = Modifier.Companion.height(32.dp)
                                ) {
                                    Text(
                                        text = if (showFullOutput) "Show Less" else "Show Full Output",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(displayOutput)) },
                            modifier = Modifier.Companion.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy output",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.Companion.size(16.dp)
                            )
                        }
                    }

                    // Render DocQL output as Markdown, other tools as plain text
                    if (toolName.lowercase().contains("docql")) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.Companion.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.Companion
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                cc.unitmesh.devins.ui.compose.sketch.MarkdownSketchRenderer.RenderMarkdown(
                                    markdown = displayOutput,
                                    isComplete = true,
                                    isDarkTheme = false, // Will be determined by system theme
                                    modifier = Modifier.Companion.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.Companion.fillMaxWidth()
                        ) {
                            Text(
                                text = formatOutput(displayOutput),
                                modifier = Modifier.Companion.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Companion.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    // Show file viewer dialog when onOpenFileViewer is null
    if (showFileViewerDialog && !filePath.isNullOrEmpty()) {
        FileViewerDialog(
            filePath = filePath,
            onClose = { showFileViewerDialog = false }
        )
    }
}

@Composable
fun ToolCallItem(
    toolName: String,
    description: String,
    details: String?,
    fullParams: String? = null,
    filePath: String? = null,
    toolType: ToolType? = null,
    onOpenFileViewer: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showFullParams by remember { mutableStateOf(false) }
    var showFileViewerDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val displayParams = if (showFullParams) fullParams else details
    val hasFullParams = fullParams != null && fullParams != details

    val isFileOperation =
        toolType in
            listOf(
                ToolType.ReadFile,
                ToolType.WriteFile
            )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
    ) {
        Column(modifier = Modifier.Companion.padding(8.dp)) {
            Row(
                modifier = Modifier.Companion.fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { if (displayParams != null) expanded = !expanded },
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = AutoDevComposeIcons.PlayArrow,
                    contentDescription = "Tool Call",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.Companion.size(16.dp)
                )
                Text(
                    text = toolName,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.Companion.weight(1f)
                )

                if (isFileOperation && !filePath.isNullOrEmpty()) {
                    IconButton(
                        onClick = {
                            if (onOpenFileViewer != null) {
                                onOpenFileViewer(filePath)
                            } else {
                                showFileViewerDialog = true
                            }
                        },
                        modifier = Modifier.Companion.size(24.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Visibility,
                            contentDescription = "View File",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.Companion.size(18.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.Companion.size(20.dp)
                )
            }

            if (expanded && displayParams != null) {
                Spacer(modifier = Modifier.Companion.height(8.dp))

                Row(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Companion.Top
                ) {
                    Column {
                        Text(
                            text = "Parameters:",
                            fontWeight = FontWeight.Companion.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Show toggle button if there are full params different from formatted details
                        if (hasFullParams) {
                            TextButton(
                                onClick = { showFullParams = !showFullParams },
                                modifier = Modifier.Companion.height(32.dp)
                            ) {
                                Text(
                                    text = if (showFullParams) "Show Formatted" else "Show Raw Params",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row {
                        // Copy parameters button
                        IconButton(
                            onClick = {
                                clipboardManager.setText(
                                    androidx.compose.ui.text.AnnotatedString(
                                        displayParams ?: ""
                                    )
                                )
                            },
                            modifier = Modifier.Companion.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy parameters",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.Companion.size(16.dp)
                            )
                        }

                        // Copy entire block button (always copy full params if available)
                        IconButton(
                            onClick = {
                                val blockText =
                                    buildString {
                                        appendLine("[Tool Call]: $toolName")
                                        appendLine("Description: $description")
                                        appendLine("Parameters: ${fullParams ?: details ?: ""}")
                                    }
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(blockText))
                            },
                            modifier = Modifier.Companion.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.ContentCopy,
                                contentDescription = "Copy entire block",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.Companion.size(16.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.Companion.fillMaxWidth()
                ) {
                    Text(
                        text = if (showFullParams) (displayParams ?: "") else formatToolParameters(displayParams ?: ""),
                        modifier = Modifier.Companion.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Companion.Monospace
                    )
                }
            }
        }
    }

    // Show file viewer dialog when onOpenFileViewer is null
    if (showFileViewerDialog && !filePath.isNullOrEmpty()) {
        FileViewerDialog(
            filePath = filePath,
            onClose = { showFileViewerDialog = false }
        )
    }
}

fun formatToolParameters(params: String): String {
    return try {
        val lines = mutableListOf<String>()
        val regex = Regex("""(\w+)=["']?([^"']*?)["']?(?:\s|$)""")
        regex.findAll(params).forEach { match ->
            val key = match.groups[1]?.value ?: ""
            val value = match.groups[2]?.value ?: ""
            lines.add("$key: $value")
        }

        if (lines.isNotEmpty()) {
            lines.joinToString("\n")
        } else {
            params // Fallback to original if parsing fails
        }
    } catch (e: Exception) {
        params // Fallback to original on any error
    }
}

fun formatOutput(output: String): String {
    return when {
        output.trim().startsWith("{") || output.trim().startsWith("[") -> {
            try {
                output.replace(",", ",\n")
                    .replace("{", "{\n  ")
                    .replace("}", "\n}")
                    .replace("[", "[\n  ")
                    .replace("]", "\n]")
            } catch (e: Exception) {
                output
            }
        }

        output.contains("│") -> output
        output.contains("\n") -> output
        output.length > 100 -> "${output.take(100)}..."
        else -> output
    }
}

@Composable
fun TerminalOutputItem(
    command: String,
    output: String,
    exitCode: Int,
    executionTimeMs: Long
) {
    var expanded by remember { mutableStateOf(exitCode != 0) } // Auto-expand on error
    val clipboardManager = LocalClipboardManager.current
    val isSuccess = exitCode == 0

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
    ) {
        Column(modifier = Modifier.Companion.padding(8.dp)) {
            Row(
                modifier =
                    Modifier.Companion
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { expanded = !expanded },
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.Companion.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isSuccess) AutoDevComposeIcons.CheckCircle else AutoDevComposeIcons.Error,
                        contentDescription = if (isSuccess) "Success" else "Error",
                        tint = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.Companion.size(16.dp)
                    )
                    Text(
                        text = if (isSuccess) "Exit 0" else "Exit $exitCode",
                        color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Companion.Medium
                    )
                }
                Text(
                    text = "${executionTimeMs}ms",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall
                )
                Icon(
                    imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.Companion.size(20.dp)
                )
            }

            if (expanded && output.isNotEmpty()) {
                PlatformTerminalDisplay(
                    output = output,
                    modifier = Modifier.Companion.padding(8.dp)
                )
            }
        }
    }
}
