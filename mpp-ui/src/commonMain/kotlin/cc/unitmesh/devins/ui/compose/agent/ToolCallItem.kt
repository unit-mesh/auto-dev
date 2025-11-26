package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import cc.unitmesh.agent.tool.impl.DocQLSearchStats
import cc.unitmesh.devins.ui.compose.agent.codereview.FileViewerDialog
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.terminal.PlatformTerminalDisplay

/**
 * Combined tool call and result display - shows both in a single compact row
 * Similar to TerminalOutputItem but for general tools (ReadFile, WriteFile, Glob, etc.)
 */
@OptIn(ExperimentalLayoutApi::class)
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
    docqlStats: DocQLSearchStats? = null,
    onOpenFileViewer: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(success == false) } // Auto-expand on error
    var showFullParams by remember { mutableStateOf(false) }
    var showFullOutput by remember { mutableStateOf(success == false) }
    var showFileViewerDialog by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
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
    val hasStats = docqlStats != null

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

                // Show stats button for DocQL
                if (hasStats) {
                    IconButton(
                        onClick = { showStats = !showStats },
                        modifier = Modifier.Companion.size(24.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Analytics,
                            contentDescription = if (showStats) "Hide Stats" else "Show Stats",
                            tint = if (showStats) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.Companion.size(18.dp)
                        )
                    }
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
                            cc.unitmesh.devins.ui.compose.sketch.MarkdownSketchRenderer.RenderMarkdown(
                                markdown = displayOutput,
                                isComplete = true,
                                isDarkTheme = false, // Will be determined by system theme
                                modifier = Modifier.Companion.padding(8.dp)
                            )
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

            // DocQL Search Statistics Section
            if (showStats && docqlStats != null) {
                Spacer(modifier = Modifier.Companion.height(8.dp))
                DocQLStatsSection(stats = docqlStats)
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
        val trimmed = params.trim()
        
        // Check if it's JSON format
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            // Parse JSON-like format
            val lines = mutableListOf<String>()
            
            // Simple JSON parsing for common cases
            val jsonPattern = Regex(""""(\w+)"\s*:\s*("([^"]*)"|(\d+)|true|false|null)""")
            jsonPattern.findAll(trimmed).forEach { match ->
                val key = match.groups[1]?.value ?: ""
                val value = match.groups[3]?.value 
                    ?: match.groups[4]?.value 
                    ?: match.groups[2]?.value?.removeSurrounding("\"") 
                    ?: ""
                lines.add("$key: $value")
            }
            
            if (lines.isNotEmpty()) {
                return lines.joinToString("\n")
            }
        }
        
        // Try key=value format
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

/**
 * Displays DocQL search statistics with detailed technical information
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DocQLStatsSection(stats: DocQLSearchStats) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = AutoDevComposeIcons.Analytics,
                    contentDescription = "Search Statistics",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Search Statistics",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            // Search type badge
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatBadge(
                    label = "Type",
                    value = when (stats.searchType) {
                        DocQLSearchStats.SearchType.SMART_SEARCH -> "Smart Search"
                        DocQLSearchStats.SearchType.DIRECT_QUERY -> "Direct Query"
                        DocQLSearchStats.SearchType.FALLBACK_CONTENT -> "Fallback Search"
                        DocQLSearchStats.SearchType.LLM_RERANKED -> "LLM Reranked"
                    },
                    color = when (stats.searchType) {
                        DocQLSearchStats.SearchType.SMART_SEARCH -> Color(0xFF2196F3)
                        DocQLSearchStats.SearchType.DIRECT_QUERY -> Color(0xFF4CAF50)
                        DocQLSearchStats.SearchType.FALLBACK_CONTENT -> Color(0xFFFF9800)
                        DocQLSearchStats.SearchType.LLM_RERANKED -> Color(0xFF9C27B0)  // Purple for LLM
                    }
                )
                
                if (stats.usedFallback) {
                    StatBadge(
                        label = "Mode",
                        value = "Fallback",
                        color = Color(0xFFFF9800)
                    )
                }
            }

            // Channels used
            if (stats.channels.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Channels:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stats.channels.forEach { channel ->
                            ChannelChip(channel)
                        }
                    }
                }
            }

            // Result counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatItem(
                    label = "Docs Searched",
                    value = stats.documentsSearched.toString()
                )
                StatItem(
                    label = "Raw Results",
                    value = stats.totalRawResults.toString()
                )
                StatItem(
                    label = "After Rerank",
                    value = stats.resultsAfterRerank.toString(),
                    highlight = true
                )
                if (stats.truncated) {
                    Text(
                        text = "(truncated)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            // Reranker configuration
            stats.rerankerConfig?.let { config ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Reranker: ${config.rerankerType}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ConfigItem("RRF-k", config.rrfK.toString())
                        ConfigItem("RRF Weight", "${(config.rrfWeight * 100).toInt()}%")
                        ConfigItem("Content Weight", "${(config.contentWeight * 100).toInt()}%")
                        ConfigItem("Min Score", formatDouble(config.minScoreThreshold, 1))
                    }
                }
            }

            // Scoring information
            stats.scoringInfo?.let { scoring ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Scorers:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                        scoring.scorerComponents.forEach { scorer ->
                            ScorerChip(scorer)
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ScoreItem("Avg", scoring.avgScore)
                        ScoreItem("Max", scoring.maxScore)
                        ScoreItem("Min", scoring.minScore)
                    }
                }
            }
            
            // LLM Reranker Information - show cost/performance metrics
            stats.llmRerankerInfo?.let { llm ->
                Surface(
                    color = if (llm.success) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Header with warning badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Analytics,
                                contentDescription = "LLM Reranking",
                                tint = if (llm.success) Color(0xFF9C27B0) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "LLM Reranking",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (llm.success) Color(0xFF9C27B0) else MaterialTheme.colorScheme.error
                            )
                            
                            // Cost warning badge
                            Surface(
                                color = Color(0xFFFF9800).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "Uses AI Tokens",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                            
                            if (llm.usedFallback) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Fallback",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        
                        // Performance metrics
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ConfigItem("Items", "${llm.itemsProcessed} → ${llm.itemsReranked}")
                            if (llm.tokensUsed > 0) {
                                ConfigItem("Tokens", "~${llm.tokensUsed}")
                            }
                            if (llm.latencyMs > 0) {
                                ConfigItem("Latency", "${llm.latencyMs}ms")
                            }
                        }
                        
                        // Explanation if available
                        llm.explanation?.let { explanation ->
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                        
                        // Error message if failed
                        llm.error?.let { error ->
                            Text(
                                text = "Error: $error",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun ChannelChip(channel: String) {
    val (icon, color) = when (channel) {
        "class" -> AutoDevComposeIcons.Code to Color(0xFF9C27B0)
        "function" -> AutoDevComposeIcons.Code to Color(0xFF673AB7)
        "heading" -> AutoDevComposeIcons.Description to Color(0xFF3F51B5)
        "toc" -> AutoDevComposeIcons.List to Color(0xFF2196F3)
        "content_chunks" -> AutoDevComposeIcons.Description to Color(0xFF00BCD4)
        else -> AutoDevComposeIcons.Search to Color(0xFF607D8B)
    }
    
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = channel,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = channel,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ConfigItem(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun ScorerChip(scorer: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(3.dp)
    ) {
        Text(
            text = scorer,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ScoreItem(label: String, score: Double) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
        )
        Text(
            text = formatDouble(score, 2),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Format a double value to specified decimal places (multiplatform compatible)
 */
private fun formatDouble(value: Double, decimals: Int = 2): String {
    val factor = when (decimals) {
        0 -> 1.0
        1 -> 10.0
        2 -> 100.0
        3 -> 1000.0
        else -> generateSequence(1.0) { it * 10 }.take(decimals + 1).last()
    }
    val rounded = kotlin.math.round(value * factor) / factor
    val str = rounded.toString()
    val dotIndex = str.indexOf('.')
    return if (dotIndex == -1) {
        "$str.${"0".repeat(decimals)}"
    } else {
        val currentDecimals = str.length - dotIndex - 1
        if (currentDecimals >= decimals) {
            str.substring(0, dotIndex + decimals + 1)
        } else {
            str + "0".repeat(decimals - currentDecimals)
        }
    }
}
