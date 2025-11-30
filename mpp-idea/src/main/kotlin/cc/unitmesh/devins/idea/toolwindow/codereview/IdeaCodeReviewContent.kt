package cc.unitmesh.devins.idea.toolwindow.codereview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.codereview.ModifiedCodeRange
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.DiffLineType
import cc.unitmesh.agent.linter.LintFileResult
import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.devins.idea.renderer.sketch.IdeaSketchRenderer
import cc.unitmesh.devins.idea.toolwindow.components.IdeaResizableSplitPane
import cc.unitmesh.devins.ui.compose.agent.codereview.*
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.Disposable
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

/**
 * Main Code Review content composable for IntelliJ IDEA plugin.
 * Uses Jewel UI components for IntelliJ-native look and feel.
 *
 * Features a three-column resizable layout:
 * - Left: Commit history list
 * - Center: Diff viewer with file tabs
 * - Right: AI Analysis with Plan, User Input, and Fix generation sections
 */
@Composable
fun IdeaCodeReviewContent(
    viewModel: IdeaCodeReviewViewModel,
    parentDisposable: Disposable
) {
    val state by viewModel.state.collectAsState()

    IdeaResizableSplitPane(
        modifier = Modifier.fillMaxSize(),
        initialSplitRatio = 0.18f,
        minRatio = 0.12f,
        maxRatio = 0.35f,
        first = {
            // Left panel: Commit list
            CommitListPanel(
                commits = state.commitHistory,
                selectedIndices = state.selectedCommitIndices,
                isLoading = state.isLoading,
                onCommitSelect = { index -> viewModel.selectCommit(index) },
                modifier = Modifier.fillMaxSize()
            )
        },
        second = {
            // Center + Right: Diff view and AI analysis
            IdeaResizableSplitPane(
                modifier = Modifier.fillMaxSize(),
                initialSplitRatio = 0.55f,
                minRatio = 0.35f,
                maxRatio = 0.75f,
                first = {
                    // Center panel: Diff viewer with commit info and file list
                    val selectedCommits = state.selectedCommitIndices.mapNotNull { index ->
                        state.commitHistory.getOrNull(index)
                    }
                    DiffViewerPanel(
                        diffFiles = state.diffFiles,
                        selectedCommits = selectedCommits,
                        selectedCommitIndices = state.selectedCommitIndices,
                        isLoadingDiff = state.isLoadingDiff,
                        onViewFile = { path -> viewModel.openFileViewer(path) },
                        onRefreshIssue = { index -> viewModel.refreshIssueForCommit(index) },
                        onConfigureToken = { /* TODO: Open token configuration */ },
                        modifier = Modifier.fillMaxSize()
                    )
                },
                second = {
                    // Right panel: AI Analysis with Plan and Fix UI
                    IdeaAIAnalysisPanel(
                        state = state,
                        viewModel = viewModel,
                        parentDisposable = parentDisposable,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        }
    )
}

@Composable
private fun CommitListPanel(
    commits: List<CommitInfo>,
    selectedIndices: Set<Int>,
    isLoading: Boolean,
    onCommitSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(JewelTheme.globalColors.panelBackground)) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Commits",
                style = JewelTheme.defaultTextStyle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (commits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No commits found",
                    style = JewelTheme.defaultTextStyle.copy(
                        color = JewelTheme.globalColors.text.info
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = rememberLazyListState()
            ) {
                itemsIndexed(commits) { index, commit ->
                    CommitItem(
                        commit = commit,
                        isSelected = index in selectedIndices,
                        onClick = { onCommitSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommitItem(
    commit: CommitInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        JewelTheme.globalColors.panelBackground.copy(alpha = 0.8f)
    } else {
        JewelTheme.globalColors.panelBackground
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = commit.shortHash,
                style = JewelTheme.defaultTextStyle.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = AutoDevColors.Blue.c400
                )
            )
            Text(
                text = commit.date,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    color = JewelTheme.globalColors.text.info
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = commit.message.lines().firstOrNull() ?: "",
            style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp),
            maxLines = 2
        )
    }
}

/**
 * File view mode for diff display
 */
private enum class IdeaFileViewMode {
    LIST,  // Flat list of files
    TREE   // Tree structure grouped by directory
}

/**
 * Redesigned DiffViewerPanel matching DiffCenterView from mpp-ui.
 * Features:
 * - Commit info card with issue display
 * - File view mode toggle (list/tree)
 * - Expandable file list with diff hunks
 * - Issue loading/error states with refresh
 */
@Composable
private fun DiffViewerPanel(
    diffFiles: List<DiffFileInfo>,
    selectedCommits: List<CommitInfo>,
    selectedCommitIndices: Set<Int>,
    isLoadingDiff: Boolean,
    onViewFile: ((String) -> Unit)? = null,
    onRefreshIssue: ((Int) -> Unit)? = null,
    onConfigureToken: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(IdeaFileViewMode.LIST) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
            .padding(8.dp)
    ) {
        // Header with commit info and issue info
        if (selectedCommits.isNotEmpty()) {
            IdeaCommitInfoCard(
                selectedCommits = selectedCommits,
                selectedCommitIndices = selectedCommitIndices.toList(),
                onRefreshIssue = onRefreshIssue,
                onConfigureToken = onConfigureToken
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Files header with view mode toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Files changed (${diffFiles.size})",
                style = JewelTheme.defaultTextStyle.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            )

            // View mode toggle
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { viewMode = IdeaFileViewMode.LIST },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.List,
                        contentDescription = "List view",
                        tint = if (viewMode == IdeaFileViewMode.LIST)
                            AutoDevColors.Indigo.c600
                        else
                            JewelTheme.globalColors.text.info,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { viewMode = IdeaFileViewMode.TREE },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.AccountTree,
                        contentDescription = "Tree view",
                        tint = if (viewMode == IdeaFileViewMode.TREE)
                            AutoDevColors.Indigo.c600
                        else
                            JewelTheme.globalColors.text.info,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Content area
        if (isLoadingDiff) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading diff...",
                        style = JewelTheme.defaultTextStyle.copy(color = JewelTheme.globalColors.text.info)
                    )
                }
            }
        } else if (diffFiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedCommits.isEmpty()) "Select a commit to view diff" else "No file changes in this commit",
                    style = JewelTheme.defaultTextStyle.copy(color = JewelTheme.globalColors.text.info)
                )
            }
        } else {
            // File list based on view mode
            when (viewMode) {
                IdeaFileViewMode.LIST -> {
                    IdeaCompactFileListView(
                        files = diffFiles,
                        onViewFile = onViewFile
                    )
                }
                IdeaFileViewMode.TREE -> {
                    IdeaFileTreeView(
                        files = diffFiles,
                        onViewFile = onViewFile
                    )
                }
            }
        }
    }
}

