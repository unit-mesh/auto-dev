package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier.Companion
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Card(
            modifier = Modifier.Companion.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        ) {
            Row(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Text(
                    text = "AI Code Review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Companion.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                when (state.aiProgress.stage) {
                    AnalysisStage.IDLE -> {
                        FilledTonalButton(
                            onClick = { viewModel.startAnalysis() },
                            enabled = state.diffFiles.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.PlayArrow,
                                contentDescription = "Start",
                                modifier = Modifier.Companion.size(18.dp)
                            )
                            Spacer(modifier = Modifier.Companion.width(4.dp))
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
                                modifier = Modifier.Companion.size(18.dp)
                            )
                            Spacer(modifier = Modifier.Companion.width(4.dp))
                            Text("Stop")
                        }
                    }

                    AnalysisStage.COMPLETED -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Companion.CenterVertically
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.CheckCircle,
                                contentDescription = "Completed",
                                tint = AutoDevColors.Green.c600,
                                modifier = Modifier.Companion.size(18.dp)
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
                            verticalAlignment = Alignment.Companion.CenterVertically
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Error,
                                contentDescription = "Error",
                                tint = AutoDevColors.Red.c600,
                                modifier = Modifier.Companion.size(18.dp)
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
                modifier = Modifier.Companion.fillMaxWidth(),
                color = AutoDevColors.Indigo.c600
            )
        }

        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (renderer.timeline.isEmpty() && state.aiProgress.stage == AnalysisStage.IDLE) {
                Column(
                    modifier = Modifier.Companion
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.Companion.size(48.dp)
                    )
                    Spacer(modifier = Modifier.Companion.height(16.dp))
                    Text(
                        text = "Click 'Start Review' to analyze the code changes with AI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.Companion.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

@Composable
fun ProgressOutputCard(
    title: String,
    content: String,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.Companion.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                AutoDevColors.Indigo.c600.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Companion.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.Companion.height(8.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Companion.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun FixResultCard(fix: FixResult) {
    Card(
        modifier = Modifier.Companion.fillMaxWidth(),
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
        Column(modifier = Modifier.Companion.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
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
                        modifier = Modifier.Companion.size(18.dp)
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
                        modifier = Modifier.Companion.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.Companion.height(8.dp))

            // Issue
            Text(
                text = "Issue: ${fix.lintIssue}",
                style = MaterialTheme.typography.bodySmall,
                color = AutoDevColors.Red.c600
            )

            Spacer(modifier = Modifier.Companion.height(4.dp))

            // AI fix description
            Text(
                text = "Fix: ${fix.aiFix}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Fixed code
            fix.fixedCode?.let { code ->
                Spacer(modifier = Modifier.Companion.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = code,
                        fontFamily = FontFamily.Companion.Monospace,
                        fontSize = 11.sp,
                        color = AutoDevColors.Green.c600,
                        modifier = Modifier.Companion.padding(8.dp)
                    )
                }
            }
        }
    }
}
