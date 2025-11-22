package cc.unitmesh.devins.ui.compose.agent.codereview.diff

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.agent.codereview.CommitInfo
import cc.unitmesh.devins.ui.compose.agent.codereview.formatDate
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import kotlinx.datetime.Clock

/**
 * Left panel: Commit history list (GitHub-style) with infinite scroll and Git Graph
 */
@Composable
fun CommitListView(
    commits: List<CommitInfo>,
    selectedIndices: Set<Int>,
    onCommitSelected: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier.Companion,
    hasMoreCommits: Boolean = false,
    isLoadingMore: Boolean = false,
    totalCommitCount: Int? = null,
    onLoadMore: () -> Unit = {},
    showGraph: Boolean = true
) {
    // Build graph structure from commit messages
//    val graphStructure = if (showGraph && commits.isNotEmpty()) {
//        GitGraphBuilder.buildGraph(commits.map { it.message })
//    } else {
//        GitGraphStructure(emptyMap(), emptyList(), 0)
//    }

    var isMultiSelectMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.Companion.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            val displayText = when {
                totalCommitCount != null -> "Commits (${commits.size}/$totalCommitCount)"
                hasMoreCommits -> "Commits (${commits.size}+)"
                else -> "Commits (${commits.size})"
            }

            Text(
                text = displayText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Companion.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Multi-select toggle
            androidx.compose.material3.IconButton(
                onClick = { isMultiSelectMode = !isMultiSelectMode },
                modifier = Modifier.Companion.size(24.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = if (isMultiSelectMode) AutoDevComposeIcons.CheckBox else AutoDevComposeIcons.CheckBoxOutlineBlank,
                    contentDescription = "Toggle Multi-select",
                    tint = if (isMultiSelectMode) AutoDevColors.Indigo.c600 else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.Companion.size(16.dp)
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.Companion.height(4.dp))

        LazyColumn(modifier = Modifier.Companion.fillMaxSize(),) {
            items(commits.size) { index ->
                CommitListItem(
                    commit = commits[index],
                    isSelected = index in selectedIndices,
                    isMultiSelectMode = isMultiSelectMode,
                    onClick = { isMultiSelect -> onCommitSelected(index, isMultiSelect) },
//                    graphNode = if (showGraph) graphStructure.nodes[index] else null,
//                    graphStructure = graphStructure
                )

                if (index == commits.size - 5 && hasMoreCommits && !isLoadingMore) {
                    LaunchedEffect(Unit) {
                        onLoadMore()
                    }
                }
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.Companion.size(24.dp),
                            color = AutoDevColors.Indigo.c600
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommitListItem(
    commit: CommitInfo,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: (Boolean) -> Unit,
//    graphNode: GitGraphNode? = null,
//    graphStructure: GitGraphStructure = GitGraphStructure(emptyMap(), emptyList(), 0)
) {
    Card(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .clickable {
                // If in multi-select mode, clicking the card toggles selection.
                // Otherwise, it's a single select.
                onClick(isMultiSelectMode)
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                AutoDevColors.Indigo.c600.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier.Companion.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
//            if (graphNode != null && graphStructure.maxColumns > 0) {
//                GitGraphColumn(
//                    node = graphNode,
//                    graphStructure = graphStructure,
//                    rowHeight = 58.dp,
//                    columnWidth = 16.dp,
//                    modifier = Modifier.Companion.padding(start = 4.dp)
//                )
//            }

            // Multi-select checkbox (only visible in multi-select mode)
            if (isMultiSelectMode) {
                androidx.compose.material3.Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick(true) }, // Toggle mode
                    modifier = Modifier.Companion.padding(start = 4.dp, top = 12.dp).size(20.dp)
                )
            }

            Column(
                modifier = Modifier.Companion
                    .weight(1f)
                    .padding(6.dp)
            ) {
            // Commit message (first line)
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.Top
            ) {
                Text(
                    text = commit.message.lines().firstOrNull() ?: commit.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Companion.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis,
                    modifier = Modifier.Companion.weight(1f)
                )

                // Extract PR/issue number if present (e.g., #453)
                val prNumber = Regex("#(\\d+)").find(commit.message)?.value
                if (prNumber != null) {
                    Surface(
                        color = AutoDevColors.Indigo.c600.copy(alpha = 0.15f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = prNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = AutoDevColors.Indigo.c600,
                            modifier = Modifier.Companion.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.Companion.height(3.dp))

            // Author, hash and timestamp in one compact row
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                // Author and hash together
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    modifier = Modifier.Companion.weight(1f, fill = false)
                ) {
                    Text(
                        text = commit.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Companion.Ellipsis,
                        modifier = Modifier.Companion.weight(1f, fill = false)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = commit.shortHash,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Companion.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Text(
                    text = formatRelativeTime(commit.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
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