/**
 * Commit info card with issue display
 * @param selectedCommitIndices The actual indices in the commit history for proper refresh targeting
 */
@Composable
private fun IdeaCommitInfoCard(
    selectedCommits: List<CommitInfo>,
    selectedCommitIndices: List<Int>,
    onRefreshIssue: ((Int) -> Unit)?,
    onConfigureToken: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                JewelTheme.globalColors.panelBackground.copy(alpha = 0.6f),
                RoundedCornerShape(6.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (selectedCommits.size == 1) {
                val selectedCommit = selectedCommits.first()
                // Single commit view
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = selectedCommit.message.lines().firstOrNull() ?: selectedCommit.message,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Inline issue indicator - use the actual commit index
                    val actualCommitIndex = selectedCommitIndices.firstOrNull() ?: 0
                    IdeaIssueIndicator(
                        commit = selectedCommit,
                        commitIndex = actualCommitIndex,
                        onRefreshIssue = onRefreshIssue,
                        onConfigureToken = onConfigureToken
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = selectedCommit.author,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 12.sp,
                            color = JewelTheme.globalColors.text.info
                        )
                    )
                    Text(
                        text = selectedCommit.shortHash,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = JewelTheme.globalColors.text.info.copy(alpha = 0.7f)
                        )
                    )
                }

                // Expanded issue information (if available)
                selectedCommit.issueInfo?.let { issueInfo ->
                    Spacer(modifier = Modifier.height(8.dp))
                    IdeaIssueInfoCard(issueInfo = issueInfo)
                }
            } else {
                // Multiple commits view
                val newest = selectedCommits.first()
                val oldest = selectedCommits.last()

                Text(
                    text = "${selectedCommits.size} commits selected",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Range: ${oldest.shortHash}..${newest.shortHash}",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = JewelTheme.globalColors.text.info
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                val authors = selectedCommits.map { it.author }.distinct()
                Text(
                    text = "Authors: ${authors.joinToString(", ")}",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 12.sp,
                        color = JewelTheme.globalColors.text.info
                    )
                )
            }
        }
    }
}

/**
 * Issue indicator for commit (loading, info chip, error with retry)
 */
