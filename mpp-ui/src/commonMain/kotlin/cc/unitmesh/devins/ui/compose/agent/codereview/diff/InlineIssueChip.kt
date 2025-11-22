package cc.unitmesh.devins.ui.compose.agent.codereview.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
 * Clickable to open detailed dialog, with scrollable content support
 */
@Composable
fun IssueInfoCard(issueInfo: IssueInfo) {
    var showDetailDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .clickable { showDetailDialog = true },
        colors = CardDefaults.cardColors(
            containerColor = AutoDevColors.Indigo.c600.copy(alpha = 0.1f)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .heightIn(max = 150.dp) // Limit max height for long content
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
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

                    // Expand icon hint
                    Icon(
                        imageVector = AutoDevComposeIcons.Info,
                        contentDescription = "View details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.Companion.size(14.dp)
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

            // Labels (with horizontal scroll for many labels)
            if (issueInfo.labels.isNotEmpty()) {
                val horizontalScrollState = rememberScrollState()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    issueInfo.labels.take(5).forEach { label ->
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
                    if (issueInfo.labels.size > 5) {
                        Text(
                            text = "+${issueInfo.labels.size - 5}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // Show detail dialog when clicked
    if (showDetailDialog) {
        IssueDetailDialog(
            issueInfo = issueInfo,
            onDismiss = { showDetailDialog = false }
        )
    }
}

/**
 * Detailed dialog to display full issue information with scrolling support
 */
@Composable
private fun IssueDetailDialog(
    issueInfo: IssueInfo,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 500.dp, max = 800.dp)
                .heightIn(min = 400.dp, max = 700.dp)
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.BugReport,
                            contentDescription = null,
                            tint = AutoDevColors.Indigo.c600,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Issue #${issueInfo.id}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            // Status badge
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = when (issueInfo.status.lowercase()) {
                                    "open" -> AutoDevColors.Green.c600.copy(alpha = 0.2f)
                                    "closed" -> AutoDevColors.Red.c600.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = issueInfo.status.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (issueInfo.status.lowercase()) {
                                        "open" -> AutoDevColors.Green.c600
                                        "closed" -> AutoDevColors.Red.c600
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                HorizontalDivider()

                // Content with vertical scroll
                val verticalScrollState = rememberScrollState()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Title",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = issueInfo.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Description
                        if (issueInfo.description.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = issueInfo.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Metadata
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Metadata",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Author
                            issueInfo.author?.let { author ->
                                MetadataRow("Author", author)
                            }

                            // Assignees
                            if (issueInfo.assignees.isNotEmpty()) {
                                MetadataRow("Assignees", issueInfo.assignees.joinToString(", "))
                            }

                            // Created/Updated
                            issueInfo.createdAt?.let { created ->
                                MetadataRow("Created", created)
                            }
                            issueInfo.updatedAt?.let { updated ->
                                MetadataRow("Updated", updated)
                            }
                        }

                        // Labels
                        if (issueInfo.labels.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Labels (${issueInfo.labels.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // Wrap labels in a scrollable row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    issueInfo.labels.chunked(3).forEach { rowLabels ->
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            rowLabels.forEach { label ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper composable for metadata rows
 */
@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
