package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer
import cc.unitmesh.devins.ui.compose.agent.AgentMessageList
import cc.unitmesh.devins.ui.compose.agent.ResizableSplitPane
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.sketch.DiffSketchRenderer
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

// Expect function for platform-specific date formatting
expect fun formatDate(timestamp: Long): String

/**
 * Main Side-by-Side Code Review UI (redesigned)
 *
 * Three-column layout using ResizableSplitPane:
 * - Left: Commit history list (like GitHub commits view)
 * - Center: Diff viewer with collapsible file changes (using DiffSketchRenderer)
 * - Right: AI code review messages (using AgentMessageList)
 */
@Composable
fun CodeReviewSideBySideView(
    viewModel: CodeReviewViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                LoadingView()
            }
            state.error != null -> {
                ErrorView(
                    error = state.error!!,
                    onRetry = { viewModel.refresh() }
                )
            }
            state.commitHistory.isEmpty() -> {
                EmptyCommitView(
                    onLoadDiff = { viewModel.refresh() }
                )
            }
            else -> {
                ThreeColumnLayout(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }
}

/**
 * Three-column layout with ResizableSplitPane
 */
@Composable
private fun ThreeColumnLayout(
    state: CodeReviewState,
    viewModel: CodeReviewViewModel
) {
    // Create a mock renderer for the right panel (AI messages)
    // In a real implementation, this would be passed from the viewModel
    val renderer = remember { ComposeRenderer() }

    ResizableSplitPane(
        modifier = Modifier.fillMaxSize(),
        initialSplitRatio = 0.25f,
        minRatio = 0.15f,
        maxRatio = 0.4f,
        first = {
            // Left: Commit history list
            CommitListView(
                commits = state.commitHistory,
                selectedIndex = state.selectedCommitIndex,
                onCommitSelected = { index ->
                    // Load diff for selected commit
                    // Call refresh which will load the diff for the commit
                    // Note: For cross-platform support, we use reflection or expect/actual pattern
                    try {
                        // Try to call loadDiffForCommit if it exists (JVM implementation)
                        val method = viewModel::class.members.find { it.name == "loadDiffForCommit" }
                        if (method != null) {
                            method.call(viewModel, index)
                        }
                    } catch (e: Exception) {
                        // Fallback: just update selection
                        viewModel.selectFile(index)
                    }
                }
            )
        },
        second = {
            // Center + Right: Diff view and AI messages
            ResizableSplitPane(
                modifier = Modifier.fillMaxSize(),
                initialSplitRatio = 0.55f,
                minRatio = 0.3f,
                maxRatio = 0.8f,
                first = {
                    // Center: Diff viewer
                    DiffCenterView(
                        diffFiles = state.diffFiles,
                        selectedCommit = state.commitHistory.getOrNull(state.selectedCommitIndex)
                    )
                },
                second = {
                    // Right: AI code review messages
                    AIReviewPanel(
                        state = state,
                        viewModel = viewModel,
                        renderer = renderer
                    )
                }
            )
        }
    )
}

/**
 * Left panel: Commit history list (GitHub-style)
 */
@Composable
private fun CommitListView(
    commits: List<CommitInfo>,
    selectedIndex: Int,
    onCommitSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        // Header
        Text(
            text = "Commits (${commits.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Commit list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(commits.size) { index ->
                CommitListItem(
                    commit = commits[index],
                    isSelected = index == selectedIndex,
                    onClick = { onCommitSelected(index) }
                )
            }
        }
    }
}

/**
 * Single commit list item (GitHub-style)
 */
