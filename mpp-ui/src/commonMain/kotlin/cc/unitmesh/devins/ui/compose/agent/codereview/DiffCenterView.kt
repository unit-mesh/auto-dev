package cc.unitmesh.devins.ui.compose.agent.codereview

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import kotlinx.datetime.Clock

/**
 * Left panel: Commit history list (GitHub-style)
 */
@Composable
fun CommitListView(
    commits: List<CommitInfo>,
    selectedIndex: Int,
    onCommitSelected: (Int) -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Text(
            text = "Commits (${commits.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Companion.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.Companion.padding(horizontal = 8.dp, vertical = 12.dp)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.Companion.height(4.dp))

        LazyColumn(modifier = Modifier.Companion.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

@Composable
fun CommitListItem(
    commit: CommitInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.Companion
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
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Commit message (first line)
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Text(
                    text = commit.message.lines().firstOrNull() ?: commit.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Companion.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Companion.Ellipsis,
                    modifier = Modifier.Companion.weight(1f)
                )

                // Extract PR/issue number if present (e.g., #453)
                val prNumber = Regex("#(\\d+)").find(commit.message)?.value
                if (prNumber != null) {
                    Surface(
                        color = AutoDevColors.Indigo.c600.copy(alpha = 0.15f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = prNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = AutoDevColors.Indigo.c600,
                            modifier = Modifier.Companion.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.Companion.height(6.dp))

            // Author and timestamp
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Text(
                    text = commit.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis
                )

                Text(
                    text = formatRelativeTime(commit.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Short hash
            Spacer(modifier = Modifier.Companion.height(4.dp))
            Text(
                text = commit.shortHash,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Companion.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
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

@Composable
fun DiffCenterView(
    diffFiles: List<DiffFileInfo>,
    selectedCommit: CommitInfo?,
    modifier: Modifier = Modifier.Companion
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
                modifier = Modifier.Companion.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = selectedCommit.message.lines().firstOrNull() ?: selectedCommit.message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Companion.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.Companion.height(4.dp))

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
                            fontFamily = FontFamily.Companion.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.Companion.height(8.dp))
        }

        Text(
            text = "Files changed (${diffFiles.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Companion.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.Companion.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        if (diffFiles.isEmpty()) {
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Companion.Center
            ) {
                Text(
                    text = "No file changes in this commit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.Companion.fillMaxSize(),
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

@Composable
fun CollapsibleFileDiffItem(file: DiffFileInfo) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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

                    Text(
                        text = file.path,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

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

                Icon(
                    imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

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

@Composable
fun DiffHunkView(hunk: DiffHunk) {
    Column(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
            .padding(4.dp)
    ) {
        Text(
            text = "@@ -${hunk.oldStart},${hunk.oldLines} +${hunk.newStart},${hunk.newLines} @@",
            fontFamily = FontFamily.Companion.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.Companion.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        hunk.lines.forEach { line ->
            DiffLineView(line)
        }
    }
}


@Composable
fun DiffLineView(line: DiffLine) {
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
            Color.Companion.Transparent,
            MaterialTheme.colorScheme.onSurfaceVariant,
            " "
        )
    }

    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = "${line.oldLineNumber ?: ""}",
            fontFamily = FontFamily.Companion.Monospace,
            fontSize = 10.sp,
            color = AutoDevColors.Diff.Dark.lineNumber,
            modifier = Modifier.Companion.width(32.dp),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.Companion.width(4.dp))

        // New line number
        Text(
            text = "${line.newLineNumber ?: ""}",
            fontFamily = FontFamily.Companion.Monospace,
            fontSize = 10.sp,
            color = AutoDevColors.Diff.Dark.lineNumber,
            modifier = Modifier.Companion.width(32.dp),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.Companion.width(8.dp))

        Text(
            text = prefix,
            fontFamily = FontFamily.Companion.Monospace,
            fontSize = 11.sp,
            color = textColor,
            modifier = Modifier.Companion.width(12.dp)
        )

        Text(
            text = line.content,
            fontFamily = FontFamily.Companion.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.Companion.weight(1f)
        )
    }
}
