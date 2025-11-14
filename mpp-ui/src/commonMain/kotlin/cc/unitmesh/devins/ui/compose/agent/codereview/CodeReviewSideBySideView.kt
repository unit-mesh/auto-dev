package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import cc.unitmesh.devins.ui.compose.agent.ResizableSplitPane
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

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
                    // Select commit (will trigger diff loading in subclasses like JvmCodeReviewViewModel)
                    viewModel.selectCommit(index)
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