@Composable
private fun CommitListItem(
    commit: CommitInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                AutoDevColors.Indigo.c600.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Commit message (first line)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = commit.message.lines().firstOrNull() ?: commit.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Extract PR/issue number if present (e.g., #453)
                val prNumber = Regex("#(\\d+)").find(commit.message)?.value
                if (prNumber != null) {
                    Surface(
                        color = AutoDevColors.Indigo.c600.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = prNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = AutoDevColors.Indigo.c600,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Author and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = commit.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatRelativeTime(commit.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Short hash
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = commit.shortHash,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Format relative time (e.g., "2 minutes ago", "Today 18:03")
 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> {
            val hours = diff / 3600_000
            if (hours < 12) "$hours hours ago" else "Today ${formatDate(timestamp).split(" ").lastOrNull() ?: ""}"
        }
        diff < 172800_000 -> "Yesterday"
        else -> formatDate(timestamp)
    }
}

/**
 * Center panel: Diff viewer with collapsible file changes
 */
@Composable
private fun DiffCenterView(
    diffFiles: List<DiffFileInfo>,
    selectedCommit: CommitInfo?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        // Header with commit info
        if (selectedCommit != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = selectedCommit.message.lines().firstOrNull() ?: selectedCommit.message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = selectedCommit.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedCommit.shortHash,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Files changed header
        Text(
            text = "Files changed (${diffFiles.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        if (diffFiles.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No file changes in this commit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // File list with collapsible diffs
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(diffFiles.size) { index ->
                    CollapsibleFileDiffItem(
                        file = diffFiles[index]
                    )
                }
            }
        }
    }
}

/**
 * Collapsible file diff item using DiffSketchRenderer
 */
@Composable
private fun CollapsibleFileDiffItem(
    file: DiffFileInfo
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // File header (clickable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Change type icon
                    Icon(
                        imageVector = when (file.changeType) {
                            ChangeType.ADDED -> AutoDevComposeIcons.Add
                            ChangeType.DELETED -> AutoDevComposeIcons.Delete
                            ChangeType.MODIFIED -> AutoDevComposeIcons.Edit
                            ChangeType.RENAMED -> AutoDevComposeIcons.DriveFileRenameOutline
                        },
                        contentDescription = file.changeType.name,
                        tint = when (file.changeType) {
                            ChangeType.ADDED -> AutoDevColors.Green.c600
                            ChangeType.DELETED -> AutoDevColors.Red.c600
                            ChangeType.MODIFIED -> AutoDevColors.Blue.c600
                            ChangeType.RENAMED -> AutoDevColors.Amber.c600
                        },
                        modifier = Modifier.size(18.dp)
                    )

                    // File path
                    Text(
                        text = file.path,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Language badge
                    file.language?.let { lang ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = lang,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Expand/collapse icon
                Icon(
                    imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expandable diff content using DiffSketchRenderer-style rendering
            if (expanded && file.hunks.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                Column(modifier = Modifier.padding(8.dp)) {
                    file.hunks.forEach { hunk ->
                        DiffHunkView(hunk)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

/**
 * Right panel: AI code review messages
 */
@Composable
private fun AIReviewPanel(
    state: CodeReviewState,
    viewModel: CodeReviewViewModel,
    renderer: ComposeRenderer,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header with action buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Code Review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Start/Stop analysis button
                when (state.aiProgress.stage) {
                    AnalysisStage.IDLE -> {
                        FilledTonalButton(
                            onClick = { viewModel.startAnalysis() },
                            enabled = state.diffFiles.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.PlayArrow,
                                contentDescription = "Start",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Review")
                        }
                    }
                    AnalysisStage.RUNNING_LINT,
                    AnalysisStage.ANALYZING_LINT,
                    AnalysisStage.GENERATING_FIX -> {
                        FilledTonalButton(
                            onClick = { viewModel.cancelAnalysis() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = AutoDevColors.Red.c600.copy(alpha = 0.2f),
                                contentColor = AutoDevColors.Red.c600
                            )
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Stop,
                                contentDescription = "Stop",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                    AnalysisStage.COMPLETED -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.CheckCircle,
                                contentDescription = "Completed",
                                tint = AutoDevColors.Green.c600,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AutoDevColors.Green.c600
                            )
                        }
                    }
                    AnalysisStage.ERROR -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Error,
                                contentDescription = "Error",
                                tint = AutoDevColors.Red.c600,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AutoDevColors.Red.c600
                            )
                        }
                    }
                }
            }
        }

        // Progress indicator
        if (state.aiProgress.stage in listOf(
                AnalysisStage.RUNNING_LINT,
                AnalysisStage.ANALYZING_LINT,
                AnalysisStage.GENERATING_FIX
            )
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = AutoDevColors.Indigo.c600
            )
        }

        // Message list (reusing AgentMessageList component)
        // For now, show a placeholder until we integrate with the actual renderer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (renderer.timeline.isEmpty() && state.aiProgress.stage == AnalysisStage.IDLE) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Click 'Start Review' to analyze the code changes with AI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Show agent messages or progress outputs
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show progress outputs
                    if (state.aiProgress.lintOutput.isNotEmpty()) {
                        item {
                            ProgressOutputCard(
                                title = "Lint Analysis",
                                content = state.aiProgress.lintOutput,
                                isActive = state.aiProgress.stage == AnalysisStage.RUNNING_LINT
                            )
                        }
                    }

                    if (state.aiProgress.analysisOutput.isNotEmpty()) {
                        item {
                            ProgressOutputCard(
                                title = "AI Analysis",
                                content = state.aiProgress.analysisOutput,
                                isActive = state.aiProgress.stage == AnalysisStage.ANALYZING_LINT
                            )
                        }
                    }

                    if (state.fixResults.isNotEmpty()) {
                        items(state.fixResults.size) { index ->
                            FixResultCard(state.fixResults[index])
                        }
                    }
                }
            }
        }
    }
}

