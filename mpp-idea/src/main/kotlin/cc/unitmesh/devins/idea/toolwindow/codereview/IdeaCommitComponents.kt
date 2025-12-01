package cc.unitmesh.devins.idea.toolwindow.codereview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.devins.idea.toolwindow.IdeaComposeIcons
import cc.unitmesh.devins.ui.compose.agent.codereview.CommitInfo
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

/**
 * Commit list panel showing all commits with selection support
 */
@Composable
internal fun CommitListPanel(
    commits: List<CommitInfo>,
    selectedIndices: Set<Int>,
    isLoading: Boolean,
    onCommitSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(JewelTheme.globalColors.panelBackground)) {
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

/**
 * Single commit item in the list
 */
@Composable
internal fun CommitItem(
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
 * Commit info card with issue display
 */
@Composable
internal fun IdeaCommitInfoCard(
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
                SingleCommitInfoView(
                    selectedCommit = selectedCommit,
                    actualCommitIndex = selectedCommitIndices.firstOrNull() ?: 0,
                    onRefreshIssue = onRefreshIssue,
                    onConfigureToken = onConfigureToken
                )
            } else {
                MultipleCommitsInfoView(selectedCommits = selectedCommits)
            }
        }
    }
}

@Composable
private fun SingleCommitInfoView(
    selectedCommit: CommitInfo,
    actualCommitIndex: Int,
    onRefreshIssue: ((Int) -> Unit)?,
    onConfigureToken: () -> Unit
) {
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

    selectedCommit.issueInfo?.let { issueInfo ->
        Spacer(modifier = Modifier.height(8.dp))
        IdeaIssueInfoCard(issueInfo = issueInfo)
    }
}

@Composable
private fun MultipleCommitsInfoView(selectedCommits: List<CommitInfo>) {
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

/**
 * Issue indicator for commit (loading, info chip, error with retry)
 */
@Composable
internal fun IdeaIssueIndicator(
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
            IssueInfoIndicator(
                commit = commit,
                commitIndex = commitIndex,
                onRefreshIssue = onRefreshIssue
            )
        }
        commit.issueLoadError != null -> {
            IssueErrorIndicator(
                errorMessage = commit.issueLoadError!!,
                commitIndex = commitIndex,
                onRefreshIssue = onRefreshIssue,
                onConfigureToken = onConfigureToken
            )
        }
    }
}

@Composable
private fun IssueInfoIndicator(
    commit: CommitInfo,
    commitIndex: Int,
    onRefreshIssue: ((Int) -> Unit)?
) {
    val issueInfo = commit.issueInfo!!
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IdeaInlineIssueChip(issueInfo = issueInfo)

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

        if (onRefreshIssue != null) {
            IconButton(
                onClick = { onRefreshIssue(commitIndex) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.Refresh,
                    contentDescription = "Refresh issue",
                    tint = JewelTheme.globalColors.text.info.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun IssueErrorIndicator(
    errorMessage: String,
    commitIndex: Int,
    onRefreshIssue: ((Int) -> Unit)?,
    onConfigureToken: () -> Unit
) {
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

        if (onRefreshIssue != null) {
            IconButton(
                onClick = { onRefreshIssue(commitIndex) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = IdeaComposeIcons.Refresh,
                    contentDescription = "Retry",
                    tint = AutoDevColors.Red.c400.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

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
                        imageVector = IdeaComposeIcons.Settings,
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

/**
 * Inline compact issue chip
 */
@Composable
internal fun IdeaInlineIssueChip(issueInfo: cc.unitmesh.agent.tracker.IssueInfo) {
    val (bgColor, iconVector, textColor) = when (issueInfo.status.lowercase()) {
        "open" -> Triple(
            AutoDevColors.Green.c600.copy(alpha = 0.15f),
            IdeaComposeIcons.BugReport,
            AutoDevColors.Green.c600
        )
        "closed" -> Triple(
            AutoDevColors.Neutral.c600.copy(alpha = 0.15f),
            IdeaComposeIcons.CheckCircle,
            AutoDevColors.Neutral.c600
        )
        else -> Triple(
            AutoDevColors.Indigo.c600.copy(alpha = 0.15f),
            IdeaComposeIcons.Info,
            AutoDevColors.Indigo.c600
        )
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = issueInfo.status,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "#${issueInfo.id}",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
        }
    }
}

/**
 * Issue info card with full details
 */
@Composable
internal fun IdeaIssueInfoCard(issueInfo: cc.unitmesh.agent.tracker.IssueInfo) {
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
            IssueInfoHeader(issueInfo = issueInfo)

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

            if (issueInfo.labels.isNotEmpty()) {
                IssueLabelsRow(labels = issueInfo.labels)
            }
        }
    }
}

@Composable
private fun IssueInfoHeader(issueInfo: cc.unitmesh.agent.tracker.IssueInfo) {
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
                imageVector = IdeaComposeIcons.BugReport,
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

        val (statusBgColor, statusTextColor) = when (issueInfo.status.lowercase()) {
            "open" -> AutoDevColors.Green.c600.copy(alpha = 0.2f) to AutoDevColors.Green.c600
            "closed" -> AutoDevColors.Red.c600.copy(alpha = 0.2f) to AutoDevColors.Red.c600
            else -> JewelTheme.globalColors.panelBackground to JewelTheme.globalColors.text.info
        }

        Box(
            modifier = Modifier
                .background(statusBgColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = issueInfo.status,
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 10.sp,
                    color = statusTextColor
                )
            )
        }
    }
}

@Composable
private fun IssueLabelsRow(labels: List<String>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        labels.take(5).forEach { label ->
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
        if (labels.size > 5) {
            Text(
                text = "+${labels.size - 5}",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = 10.sp,
                    color = JewelTheme.globalColors.text.info
                )
            )
        }
    }
}