@Composable
private fun IdeaIssueIndicator(
    commit: CommitInfo,
    commitIndex: Int,
    onRefreshIssue: ((Int) -> Unit)?,
    onConfigureToken: () -> Unit
) {
    when {
        commit.isLoadingIssue -> {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }
        commit.issueInfo != null -> {
            val issueInfo = commit.issueInfo!!
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IdeaInlineIssueChip(issueInfo = issueInfo)

                // Show cache indicator and refresh button if from cache
                val cacheAge = commit.issueCacheAge
                if (commit.issueFromCache && cacheAge != null) {
                    Text(
                        text = cacheAge,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 10.sp,
                            color = JewelTheme.globalColors.text.info.copy(alpha = 0.6f)
                        )
                    )
                }

                // Refresh button
                if (onRefreshIssue != null) {
                    IconButton(
                        onClick = { onRefreshIssue(commitIndex) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.Refresh,
                            contentDescription = "Refresh issue",
                            tint = JewelTheme.globalColors.text.info.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        commit.issueLoadError != null -> {
            val errorMessage = commit.issueLoadError!!
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = errorMessage,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 10.sp,
                        color = AutoDevColors.Red.c400.copy(alpha = 0.8f)
                    )
                )

                // Retry button
                if (onRefreshIssue != null) {
                    IconButton(
                        onClick = { onRefreshIssue(commitIndex) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.Refresh,
                            contentDescription = "Retry",
                            tint = AutoDevColors.Red.c400.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Configure token button (only for auth errors)
                if (errorMessage.contains("Authentication", ignoreCase = true)) {
                    DefaultButton(
                        onClick = onConfigureToken,
                        modifier = Modifier.height(24.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.Settings,
                                contentDescription = "Configure",
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Token",
                                style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inline compact issue chip
 */
@Composable
private fun IdeaInlineIssueChip(issueInfo: cc.unitmesh.agent.tracker.IssueInfo) {
    Box(
        modifier = Modifier
            .background(
                when (issueInfo.status.lowercase()) {
                    "open" -> AutoDevColors.Green.c600.copy(alpha = 0.15f)
                    "closed" -> AutoDevColors.Neutral.c600.copy(alpha = 0.15f)
                    else -> AutoDevColors.Indigo.c600.copy(alpha = 0.15f)
                },
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (issueInfo.status.lowercase()) {
                    "open" -> cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.BugReport
                    "closed" -> cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.CheckCircle
                    else -> cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.Info
                },
                contentDescription = issueInfo.status,
                tint = when (issueInfo.status.lowercase()) {
                    "open" -> AutoDevColors.Green.c600
                    "closed" -> AutoDevColors.Neutral.c600
                    else -> AutoDevColors.Indigo.c600
                },
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "#${issueInfo.id}",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (issueInfo.status.lowercase()) {
                        "open" -> AutoDevColors.Green.c600
                        "closed" -> AutoDevColors.Neutral.c600
                        else -> AutoDevColors.Indigo.c600
                    }
                )
            )
        }
    }
}

/**
 * Issue info card with full details
 */
@Composable
private fun IdeaIssueInfoCard(issueInfo: cc.unitmesh.agent.tracker.IssueInfo) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                AutoDevColors.Indigo.c600.copy(alpha = 0.1f),
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.BugReport,
                        contentDescription = "Issue",
                        tint = AutoDevColors.Indigo.c600,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "#${issueInfo.id}",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AutoDevColors.Indigo.c600
                        )
                    )
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .background(
                            when (issueInfo.status.lowercase()) {
                                "open" -> AutoDevColors.Green.c600.copy(alpha = 0.2f)
                                "closed" -> AutoDevColors.Red.c600.copy(alpha = 0.2f)
                                else -> JewelTheme.globalColors.panelBackground
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = issueInfo.status,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 10.sp,
                            color = when (issueInfo.status.lowercase()) {
                                "open" -> AutoDevColors.Green.c600
                                "closed" -> AutoDevColors.Red.c600
                                else -> JewelTheme.globalColors.text.info
                            }
                        )
                    )
                }
            }

            Text(
                text = issueInfo.title,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2
            )

            if (issueInfo.description.isNotBlank()) {
                Text(
                    text = issueInfo.description,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 11.sp,
                        color = JewelTheme.globalColors.text.info
                    ),
                    maxLines = 3
                )
            }

            // Labels
            if (issueInfo.labels.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    issueInfo.labels.take(5).forEach { label ->
                        Box(
                            modifier = Modifier
                                .background(
                                    AutoDevColors.Indigo.c600.copy(alpha = 0.15f),
                                    RoundedCornerShape(3.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                style = JewelTheme.defaultTextStyle.copy(fontSize = 10.sp)
                            )
                        }
                    }
                    if (issueInfo.labels.size > 5) {
                        Text(
                            text = "+${issueInfo.labels.size - 5}",
                            style = JewelTheme.defaultTextStyle.copy(
                                fontSize = 10.sp,
                                color = JewelTheme.globalColors.text.info
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact file list view with expandable diff items
 */
@Composable
private fun IdeaCompactFileListView(
    files: List<DiffFileInfo>,
    onViewFile: ((String) -> Unit)?
) {
    val scrollState = rememberLazyListState()
    var expandedFileIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(files) { index, file ->
            IdeaCompactFileDiffItem(
                file = file,
                isExpanded = expandedFileIndex == index,
                onToggleExpand = {
                    expandedFileIndex = if (expandedFileIndex == index) null else index
                },
                onViewFile = onViewFile
            )
        }
    }
}

/**
 * Compact file diff item with expandable hunks
 */
@Composable
private fun IdeaCompactFileDiffItem(
    file: DiffFileInfo,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onViewFile: ((String) -> Unit)?
) {
    val changeColor = when (file.changeType) {
        ChangeType.CREATE -> AutoDevColors.Green.c400
        ChangeType.DELETE -> AutoDevColors.Red.c400
        ChangeType.RENAME -> AutoDevColors.Amber.c400
        else -> AutoDevColors.Blue.c400
    }

    val changeIcon = when (file.changeType) {
        ChangeType.CREATE -> cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.Add
        ChangeType.DELETE -> cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.Delete
        ChangeType.RENAME -> cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.DriveFileRenameOutline
        else -> cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.Edit
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // File header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Expand/collapse icon
                Icon(
                    imageVector = if (isExpanded)
                        cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.ExpandMore
                    else
                        cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = JewelTheme.globalColors.text.info,
                    modifier = Modifier.size(16.dp)
                )

                // Change type icon
                Icon(
                    imageVector = changeIcon,
                    contentDescription = file.changeType.name,
                    tint = changeColor,
                    modifier = Modifier.size(14.dp)
                )

                // File name
                Text(
                    text = file.path.split("/").lastOrNull() ?: file.path,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                // File path (directory)
                val directory = file.path.substringBeforeLast("/", "")
                if (directory.isNotEmpty()) {
                    Text(
                        text = directory,
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 11.sp,
                            color = JewelTheme.globalColors.text.info.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // View file button
                if (onViewFile != null) {
                    IconButton(
                        onClick = { onViewFile(file.path) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.Visibility,
                            contentDescription = "View file",
                            tint = JewelTheme.globalColors.text.info,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Line count badge
                val addedLines = file.hunks.sumOf { hunk ->
                    hunk.lines.count { it.type == DiffLineType.ADDED }
                }
                val deletedLines = file.hunks.sumOf { hunk ->
                    hunk.lines.count { it.type == DiffLineType.DELETED }
                }

                if (addedLines > 0) {
                    Text(
                        text = "+$addedLines",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 10.sp,
                            color = AutoDevColors.Green.c400,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                if (deletedLines > 0) {
                    Text(
                        text = "-$deletedLines",
                        style = JewelTheme.defaultTextStyle.copy(
                            fontSize = 10.sp,
                            color = AutoDevColors.Red.c400,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Expanded diff content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp, bottom = 8.dp)
                    .background(
                        JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {
                file.hunks.forEachIndexed { hunkIndex, hunk ->
                    if (hunkIndex > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    IdeaDiffHunkView(hunk = hunk)
                }
            }
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))
    }
}

/**
 * Diff hunk view with line numbers and content
 */
@Composable
private fun IdeaDiffHunkView(hunk: cc.unitmesh.agent.diff.DiffHunk) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Hunk header
        Text(
            text = "@@ -${hunk.oldStartLine},${hunk.oldLineCount} +${hunk.newStartLine},${hunk.newLineCount} @@",
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = AutoDevColors.Blue.c400
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Lines
        hunk.lines.forEach { line ->
            IdeaDiffLineView(line = line)
        }
    }
}

/**
 * Single diff line view
 */
@Composable
private fun IdeaDiffLineView(line: cc.unitmesh.agent.diff.DiffLine) {
    val backgroundColor = when (line.type) {
        DiffLineType.ADDED -> AutoDevColors.Green.c400.copy(alpha = 0.15f)
        DiffLineType.DELETED -> AutoDevColors.Red.c400.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val textColor = when (line.type) {
        DiffLineType.ADDED -> AutoDevColors.Green.c400
        DiffLineType.DELETED -> AutoDevColors.Red.c400
        else -> JewelTheme.globalColors.text.normal
    }

    val prefix = when (line.type) {
        DiffLineType.ADDED -> "+"
        DiffLineType.DELETED -> "-"
        else -> " "
    }

    // Use appropriate line number based on line type
    val displayLineNumber = when (line.type) {
        DiffLineType.ADDED -> line.newLineNumber
        DiffLineType.DELETED -> line.oldLineNumber
        else -> line.newLineNumber ?: line.oldLineNumber
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        // Line number
        Text(
            text = (displayLineNumber ?: 0).toString().padStart(4),
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = JewelTheme.globalColors.text.info.copy(alpha = 0.5f)
            ),
            modifier = Modifier.width(36.dp)
        )

        // Prefix
        Text(
            text = prefix,
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = textColor,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.width(12.dp)
        )

        // Content
        Text(
            text = line.content.removePrefix(prefix),
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = textColor
            ),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        )
    }
}

/**
 * File tree view with directory grouping
 * Uses file.path as unique identifier for O(1) expansion tracking instead of indexOf
 */
@Composable
private fun IdeaFileTreeView(
    files: List<DiffFileInfo>,
    onViewFile: ((String) -> Unit)?
) {
    val scrollState = rememberLazyListState()
    val treeNodes = remember(files) { buildFileTreeStructure(files) }
    var expandedDirs by remember { mutableStateOf(setOf<String>()) }
    // Use file path as identifier instead of index for O(1) lookup
    var expandedFilePath by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize()
    ) {
        treeNodes.forEach { node ->
            when (node) {
                is FileTreeNode.Directory -> {
                    item(key = "dir_${node.path}") {
                        IdeaDirectoryTreeItem(
                            directory = node,
                            isExpanded = expandedDirs.contains(node.path),
                            onToggle = {
                                expandedDirs = if (expandedDirs.contains(node.path)) {
                                    expandedDirs - node.path
                                } else {
                                    expandedDirs + node.path
                                }
                            }
                        )
                    }

                    if (expandedDirs.contains(node.path)) {
                        node.files.forEachIndexed { index, file ->
                            item(key = "file_${node.path}_$index") {
                                IdeaFileTreeItemCompact(
                                    file = file,
                                    isExpanded = expandedFilePath == file.path,
                                    onToggleExpand = {
                                        expandedFilePath = if (expandedFilePath == file.path) null else file.path
                                    },
                                    onViewFile = onViewFile,
                                    indentLevel = 1
                                )
                            }
                        }
                    }
                }
                is FileTreeNode.File -> {
                    item(key = "file_root_${node.file.path}") {
                        IdeaFileTreeItemCompact(
                            file = node.file,
                            isExpanded = expandedFilePath == node.file.path,
                            onToggleExpand = {
                                expandedFilePath = if (expandedFilePath == node.file.path) null else node.file.path
                            },
                            onViewFile = onViewFile,
                            indentLevel = 0
                        )
                    }
                }
            }
        }
    }
}

/**
 * File tree node sealed class
 */
private sealed class FileTreeNode {
    data class Directory(
        val name: String,
        val path: String,
        val files: List<DiffFileInfo>
    ) : FileTreeNode()

    data class File(val file: DiffFileInfo) : FileTreeNode()
}

/**
 * Build file tree structure from flat file list
 */
private fun buildFileTreeStructure(files: List<DiffFileInfo>): List<FileTreeNode> {
    val result = mutableListOf<FileTreeNode>()
    val directoryMap = mutableMapOf<String, MutableList<DiffFileInfo>>()

    files.forEach { file ->
        val directory = file.path.substringBeforeLast("/", "")
        if (directory.isEmpty()) {
            result.add(FileTreeNode.File(file))
        } else {
            directoryMap.getOrPut(directory) { mutableListOf() }.add(file)
        }
    }

    directoryMap.entries.sortedBy { it.key }.forEach { (path, dirFiles) ->
        result.add(FileTreeNode.Directory(
            name = path.split("/").lastOrNull() ?: path,
            path = path,
            files = dirFiles.sortedBy { it.path }
        ))
    }

    return result
}

/**
 * Directory tree item
 */
@Composable
private fun IdeaDirectoryTreeItem(
    directory: FileTreeNode.Directory,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isExpanded)
                cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.ExpandMore
            else
                cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.ChevronRight,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = JewelTheme.globalColors.text.info,
            modifier = Modifier.size(16.dp)
        )

        Icon(
            imageVector = if (isExpanded)
                cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.FolderOpen
            else
                cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.Folder,
            contentDescription = "Directory",
            tint = AutoDevColors.Amber.c400,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = directory.name,
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Text(
            text = "(${directory.files.size})",
            style = JewelTheme.defaultTextStyle.copy(
                fontSize = 11.sp,
                color = JewelTheme.globalColors.text.info.copy(alpha = 0.6f)
            )
        )
    }
}

/**
 * File tree item (compact version for tree view)
 */
@Composable
private fun IdeaFileTreeItemCompact(
    file: DiffFileInfo,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onViewFile: ((String) -> Unit)?,
    indentLevel: Int
) {
    val changeColor = when (file.changeType) {
        ChangeType.CREATE -> AutoDevColors.Green.c400
        ChangeType.DELETE -> AutoDevColors.Red.c400
        ChangeType.RENAME -> AutoDevColors.Amber.c400
        else -> AutoDevColors.Blue.c400
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(start = (8 + indentLevel * 16).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isExpanded)
                        cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.ExpandMore
                    else
                        cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = JewelTheme.globalColors.text.info,
                    modifier = Modifier.size(14.dp)
                )

                Icon(
                    imageVector = cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.Description,
                    contentDescription = "File",
                    tint = changeColor,
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = file.path.split("/").lastOrNull() ?: file.path,
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp)
                )
            }

            // View file button
            if (onViewFile != null) {
                IconButton(
                    onClick = { onViewFile(file.path) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons.Visibility,
                        contentDescription = "View file",
                        tint = JewelTheme.globalColors.text.info,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Expanded diff content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (24 + indentLevel * 16).dp, end = 8.dp, bottom = 8.dp)
                    .background(
                        JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {
                file.hunks.forEachIndexed { hunkIndex, hunk ->
                    if (hunkIndex > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    IdeaDiffHunkView(hunk = hunk)
                }
            }
        }
    }
}

/**
 * Comprehensive AI Analysis Panel with Plan, User Input, and Fix sections.
 * Redesigned to match the CodeReviewAgentPanel from mpp-ui.
 */
@Composable
private fun IdeaAIAnalysisPanel(
    state: CodeReviewState,
    viewModel: IdeaCodeReviewViewModel,
    parentDisposable: Disposable,
    modifier: Modifier = Modifier
) {
    val progress = state.aiProgress

    Column(modifier = modifier.background(JewelTheme.globalColors.panelBackground)) {
        // Header with action button
        IdeaAnalysisHeader(
            stage = progress.stage,
            hasDiffFiles = state.diffFiles.isNotEmpty(),
            onStartAnalysis = { viewModel.startAnalysis() },
            onCancelAnalysis = { viewModel.cancelAnalysis() }
        )

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Error message
        state.error?.let { error ->
            Text(
                text = error,
                style = JewelTheme.defaultTextStyle.copy(
                    color = AutoDevColors.Red.c400,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Content area with scrollable sections
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            if (progress.stage == AnalysisStage.IDLE && progress.lintResults.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Click 'Start Review' to analyze code changes with AI",
                            style = JewelTheme.defaultTextStyle.copy(
                                color = JewelTheme.globalColors.text.info,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Lint Analysis Section
                    if (progress.lintResults.isNotEmpty() || progress.lintOutput.isNotEmpty()) {
                        item {
                            IdeaLintAnalysisCard(
                                lintResults = progress.lintResults,
                                lintOutput = progress.lintOutput,
                                isActive = progress.stage == AnalysisStage.RUNNING_LINT,
                                diffFiles = state.diffFiles,
                                modifiedCodeRanges = progress.modifiedCodeRanges
                            )
                        }
                    }

                    // AI Analysis Section
                    if (progress.analysisOutput.isNotEmpty()) {
                        item {
                            IdeaAIAnalysisSection(
                                analysisOutput = progress.analysisOutput,
                                isActive = progress.stage == AnalysisStage.ANALYZING_LINT,
                                parentDisposable = parentDisposable
                            )
                        }
                    }

                    // Modification Plan Section
                    if (progress.planOutput.isNotEmpty()) {
                        item {
                            IdeaModificationPlanSection(
                                planOutput = progress.planOutput,
                                isActive = progress.stage == AnalysisStage.GENERATING_PLAN,
                                parentDisposable = parentDisposable,
                                onItemSelectionChanged = { selection ->
                                    viewModel.setSelectedPlanItems(selection)
                                }
                            )
                        }
                    }

                    // User Input Section (when waiting for feedback)
                    if (progress.stage == AnalysisStage.WAITING_FOR_USER_INPUT) {
                        item {
                            IdeaUserInputSection(
                                onGenerate = { feedback ->
                                    viewModel.proceedToGenerateFixes(feedback)
                                },
                                onCancel = { viewModel.cancelAnalysis() }
                            )
                        }
                    }

                    // Fix Generation Section
                    if (progress.fixRenderer != null || progress.stage == AnalysisStage.GENERATING_FIX) {
                        item {
                            IdeaSuggestedFixesSection(
                                fixOutput = progress.fixOutput,
                                isGenerating = progress.stage == AnalysisStage.GENERATING_FIX,
                                parentDisposable = parentDisposable
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header component with status and action buttons
 */
@Composable
private fun IdeaAnalysisHeader(
    stage: AnalysisStage,
    hasDiffFiles: Boolean,
    onStartAnalysis: () -> Unit,
    onCancelAnalysis: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Code Review",
                style = JewelTheme.defaultTextStyle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )

            // Status badge
            val (statusText, statusColor) = when (stage) {
                AnalysisStage.IDLE -> "Ready" to JewelTheme.globalColors.text.info
                AnalysisStage.RUNNING_LINT -> "Linting..." to AutoDevColors.Amber.c400
                AnalysisStage.ANALYZING_LINT -> "Analyzing..." to AutoDevColors.Blue.c400
                AnalysisStage.GENERATING_PLAN -> "Planning..." to AutoDevColors.Blue.c400
                AnalysisStage.WAITING_FOR_USER_INPUT -> "Awaiting Input" to AutoDevColors.Amber.c400
                AnalysisStage.GENERATING_FIX -> "Fixing..." to AutoDevColors.Indigo.c400
                AnalysisStage.COMPLETED -> "Done" to AutoDevColors.Green.c400
                AnalysisStage.ERROR -> "Error" to AutoDevColors.Red.c400
            }

            if (stage != AnalysisStage.IDLE) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (stage != AnalysisStage.COMPLETED && stage != AnalysisStage.ERROR) {
                            CircularProgressIndicator()
                        }
                        Text(
                            text = statusText,
                            style = JewelTheme.defaultTextStyle.copy(
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // Action buttons
        when (stage) {
            AnalysisStage.IDLE, AnalysisStage.COMPLETED, AnalysisStage.ERROR -> {
                DefaultButton(
                    onClick = onStartAnalysis,
                    enabled = hasDiffFiles
                ) {
                    Text("Start Review")
                }
            }
            else -> {
                OutlinedButton(onClick = onCancelAnalysis) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Collapsible Lint Analysis Card showing lint results and filtered issues
 */
@Composable
private fun IdeaLintAnalysisCard(
    lintResults: List<LintFileResult>,
    lintOutput: String,
    isActive: Boolean,
    diffFiles: List<DiffFileInfo>,
    modifiedCodeRanges: Map<String, List<ModifiedCodeRange>>
) {
    var isExpanded by remember { mutableStateOf(true) }
    val totalErrors = lintResults.sumOf { it.errorCount }
    val totalWarnings = lintResults.sumOf { it.warningCount }

    IdeaCollapsibleCard(
        title = "Lint Analysis",
        isExpanded = isExpanded,
        onExpandChange = { isExpanded = it },
        isActive = isActive,
        badge = {
            if (totalErrors > 0 || totalWarnings > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (totalErrors > 0) {
                        IdeaBadge(text = "$totalErrors errors", color = AutoDevColors.Red.c400)
                    }
                    if (totalWarnings > 0) {
                        IdeaBadge(text = "$totalWarnings warnings", color = AutoDevColors.Amber.c400)
                    }
                }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lintResults.forEach { result ->
                if (result.issues.isNotEmpty()) {
                    IdeaLintFileCard(
                        fileResult = result,
                        modifiedRanges = modifiedCodeRanges[result.filePath] ?: emptyList()
                    )
                }
            }

            if (lintOutput.isNotEmpty() && lintResults.isEmpty()) {
                Text(
                    text = lintOutput,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

/**
 * Card showing lint issues for a single file
 */
@Composable
private fun IdeaLintFileCard(
    fileResult: LintFileResult,
    modifiedRanges: List<ModifiedCodeRange>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = fileResult.filePath.substringAfterLast("/"),
            style = JewelTheme.defaultTextStyle.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        )

        fileResult.issues.take(5).forEach { issue ->
            IdeaLintIssueRow(issue = issue, modifiedRanges = modifiedRanges)
        }

        if (fileResult.issues.size > 5) {
            Text(
                text = "...and ${fileResult.issues.size - 5} more issues",
                style = JewelTheme.defaultTextStyle.copy(
                    color = JewelTheme.globalColors.text.info,
                    fontSize = 11.sp
                )
            )
        }
    }
}

/**
 * Single lint issue row
 */
@Composable
private fun IdeaLintIssueRow(
    issue: LintIssue,
    modifiedRanges: List<ModifiedCodeRange>
) {
    val isInModifiedRange = modifiedRanges.any { range ->
        issue.line in range.startLine..range.endLine
    }

    val severityColor = when (issue.severity) {
        LintSeverity.ERROR -> AutoDevColors.Red.c400
        LintSeverity.WARNING -> AutoDevColors.Amber.c400
        LintSeverity.INFO -> AutoDevColors.Blue.c400
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "L${issue.line}",
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = if (isInModifiedRange) severityColor else JewelTheme.globalColors.text.info
            ),
            modifier = Modifier.width(40.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = issue.message,
                style = JewelTheme.defaultTextStyle.copy(fontSize = 11.sp)
            )
            issue.rule?.let { rule ->
                Text(
                    text = rule,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontSize = 10.sp,
                        color = JewelTheme.globalColors.text.info
                    )
                )
            }
        }
    }
}

/**
 * AI Analysis Section showing streaming AI analysis output
 */
@Composable
private fun IdeaAIAnalysisSection(
    analysisOutput: String,
    isActive: Boolean,
    parentDisposable: Disposable
) {
    var isExpanded by remember { mutableStateOf(true) }

    IdeaCollapsibleCard(
        title = "AI Analysis",
        isExpanded = isExpanded,
        onExpandChange = { isExpanded = it },
        isActive = isActive
    ) {
        IdeaSketchRenderer.RenderResponse(
            content = analysisOutput,
            isComplete = !isActive,
            parentDisposable = parentDisposable,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Modification Plan Section showing AI-generated fix plan
 */
@Composable
private fun IdeaModificationPlanSection(
    planOutput: String,
    isActive: Boolean,
    parentDisposable: Disposable,
    onItemSelectionChanged: (Set<Int>) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    IdeaCollapsibleCard(
        title = "Modification Plan",
        isExpanded = isExpanded,
        onExpandChange = { isExpanded = it },
        isActive = isActive,
        badge = {
            if (isActive) {
                IdeaBadge(text = "Generating...", color = AutoDevColors.Blue.c400)
            }
        }
    ) {
        IdeaSketchRenderer.RenderResponse(
            content = planOutput,
            isComplete = !isActive,
            parentDisposable = parentDisposable,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * User Input Section for providing feedback before fix generation
 */
@Composable
private fun IdeaUserInputSection(
    onGenerate: (String) -> Unit,
    onCancel: () -> Unit
) {
    var userInput by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }

    IdeaCollapsibleCard(
        title = "Your Feedback",
        isExpanded = true,
        onExpandChange = {},
        isActive = true,
        badge = {
            IdeaBadge(text = "Action Required", color = AutoDevColors.Amber.c400)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Review the plan above and provide any additional instructions:",
                style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp)
            )

            TextArea(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                placeholder = { Text("Optional: Add specific instructions or constraints...") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
                DefaultButton(onClick = { onGenerate(userInput.text) }) {
                    Text("Generate Fixes")
                }
            }
        }
    }
}

/**
 * Suggested Fixes Section showing fix generation output
 */
@Composable
private fun IdeaSuggestedFixesSection(
    fixOutput: String,
    isGenerating: Boolean,
    parentDisposable: Disposable
) {
    var isExpanded by remember { mutableStateOf(true) }

    IdeaCollapsibleCard(
        title = "Fix Generation",
        isExpanded = isExpanded,
        onExpandChange = { isExpanded = it },
        isActive = isGenerating,
        badge = {
            if (isGenerating) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    IdeaBadge(text = "Generating...", color = AutoDevColors.Indigo.c400)
                }
            } else if (fixOutput.isNotEmpty()) {
                IdeaBadge(text = "Complete", color = AutoDevColors.Green.c400)
            }
        }
    ) {
        if (fixOutput.isNotEmpty()) {
            IdeaSketchRenderer.RenderResponse(
                content = fixOutput,
                isComplete = !isGenerating,
                parentDisposable = parentDisposable,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (isGenerating) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Text(
                text = "No fixes generated yet.",
                style = JewelTheme.defaultTextStyle.copy(
                    color = JewelTheme.globalColors.text.info,
                    fontSize = 12.sp
                )
            )
        }
    }
}

/**
 * Reusable collapsible card component for sections
 */
@Composable
private fun IdeaCollapsibleCard(
    title: String,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    isActive: Boolean = false,
    badge: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val backgroundColor = if (isActive) {
        AutoDevColors.Blue.c600.copy(alpha = 0.08f)
    } else {
        JewelTheme.globalColors.panelBackground
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(6.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandChange(!isExpanded) }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "-" else "+",
                    style = JewelTheme.defaultTextStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )

                Text(
                    text = title,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )

                badge?.invoke()
            }
        }

        // Expandable content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Small badge component for status indicators
 */
@Composable
private fun IdeaBadge(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = JewelTheme.defaultTextStyle.copy(
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