/**
 * Progress output card
 */
@Composable
private fun ProgressOutputCard(
    title: String,
    content: String,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                AutoDevColors.Indigo.c600.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Diff hunk view (simplified from DiffSketchRenderer)
 */
@Composable
private fun DiffHunkView(hunk: DiffHunk) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(4.dp)
            )
            .padding(4.dp)
    ) {
        // Hunk header
        Text(
            text = "@@ -${hunk.oldStart},${hunk.oldLines} +${hunk.newStart},${hunk.newLines} @@",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        // Lines
        hunk.lines.forEach { line ->
            DiffLineView(line)
        }
    }
}

/**
 * Diff line view (using design system colors)
 */
@Composable
private fun DiffLineView(line: DiffLine) {
    val (backgroundColor, textColor, prefix) = when (line.type) {
        DiffLineType.ADDED -> Triple(
            AutoDevColors.Diff.Dark.addedBg,
            AutoDevColors.Green.c400,
            "+"
        )
        DiffLineType.DELETED -> Triple(
            AutoDevColors.Diff.Dark.deletedBg,
            AutoDevColors.Red.c400,
            "-"
        )
        DiffLineType.CONTEXT -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurfaceVariant,
            " "
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        // Old line number
        Text(
            text = "${line.oldLineNumber ?: ""}",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = AutoDevColors.Diff.Dark.lineNumber,
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // New line number
        Text(
            text = "${line.newLineNumber ?: ""}",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = AutoDevColors.Diff.Dark.lineNumber,
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Prefix (+/-)
        Text(
            text = prefix,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = textColor,
            modifier = Modifier.width(12.dp)
        )

        // Content
        Text(
            text = line.content,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Fix result card for displaying AI-generated fixes
 */

@Composable
private fun FixResultCard(fix: FixResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (fix.status) {
                FixStatus.FIXED -> AutoDevColors.Green.c600.copy(alpha = 0.1f)
                FixStatus.NO_ISSUE -> AutoDevColors.Blue.c600.copy(alpha = 0.1f)
                FixStatus.SKIPPED -> AutoDevColors.Amber.c600.copy(alpha = 0.1f)
                FixStatus.FAILED -> AutoDevColors.Red.c600.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status icon
                    Icon(
                        imageVector = when (fix.status) {
                            FixStatus.FIXED -> AutoDevComposeIcons.CheckCircle
                            FixStatus.NO_ISSUE -> AutoDevComposeIcons.Info
                            FixStatus.SKIPPED -> AutoDevComposeIcons.Warning
                            FixStatus.FAILED -> AutoDevComposeIcons.Error
                        },
                        contentDescription = fix.status.name,
                        tint = when (fix.status) {
                            FixStatus.FIXED -> AutoDevColors.Green.c600
                            FixStatus.NO_ISSUE -> AutoDevColors.Blue.c600
                            FixStatus.SKIPPED -> AutoDevColors.Amber.c600
                            FixStatus.FAILED -> AutoDevColors.Red.c600
                        },
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = "${fix.filePath}:${fix.line}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Risk badge
                Surface(
                    color = when (fix.risk) {
                        RiskLevel.CRITICAL -> AutoDevColors.Red.c600
                        RiskLevel.HIGH -> AutoDevColors.Amber.c600
                        RiskLevel.MEDIUM -> AutoDevColors.Amber.c500
                        RiskLevel.LOW -> AutoDevColors.Green.c600
                        RiskLevel.INFO -> AutoDevColors.Blue.c600
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = fix.risk.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Issue
            Text(
                text = "Issue: ${fix.lintIssue}",
                style = MaterialTheme.typography.bodySmall,
                color = AutoDevColors.Red.c600
            )

            Spacer(modifier = Modifier.height(4.dp))

            // AI fix description
            Text(
                text = "Fix: ${fix.aiFix}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Fixed code
            fix.fixedCode?.let { code ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = AutoDevColors.Green.c600,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

// ===============================================================================
// Supporting Views
// ===============================================================================

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = AutoDevColors.Indigo.c600
            )
            Text(
                text = "Loading commit history...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Error,
                contentDescription = "Error",
                tint = AutoDevColors.Red.c600,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            FilledTonalButton(onClick = onRetry) {
                Icon(
                    imageVector = AutoDevComposeIcons.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyCommitView(onLoadDiff: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No commits available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Make sure you have commits in your repository",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            FilledTonalButton(onClick = onLoadDiff) {
                Icon(
                    imageVector = AutoDevComposeIcons.Refresh,
                    contentDescription = "Load",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load Commits")
            }
        }
    }
}
