package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.scoring.ScoringBreakdown
import cc.unitmesh.agent.tool.impl.docql.DocQLSearchStats
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

/**
 * Displays DocQL search statistics with detailed technical information
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocQLStatsSection(stats: DocQLSearchStats) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.Companion.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.Companion.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = AutoDevComposeIcons.Analytics,
                    contentDescription = "Search Statistics",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.Companion.size(16.dp)
                )
                Text(
                    text = "Search Statistics",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Companion.Bold,
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
                    verticalAlignment = Alignment.Companion.Top,
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
                modifier = Modifier.Companion.fillMaxWidth(),
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
                        fontWeight = FontWeight.Companion.Medium,
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
                        verticalAlignment = Alignment.Companion.CenterVertically,
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier.Companion.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Header with warning badge
                        Row(
                            verticalAlignment = Alignment.Companion.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Analytics,
                                contentDescription = "LLM Reranking",
                                tint = if (llm.success) Color(0xFF9C27B0) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.Companion.size(14.dp)
                            )
                            Text(
                                text = "LLM Reranking",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Companion.Bold,
                                color = if (llm.success) Color(0xFF9C27B0) else MaterialTheme.colorScheme.error
                            )

                            // Cost warning badge
                            Surface(
                                color = Color(0xFFFF9800).copy(alpha = 0.15f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.Companion.padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.Companion.CenterVertically
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
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Fallback",
                                        modifier = Modifier.Companion.padding(horizontal = 4.dp, vertical = 2.dp),
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
                                fontStyle = FontStyle.Italic
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.Companion.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Companion.Bold,
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.Companion.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = channel,
                tint = color,
                modifier = Modifier.Companion.size(12.dp)
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
    Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Companion.Bold,
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
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Companion.Medium,
            fontFamily = FontFamily.Companion.Monospace,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun ScorerChip(scorer: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
    ) {
        Text(
            text = scorer,
            modifier = Modifier.Companion.padding(horizontal = 5.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Companion.Monospace,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ScoreItem(label: String, score: Double) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
        )
        Text(
            text = formatDouble(score, 2),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Companion.Medium,
            fontFamily = FontFamily.Companion.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatDouble(value: Double, decimals: Int = 2): String {
    return ScoringBreakdown.Companion.formatDouble(value, decimals)
}
