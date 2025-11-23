package cc.unitmesh.devins.ui.compose.agent.codereview.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.agent.diff.DiffHunk
import cc.unitmesh.agent.diff.DiffLine
import cc.unitmesh.agent.diff.DiffLineType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import cc.unitmesh.devins.ui.compose.agent.codereview.CommitInfo
import cc.unitmesh.devins.ui.compose.agent.codereview.DiffFileInfo
import cc.unitmesh.devins.ui.compose.agent.codereview.TestFileInfo
import cc.unitmesh.devins.ui.compose.agent.codereview.QualityReviewPanel
import cc.unitmesh.devins.ui.compose.agent.VerticalResizableSplitPane
import androidx.compose.foundation.lazy.items

@Composable
fun DiffCenterView(
    diffFiles: List<DiffFileInfo>,
    selectedCommits: List<CommitInfo>,
    modifier: Modifier = Modifier,
    onViewFile: ((String) -> Unit)? = null,
    onViewFileWithLines: ((String, Int, Int) -> Unit)? = null,
    workspaceRoot: String? = null,
    isLoadingDiff: Boolean = false,
    onConfigureToken: () -> Unit = {},
    // Test coverage data
    relatedTests: Map<String, List<TestFileInfo>> = emptyMap(),
    isLoadingTests: Boolean = false
) {
    var viewMode by remember { mutableStateOf(FileViewMode.LIST) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        // Header with commit info and issue info
        if (selectedCommits.isNotEmpty()) {
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
                    if (selectedCommits.size == 1) {
                        val selectedCommit = selectedCommits.first()
                        // Single commit view (existing logic)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Companion.Top
                        ) {
                            Text(
                                text = selectedCommit.message.lines().firstOrNull() ?: selectedCommit.message,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Companion.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Inline issue indicator
                            when {
                                selectedCommit.isLoadingIssue -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = AutoDevColors.Indigo.c600
                                    )
                                }
                                selectedCommit.issueInfo != null -> {
                                    InlineIssueChip(issueInfo = selectedCommit.issueInfo)
                                }
                                selectedCommit.issueLoadError != null -> {
                                    Button(
                                        onClick = onConfigureToken,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = AutoDevComposeIcons.Settings,
                                                contentDescription = "Configure",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Configure Token",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }

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

                        // Expanded issue information (if available)
                        if (selectedCommit.issueInfo != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            IssueInfoCard(issueInfo = selectedCommit.issueInfo)
                        }
                    } else {
                        // Multiple commits view
                        val newest = selectedCommits.first() // Assuming sorted by date desc
                        val oldest = selectedCommits.last()

                        Text(
                            text = "${selectedCommits.size} commits selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Companion.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Range: ${oldest.shortHash}..${newest.shortHash}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Companion.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val authors = selectedCommits.map { it.author }.distinct()
                        Text(
                            text = "Authors: ${authors.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Files header with view mode toggle
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Files changed (${diffFiles.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Companion.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // View mode toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { viewMode = FileViewMode.LIST },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.List,
                        contentDescription = "List view",
                        tint = if (viewMode == FileViewMode.LIST)
                            AutoDevColors.Indigo.c600
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { viewMode = FileViewMode.TREE },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.AccountTree,
                        contentDescription = "Tree view",
                        tint = if (viewMode == FileViewMode.TREE)
                            AutoDevColors.Indigo.c600
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

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
            // Collect all related tests (used in both views)
            val allTests = relatedTests.values.flatten()

            // Show vertical split pane if we have tests, otherwise show normal view
            if (allTests.isNotEmpty()) {
                VerticalResizableSplitPane(
                    modifier = Modifier.fillMaxSize(),
                    initialSplitRatio = 0.65f,
                    minRatio = 0.3f,
                    maxRatio = 0.85f,
                    dividerHeight = 8,
                    saveKey = "diff_quality_split",
                    top = {
                        // File list at top
                        when (viewMode) {
                            FileViewMode.LIST -> {
                                CompactFileListView(
                                    files = diffFiles,
                                    relatedTests = emptyList(), // Don't show tests in list, they're in bottom pane
                                    onViewFile = onViewFile,
                                    workspaceRoot = workspaceRoot
                                )
                            }
                            FileViewMode.TREE -> {
                                FileTreeView(
                                    files = diffFiles,
                                    onViewFile = onViewFile,
                                    workspaceRoot = workspaceRoot
                                )
                            }
                        }
                    },
                    bottom = {
                        // Quality review panel at bottom
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            QualityReviewPanel(
                                testFiles = allTests,
                                onTestFileClick = onViewFile,
                                onTestCaseClick = onViewFileWithLines,
                                workspaceRoot = workspaceRoot
                            )
                        }
                    }
                )
            } else {
                // No tests available, show normal file list
                when (viewMode) {
                    FileViewMode.LIST -> {
                        CompactFileListView(
                            files = diffFiles,
                            relatedTests = emptyList(),
                            onViewFile = onViewFile,
                            onViewFileWithLines = onViewFileWithLines,
                            workspaceRoot = workspaceRoot
                        )
                    }
                    FileViewMode.TREE -> {
                        FileTreeView(
                            files = diffFiles,
                            onViewFile = onViewFile,
                            workspaceRoot = workspaceRoot
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact file list view (no cards, just rows)
 */
@Composable
fun CompactFileListView(
    files: List<DiffFileInfo>,
    relatedTests: List<TestFileInfo> = emptyList(),
    onViewFile: ((String) -> Unit)?,
    onViewFileWithLines: ((String, Int, Int) -> Unit)? = null,
    workspaceRoot: String?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(files) { file ->
            CompactFileDiffItem(
                file = file,
                onViewFile = if (onViewFile != null && workspaceRoot != null) {
                    { path ->
                        val fullPath = if (path.startsWith("/")) path else "$workspaceRoot/$path"
                        onViewFile(fullPath)
                    }
                } else null
            )
        }

        if (relatedTests.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    QualityReviewPanel(
                        testFiles = relatedTests,
                        onTestFileClick = onViewFile,
                        onTestCaseClick = onViewFileWithLines,
                        workspaceRoot = workspaceRoot
                    )
                }
            }
        }
    }
}

/**
 * Compact file diff item - no card wrapper, just a row with hover background
 */
@Composable
fun CompactFileDiffItem(
    file: DiffFileInfo,
    onViewFile: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (expanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else Color.Transparent
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Change type icon (smaller, inline)
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
                    modifier = Modifier.size(16.dp)
                )

                // File path (show full path, let it wrap if needed)
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Language badge (smaller)
                file.language?.let { lang ->
                    Text(
                        text = lang,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Action buttons (smaller, inline)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy path button
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(file.path))
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.ContentCopy,
                        contentDescription = "Copy path",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // View file button
                if (onViewFile != null) {
                    IconButton(
                        onClick = { onViewFile(file.path) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Visibility,
                            contentDescription = "View file",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Expand/collapse button
                Icon(
                    imageVector = if (expanded) AutoDevComposeIcons.ExpandLess else AutoDevComposeIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Expanded diff content
        if (expanded && file.hunks.isNotEmpty()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            ) {
                file.hunks.forEach { hunk ->
                    DiffHunkView(hunk)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // Separator line between files
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 1.dp
        )
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

