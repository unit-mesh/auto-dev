package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

@Composable
fun AIAnalysisSection(
    analysisOutput: String,
    reviewFindings: List<cc.unitmesh.agent.ReviewFinding>,
    isActive: Boolean,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) AutoDevComposeIcons.ExpandMore else AutoDevComposeIcons.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Icon(
                        imageVector = AutoDevComposeIcons.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = "Issues analysis (AI)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isActive) {
                        Surface(
                            color = AutoDevColors.Indigo.c600,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ANALYZING",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Finding count badges
                if (reviewFindings.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val criticalCount = reviewFindings.count { it.severity == cc.unitmesh.agent.Severity.CRITICAL }
                        val highCount = reviewFindings.count { it.severity == cc.unitmesh.agent.Severity.HIGH }
                        val mediumCount = reviewFindings.count { it.severity == cc.unitmesh.agent.Severity.MEDIUM }

                        if (criticalCount > 0) {
                            Surface(
                                color = AutoDevColors.Red.c600.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "$criticalCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AutoDevColors.Red.c600,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (highCount > 0) {
                            Surface(
                                color = AutoDevColors.Amber.c600.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "$highCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AutoDevColors.Amber.c600,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (mediumCount > 0) {
                            Surface(
                                color = AutoDevColors.Blue.c600.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "$mediumCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AutoDevColors.Blue.c600,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 0.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show analysis output first if available
                    if (analysisOutput.isNotBlank()) {
                        Text(
                            text = analysisOutput,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Show structured findings
                    if (reviewFindings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        reviewFindings.forEach { finding ->
                            ReviewFindingCard(finding)
                        }
                    } else if (analysisOutput.isBlank()) {
                        Text(
                            text = "No analysis results yet...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewFindingCard(finding: cc.unitmesh.agent.ReviewFinding) {
    val severityColor = when (finding.severity) {
        cc.unitmesh.agent.Severity.CRITICAL -> AutoDevColors.Red.c600
        cc.unitmesh.agent.Severity.HIGH -> AutoDevColors.Amber.c600
        cc.unitmesh.agent.Severity.MEDIUM -> AutoDevColors.Blue.c600
        cc.unitmesh.agent.Severity.LOW -> AutoDevColors.Green.c600
        cc.unitmesh.agent.Severity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val severityIcon = when (finding.severity) {
        cc.unitmesh.agent.Severity.CRITICAL, cc.unitmesh.agent.Severity.HIGH -> AutoDevComposeIcons.Error
        cc.unitmesh.agent.Severity.MEDIUM -> AutoDevComposeIcons.Warning
        else -> AutoDevComposeIcons.Info
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = severityIcon,
                contentDescription = finding.severity.name,
                tint = severityColor,
                modifier = Modifier.size(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = severityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = finding.severity.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = severityColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = finding.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    finding.filePath?.let { path ->
                        Text(
                            text = "• $path${finding.lineNumber?.let { ":$it" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = finding.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                finding.suggestion?.let { suggestion ->
                    Surface(
                        color = AutoDevColors.Green.c600.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Info,
                                contentDescription = "Suggestion",
                                tint = AutoDevColors.Green.c600,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

