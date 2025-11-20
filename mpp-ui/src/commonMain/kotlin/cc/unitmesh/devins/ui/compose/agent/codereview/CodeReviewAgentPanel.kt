package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.linter.LintFileResult
import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * Right panel: AI code review messages
 */
@Composable
fun CodeReviewAgentPanel(
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
        // Header with Start/Stop button
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
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Companion.CenterVertically
                        ) {
                            Surface(
                                color = AutoDevColors.Green.c600,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.Companion.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = AutoDevComposeIcons.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Complete",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Companion.Bold
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { viewModel.startAnalysis() },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AutoDevColors.Indigo.c600,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = AutoDevComposeIcons.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Restart review",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    AnalysisStage.ERROR -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Companion.CenterVertically
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

        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (state.aiProgress.stage == AnalysisStage.IDLE && state.aiProgress.lintResults.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
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
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.aiProgress.lintResults.isNotEmpty() || state.aiProgress.lintOutput.isNotEmpty()) {
                        item {
                            CollapsibleLintAnalysisCard(
                                lintResults = state.aiProgress.lintResults,
                                lintOutput = state.aiProgress.lintOutput,
                                isActive = state.aiProgress.stage == AnalysisStage.RUNNING_LINT
                            )
                        }
                    }

                    if (state.aiProgress.analysisOutput.isNotEmpty()) {
                        item {
                            AIAnalysisSection(
                                analysisOutput = state.aiProgress.analysisOutput,
                                isActive = state.aiProgress.stage == AnalysisStage.ANALYZING_LINT
                            )
                        }
                    }

                    if (state.aiProgress.fixOutput.isNotEmpty()) {
                        item {
                            SuggestedFixesSection(
                                fixOutput = state.aiProgress.fixOutput,
                                isActive = state.aiProgress.stage == AnalysisStage.GENERATING_FIX,
                                onApplyFix = { diffPatch ->
                                    viewModel.applyDiffPatch(diffPatch)
                                },
                                onRejectFix = { diffPatch ->
                                    viewModel.rejectDiffPatch(diffPatch)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Collapsible Lint Analysis Card with per-file breakdown
 */
@Composable
fun CollapsibleLintAnalysisCard(
    lintResults: List<LintFileResult>,
    lintOutput: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    val totalErrors = lintResults.sumOf { it.errorCount }
    val totalWarnings = lintResults.sumOf { it.warningCount }
    val totalInfos = lintResults.sumOf { it.infoCount }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                AutoDevColors.Indigo.c600.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = "Lint Analysis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Companion.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isActive) {
                        Surface(
                            color = AutoDevColors.Indigo.c600,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "RUNNING",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Companion.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Issue counts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    if (totalErrors > 0) {
                        IssueBadge(
                            count = totalErrors,
                            color = AutoDevColors.Red.c600,
                            label = "E"
                        )
                    }
                    if (totalWarnings > 0) {
                        IssueBadge(
                            count = totalWarnings,
                            color = AutoDevColors.Amber.c600,
                            label = "W"
                        )
                    }
                    if (totalInfos > 0) {
                        IssueBadge(
                            count = totalInfos,
                            color = AutoDevColors.Blue.c600,
                            label = "I"
                        )
                    }
                }
            }

            // Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    // Show lint results per file
                    if (lintResults.isNotEmpty()) {
                        lintResults.forEach { fileResult ->
                            FileLintResultCard(fileResult = fileResult)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        // Fallback to raw output
                        Text(
                            text = lintOutput,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Companion.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual file lint result card (collapsible)
 */
@Composable
fun FileLintResultCard(
    fileResult: LintFileResult,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // File header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = fileResult.filePath.substringAfterLast("/"),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Companion.Monospace,
                        fontWeight = FontWeight.Companion.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    if (fileResult.errorCount > 0) {
                        IssueBadge(
                            count = fileResult.errorCount,
                            color = AutoDevColors.Red.c600,
                            label = "E"
                        )
                    }
                    if (fileResult.warningCount > 0) {
                        IssueBadge(
                            count = fileResult.warningCount,
                            color = AutoDevColors.Amber.c600,
                            label = "W"
                        )
                    }
                    if (fileResult.infoCount > 0) {
                        IssueBadge(
                            count = fileResult.infoCount,
                            color = AutoDevColors.Blue.c600,
                            label = "I"
                        )
                    }
                }
            }

            // Issues list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    fileResult.issues.forEach { issue ->
                        LintIssueRow(issue = issue)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Individual lint issue row
 */
@Composable
fun LintIssueRow(issue: LintIssue, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Severity icon
        val (severityColor, severityIcon) = when (issue.severity) {
            cc.unitmesh.agent.linter.LintSeverity.ERROR ->  AutoDevColors.Red.c600 to AutoDevComposeIcons.Error
            cc.unitmesh.agent.linter.LintSeverity.WARNING -> AutoDevColors.Amber.c600 to AutoDevComposeIcons.Warning
            cc.unitmesh.agent.linter.LintSeverity.INFO -> AutoDevColors.Blue.c600 to AutoDevComposeIcons.Info
        }

        Icon(
            imageVector = severityIcon,
            contentDescription = issue.severity.name,
            tint = severityColor,
            modifier = Modifier.size(14.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Line ${issue.line}:${issue.column}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Companion.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                issue.rule?.let { rule ->
                    Text(
                        text = "[$rule]",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Companion.Monospace,
                        color = AutoDevColors.Indigo.c600
                    )
                }
            }
            Text(
                text = issue.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            issue.suggestion?.let { suggestion ->
                Text(
                    text = "💡 $suggestion",
                    style = MaterialTheme.typography.bodySmall,
                    color = AutoDevColors.Green.c600,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Generic collapsible analysis card (for AI Analysis and Fixes)
 */
@Composable
fun CollapsibleAnalysisCard(
    title: String,
    content: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector = AutoDevComposeIcons.Article,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                AutoDevColors.Indigo.c600.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Companion.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isActive) {
                        Surface(
                            color = AutoDevColors.Indigo.c600,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "RUNNING",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Companion.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Companion.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 0.dp)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}

/**
 * Issue count badge
 */
@Composable
fun IssueBadge(
    count: Int,
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Companion.Bold,
                color = color,
                fontSize = 10.sp
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Companion.Bold,
                color = color,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Deprecated: Kept for backward compatibility
 */
@Composable
fun ProgressOutputCard(
    title: String,
    content: String,
    isActive: Boolean
) {
    CollapsibleAnalysisCard(
        title = title,
        content = content,
        isActive = isActive
    )
}

/**
 * Deprecated: Kept for backward compatibility
 */
@Composable
fun FixResultCard(fix: FixResult) {
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Companion.CenterVertically,
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
                        fontFamily = FontFamily.Companion.Monospace,
                        fontWeight = FontWeight.Companion.Bold,
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = fix.risk.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Companion.White,
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = code,
                        fontFamily = FontFamily.Companion.Monospace,
                        fontSize = 11.sp,
                        color = AutoDevColors.Green.c600,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
