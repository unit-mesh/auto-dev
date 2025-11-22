package cc.unitmesh.devins.ui.compose.agent.codereview.diff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.tracker.IssueInfo
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors

/**
 * Inline compact issue chip (shown next to commit message)
 */
@Composable
fun InlineIssueChip(issueInfo: IssueInfo) {
    Surface(
        color = when (issueInfo.status.lowercase()) {
            "open" -> AutoDevColors.Green.c600.copy(alpha = 0.15f)
            "closed" -> AutoDevColors.Neutral.c600.copy(alpha = 0.15f)
            else -> AutoDevColors.Indigo.c600.copy(alpha = 0.15f)
        },
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.Companion.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Icon(
                imageVector = when (issueInfo.status.lowercase()) {
                    "open" -> AutoDevComposeIcons.BugReport
                    "closed" -> AutoDevComposeIcons.CheckCircle
                    else -> AutoDevComposeIcons.Info
                },
                contentDescription = issueInfo.status,
                tint = when (issueInfo.status.lowercase()) {
                    "open" -> AutoDevColors.Green.c600
                    "closed" -> AutoDevColors.Neutral.c600
                    else -> AutoDevColors.Indigo.c600
                },
                modifier = Modifier.Companion.size(14.dp)
            )
            Text(
                text = "#${issueInfo.id}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Companion.Bold,
                color = when (issueInfo.status.lowercase()) {
                    "open" -> AutoDevColors.Green.c600
                    "closed" -> AutoDevColors.Neutral.c600
                    else -> AutoDevColors.Indigo.c600
                }
            )
        }
    }
}

/**
 * Card to display issue information from issue tracker
 */
@Composable
fun IssueInfoCard(issueInfo: IssueInfo) {
    Card(
        modifier = Modifier.Companion.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AutoDevColors.Indigo.c600.copy(alpha = 0.1f)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.BugReport,
                        contentDescription = "Issue",
                        tint = AutoDevColors.Indigo.c600,
                        modifier = Modifier.Companion.size(16.dp)
                    )
                    Text(
                        text = "#${issueInfo.id}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Companion.Bold,
                        color = AutoDevColors.Indigo.c600
                    )
                }

                // Status badge
                Surface(
                    color = when (issueInfo.status.lowercase()) {
                        "open" -> AutoDevColors.Green.c600.copy(alpha = 0.2f)
                        "closed" -> AutoDevColors.Red.c600.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = issueInfo.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (issueInfo.status.lowercase()) {
                            "open" -> AutoDevColors.Green.c600
                            "closed" -> AutoDevColors.Red.c600
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.Companion.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = issueInfo.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Companion.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Companion.Ellipsis
            )

            if (issueInfo.description.isNotBlank()) {
                Text(
                    text = issueInfo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Companion.Ellipsis
                )
            }

            // Labels
            if (issueInfo.labels.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.Companion.fillMaxWidth()
                ) {
                    issueInfo.labels.take(3).forEach { label ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.Companion.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (issueInfo.labels.size > 3) {
                        Text(
                            text = "+${issueInfo.labels.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
