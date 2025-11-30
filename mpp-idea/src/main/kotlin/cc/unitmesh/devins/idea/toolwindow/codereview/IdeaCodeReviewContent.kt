package cc.unitmesh.devins.idea.toolwindow.codereview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.devins.idea.renderer.sketch.IdeaSketchRenderer
import cc.unitmesh.devins.ui.compose.agent.codereview.AIAnalysisProgress
import cc.unitmesh.devins.ui.compose.agent.codereview.AnalysisStage
import cc.unitmesh.devins.ui.compose.agent.codereview.CommitInfo
import cc.unitmesh.devins.ui.compose.agent.codereview.DiffFileInfo
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import com.intellij.openapi.Disposable
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

/**
 * Main Code Review content composable for IntelliJ IDEA plugin.
 * Uses Jewel UI components for IntelliJ-native look and feel.
 */
@Composable
fun IdeaCodeReviewContent(
    viewModel: IdeaCodeReviewViewModel,
    parentDisposable: Disposable
) {
    val state by viewModel.state.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel: Commit list
        CommitListPanel(
            commits = state.commitHistory,
            selectedIndices = state.selectedCommitIndices,
            isLoading = state.isLoading,
            onCommitSelect = { index ->
                viewModel.selectCommit(index)
            },
            modifier = Modifier.width(280.dp).fillMaxHeight()
        )

        Divider(Orientation.Vertical, modifier = Modifier.fillMaxHeight().width(1.dp))

        // Center panel: Diff viewer
        DiffViewerPanel(
            diffFiles = state.diffFiles,
            selectedFileIndex = state.selectedFileIndex,
            isLoading = state.isLoadingDiff,
            onFileSelect = { index -> viewModel.selectFile(index) },
            modifier = Modifier.weight(1f).fillMaxHeight()
        )

        Divider(Orientation.Vertical, modifier = Modifier.fillMaxHeight().width(1.dp))

        // Right panel: AI Analysis
        AIAnalysisPanel(
            progress = state.aiProgress,
            error = state.error,
            onStartAnalysis = { viewModel.startAnalysis() },
            onCancelAnalysis = { viewModel.cancelAnalysis() },
            parentDisposable = parentDisposable,
            modifier = Modifier.width(350.dp).fillMaxHeight()
        )
    }
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

@Composable
private fun DiffViewerPanel(
    diffFiles: List<DiffFileInfo>,
    selectedFileIndex: Int,
    isLoading: Boolean,
    onFileSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(JewelTheme.globalColors.panelBackground)) {
        // File tabs
        if (diffFiles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                diffFiles.forEachIndexed { index, file ->
                    val isSelected = index == selectedFileIndex
                    val changeIcon = when (file.changeType) {
                        ChangeType.CREATE -> "+"
                        ChangeType.DELETE -> "-"
                        ChangeType.RENAME -> "R"
                        else -> "M"
                    }
                    val changeColor = when (file.changeType) {
                        ChangeType.CREATE -> AutoDevColors.Green.c400
                        ChangeType.DELETE -> AutoDevColors.Red.c400
                        else -> AutoDevColors.Amber.c400
                    }

                    Box(
                        modifier = Modifier
                            .clickable { onFileSelect(index) }
                            .background(
                                if (isSelected) JewelTheme.globalColors.panelBackground.copy(alpha = 0.8f)
                                else JewelTheme.globalColors.panelBackground
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = changeIcon,
                                style = JewelTheme.defaultTextStyle.copy(
                                    color = changeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = file.path.split("/").lastOrNull() ?: file.path,
                                style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp)
                            )
                        }
                    }
                }
            }

            Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))
        }

        // Diff content
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (diffFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Select a commit to view diff",
                    style = JewelTheme.defaultTextStyle.copy(color = JewelTheme.globalColors.text.info)
                )
            }
        } else {
            val selectedFile = diffFiles.getOrNull(selectedFileIndex)
            if (selectedFile != null) {
                DiffContent(file = selectedFile)
            }
        }
    }
}

@Composable
private fun DiffContent(file: DiffFileInfo) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        // File path header
        Text(
            text = file.path,
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hunks
        file.hunks.forEach { hunk ->
            // Hunk header
            Text(
                text = "@@ -${hunk.oldStartLine},${hunk.oldLineCount} +${hunk.newStartLine},${hunk.newLineCount} @@",
                style = JewelTheme.defaultTextStyle.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = AutoDevColors.Blue.c400
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Lines
            hunk.lines.forEach { diffLine ->
                val color = when (diffLine.type) {
                    cc.unitmesh.agent.diff.DiffLineType.ADDED -> AutoDevColors.Green.c400
                    cc.unitmesh.agent.diff.DiffLineType.DELETED -> AutoDevColors.Red.c400
                    else -> JewelTheme.globalColors.text.normal
                }

                Text(
                    text = diffLine.content,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = color
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AIAnalysisPanel(
    progress: AIAnalysisProgress,
    error: String?,
    onStartAnalysis: () -> Unit,
    onCancelAnalysis: () -> Unit,
    parentDisposable: Disposable,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(JewelTheme.globalColors.panelBackground)) {
        // Header with action button
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Analysis",
                style = JewelTheme.defaultTextStyle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )

            when (progress.stage) {
                AnalysisStage.IDLE, AnalysisStage.COMPLETED, AnalysisStage.ERROR -> {
                    DefaultButton(onClick = onStartAnalysis) {
                        Text("Start Analysis")
                    }
                }
                else -> {
                    OutlinedButton(onClick = onCancelAnalysis) {
                        Text("Cancel")
                    }
                }
            }
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Status
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (statusText, statusColor) = when (progress.stage) {
                AnalysisStage.IDLE -> "Ready" to JewelTheme.globalColors.text.info
                AnalysisStage.RUNNING_LINT -> "Running lint..." to AutoDevColors.Amber.c400
                AnalysisStage.ANALYZING_LINT -> "Analyzing code..." to AutoDevColors.Blue.c400
                AnalysisStage.GENERATING_PLAN -> "Generating plan..." to AutoDevColors.Blue.c400
                AnalysisStage.WAITING_FOR_USER_INPUT -> "Waiting for input..." to AutoDevColors.Amber.c400
                AnalysisStage.GENERATING_FIX -> "Generating fixes..." to AutoDevColors.Blue.c400
                AnalysisStage.COMPLETED -> "Completed" to AutoDevColors.Green.c400
                AnalysisStage.ERROR -> "Error" to AutoDevColors.Red.c400
            }

            if (progress.stage != AnalysisStage.IDLE &&
                progress.stage != AnalysisStage.COMPLETED &&
                progress.stage != AnalysisStage.ERROR) {
                CircularProgressIndicator()
            }

            Text(
                text = statusText,
                style = JewelTheme.defaultTextStyle.copy(color = statusColor)
            )
        }

        // Error message
        if (error != null) {
            Text(
                text = error,
                style = JewelTheme.defaultTextStyle.copy(
                    color = AutoDevColors.Red.c400,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Analysis output - use IdeaSketchRenderer for rich markdown/code rendering
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            if (progress.analysisOutput.isNotEmpty()) {
                val isComplete = progress.stage == AnalysisStage.COMPLETED ||
                                 progress.stage == AnalysisStage.ERROR
                IdeaSketchRenderer.RenderResponse(
                    content = progress.analysisOutput,
                    isComplete = isComplete,
                    parentDisposable = parentDisposable,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Click 'Start Analysis' to begin AI code review",
                    style = JewelTheme.defaultTextStyle.copy(
                        color = JewelTheme.globalColors.text.info,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

