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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.tool.tracking.ChangeType
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.devins.ui.compose.sketch.DiffHunk
import cc.unitmesh.devins.ui.compose.sketch.DiffLine
import cc.unitmesh.devins.ui.compose.sketch.DiffLineType
import kotlinx.datetime.Clock
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import cc.unitmesh.devins.ui.compose.agent.codereview.CommitInfo
import cc.unitmesh.devins.ui.compose.agent.codereview.DiffFileInfo
import cc.unitmesh.devins.ui.compose.agent.codereview.formatDate

/**
 * Truncate a file path by showing first/last segments with ... in middle
 * Examples:
 * - "src/main/kotlin/com/example/project/VeryLongFileName.kt" -> "src/.../VeryLongFileName.kt"
 * - "short/path.kt" -> "short/path.kt"
 */
private fun truncateFilePath(path: String): String {
    val segments = path.split("/")

    // If path is short enough, return as is
    if (segments.size <= 2) return path

    // Always use format: first/.../ last
    val first = segments.first()
    val last = segments.last()
    return "$first/.../$last"
}

/**
 * Left panel: Commit history list (GitHub-style) with infinite scroll
 */
@Composable
fun CommitListView(
    commits: List<CommitInfo>,
    selectedIndex: Int,
    onCommitSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hasMoreCommits: Boolean = false,
    isLoadingMore: Boolean = false,
    totalCommitCount: Int? = null,
    onLoadMore: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        val displayText = when {
            totalCommitCount != null -> "Commits (${commits.size}/$totalCommitCount)"
            hasMoreCommits -> "Commits (${commits.size}+)"
            else -> "Commits (${commits.size})"
        }

        Text(
            text = displayText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Companion.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

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

                if (index == commits.size - 5 && hasMoreCommits && !isLoadingMore) {
                    LaunchedEffect(Unit) {
                        onLoadMore()
                    }
                }
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
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
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Text(
                    text = commit.message.lines().firstOrNull() ?: commit.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Companion.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Companion.Ellipsis,
                    modifier = Modifier.weight(1f)
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
            Spacer(modifier = Modifier.height(4.dp))
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
    modifier: Modifier = Modifier,
    onViewFile: ((String) -> Unit)? = null,
    workspaceRoot: String? = null,
    isLoadingDiff: Boolean = false
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
                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = selectedCommit.message.lines().firstOrNull() ?: selectedCommit.message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Companion.Bold,
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
                            fontFamily = FontFamily.Companion.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "Files changed (${diffFiles.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Companion.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        if (isLoadingDiff) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Companion.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = AutoDevColors.Indigo.c600
                    )
                    Text(
                        text = "Loading diff...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (diffFiles.isEmpty()) {
            Box(
                modifier = Modifier
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
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(diffFiles.size) { index ->
                    CollapsibleFileDiffItem(
                        file = diffFiles[index],
                        onViewFile = if (onViewFile != null && workspaceRoot != null) {
                            { path ->
                                val fullPath = if (path.startsWith("/")) path else "$workspaceRoot/$path"
                                onViewFile(fullPath)
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun CollapsibleFileDiffItem(
    file: DiffFileInfo,
    onViewFile: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

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
                            ChangeType.CREATE -> AutoDevComposeIcons.Add
                            ChangeType.DELETE -> AutoDevComposeIcons.Delete
                            ChangeType.EDIT -> AutoDevComposeIcons.Edit
                            ChangeType.RENAME -> AutoDevComposeIcons.DriveFileRenameOutline
                        },
                        contentDescription = file.changeType.name,
                        tint = when (file.changeType) {
                            ChangeType.CREATE -> AutoDevColors.Green.c600
                            ChangeType.DELETE -> AutoDevColors.Red.c600
                            ChangeType.EDIT -> AutoDevColors.Blue.c600
                            ChangeType.RENAME -> AutoDevColors.Amber.c600
                        },
                        modifier = Modifier.size(18.dp)
                    )

                    // File path with clickable expand
                    Text(
                        text = truncateFilePath(file.path),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable { expanded = !expanded }
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

                // Action buttons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy path button
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(file.path))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.ContentCopy,
                            contentDescription = "Copy path",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // View file button (only on supported platforms)
                    if (onViewFile != null) {
                        IconButton(
                            onClick = { onViewFile(file.path) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Visibility,
                                contentDescription = "View file",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Expand/collapse button
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
            .padding(4.dp)
    ) {
        // Display hunk header (already formatted in DiffHunk.header)
        Text(
            text = hunk.header,
            fontFamily = FontFamily.Companion.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        hunk.lines.forEach { line ->
            // Skip HEADER type lines (they're handled above)
            if (line.type != DiffLineType.HEADER) {
                DiffLineView(line)
            }
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

        DiffLineType.HEADER -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onSurfaceVariant,
            ""
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
            text = line.oldLineNumber?.toString() ?: "",
            fontFamily = FontFamily.Companion.Monospace,
            fontSize = 10.sp,
            color = AutoDevColors.Diff.Dark.lineNumber,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(4.dp))

        // New line number
        Text(
            text = line.newLineNumber?.toString() ?: "",
            fontFamily = FontFamily.Companion.Monospace,
            fontSize = 10.sp,
            color = AutoDevColors.Diff.Dark.lineNumber,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Line prefix (+/-/ )
        Text(
            text = prefix,
            fontFamily = FontFamily.Companion.Monospace,
            fontSize = 11.sp,
            color = textColor,
            modifier = Modifier.width(12.dp)
        )

        // Line content
        Text(
            text = line.content,
            fontFamily = FontFamily.Companion.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
