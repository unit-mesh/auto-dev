package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Main Side-by-Side Code Review UI
 *
 * Left: Original diff view
 * Right: AI analysis & fix flow
 */
@Composable
fun CodeReviewSideBySideView(
    viewModel: CodeReviewViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                LoadingView()
            }
            state.error != null -> {
                ErrorView(
                    error = state.error!!,
                    onRetry = { viewModel.refresh() }
                )
            }
            state.diffFiles.isEmpty() -> {
                EmptyDiffView(
                    onLoadDiff = { viewModel.refresh() }
                )
            }
            else -> {
                SideBySideContent(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun SideBySideContent(
    state: CodeReviewState,
    viewModel: CodeReviewViewModel
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left side: Diff view
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colors.surface)
                .padding(8.dp)
        ) {
            DiffView(
                files = state.diffFiles,
                selectedIndex = state.selectedFileIndex,
                onSelectFile = { index -> viewModel.selectFile(index) }
            )
        }

        // Divider
        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        )

        // Right side: AI analysis flow
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colors.surface)
                .padding(8.dp)
        ) {
            AIAnalysisView(
                progress = state.aiProgress,
                fixResults = state.fixResults,
                onStartAnalysis = { viewModel.startAnalysis() },
                onCancelAnalysis = { viewModel.cancelAnalysis() }
            )
        }
    }
}

/**
 * Left side: Diff viewer with file tree
 */
@Composable
private fun DiffView(
    files: List<DiffFileInfo>,
    selectedIndex: Int,
    onSelectFile: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            text = "📄 Code Changes",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // File list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(files.size) { index ->
                DiffFileItem(
                    file = files[index],
                    isSelected = index == selectedIndex,
                    onClick = { onSelectFile(index) }
                )
            }
        }
    }
}

@Composable
private fun DiffFileItem(
    file: DiffFileInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = if (isSelected) 4.dp else 1.dp,
        backgroundColor = if (isSelected) {
            MaterialTheme.colors.primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colors.surface
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Change type icon
                Icon(
                    imageVector = when (file.changeType) {
                        ChangeType.ADDED -> Icons.Default.Add
                        ChangeType.DELETED -> Icons.Default.Delete
                        ChangeType.MODIFIED -> Icons.Default.Edit
                        ChangeType.RENAMED -> Icons.Default.DriveFileRenameOutline
                    },
                    contentDescription = file.changeType.name,
                    tint = when (file.changeType) {
                        ChangeType.ADDED -> Color(0xFF4CAF50)
                        ChangeType.DELETED -> Color(0xFFF44336)
                        ChangeType.MODIFIED -> Color(0xFF2196F3)
                        ChangeType.RENAMED -> Color(0xFFFF9800)
                    },
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // File path
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.body2,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )

                // Language badge
                file.language?.let { lang ->
                    Text(
                        text = lang,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(
                                MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    // Expandable diff content (for future implementation)
    if (isSelected && file.hunks.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        DiffHunksView(file.hunks)
    }
}

@Composable
private fun DiffHunksView(hunks: List<DiffHunk>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        hunks.forEach { hunk ->
            DiffHunkView(hunk)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DiffHunkView(hunk: DiffHunk) {
    Column {
        // Hunk header
        Text(
            text = "@@ -${hunk.oldStart},${hunk.oldLines} +${hunk.newStart},${hunk.newLines} @@",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFF888888),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Lines
        hunk.lines.forEach { line ->
            DiffLineView(line)
        }
    }
}

@Composable
private fun DiffLineView(line: DiffLine) {
    val backgroundColor = when (line.type) {
        DiffLineType.ADDED -> Color(0xFF1B4D2E)
        DiffLineType.DELETED -> Color(0xFF5C1F1F)
        DiffLineType.CONTEXT -> Color.Transparent
    }

    val textColor = when (line.type) {
        DiffLineType.ADDED -> Color(0xFF4CAF50)
        DiffLineType.DELETED -> Color(0xFFF44336)
        DiffLineType.CONTEXT -> Color(0xFFCCCCCC)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Line numbers
        Text(
            text = "${line.oldLineNumber ?: ""}",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = "${line.newLineNumber ?: ""}",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.width(40.dp)
        )

        // Content
        Text(
            text = line.content,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Right side: AI analysis and fix flow
 */
@Composable
private fun AIAnalysisView(
    progress: AIAnalysisProgress,
    fixResults: List<FixResult>,
    onStartAnalysis: () -> Unit,
    onCancelAnalysis: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🤖 AI Analysis & Auto-Fix",
                style = MaterialTheme.typography.h6
            )

            // Control buttons
            when (progress.stage) {
                AnalysisStage.IDLE -> {
                    Button(onClick = onStartAnalysis) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Analysis")
                    }
                }
                AnalysisStage.RUNNING_LINT,
                AnalysisStage.ANALYZING_LINT,
                AnalysisStage.GENERATING_FIX -> {
                    Button(onClick = onCancelAnalysis, colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.error
                    )) {
                        Icon(Icons.Default.Stop, contentDescription = "Cancel")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
                AnalysisStage.COMPLETED -> {
                    Row {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Completed", color = Color(0xFF4CAF50))
                    }
                }
                AnalysisStage.ERROR -> {
                    Row {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colors.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Error", color = MaterialTheme.colors.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress stages
        ProgressStagesView(progress)

        Spacer(modifier = Modifier.height(16.dp))

        // Output sections
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Lint output
            if (progress.lintOutput.isNotEmpty()) {
                item {
                    OutputSection(
                        title = "1️⃣ Lint Analysis",
                        content = progress.lintOutput,
                        isActive = progress.stage == AnalysisStage.RUNNING_LINT
                    )
                }
            }

            // AI analysis
            if (progress.analysisOutput.isNotEmpty()) {
                item {
                    OutputSection(
                        title = "2️⃣ AI Analysis",
                        content = progress.analysisOutput,
                        isActive = progress.stage == AnalysisStage.ANALYZING_LINT
                    )
                }
            }

            // Fix results
            if (fixResults.isNotEmpty()) {
                item {
                    Text(
                        text = "3️⃣ Auto-Fix Results (${fixResults.size} fixes)",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(fixResults) { fix ->
                    FixResultCard(fix)
                }
            }
        }
    }
}

@Composable
private fun ProgressStagesView(progress: AIAnalysisProgress) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StageIndicator("Lint", progress.stage >= AnalysisStage.RUNNING_LINT)
        Text("→", modifier = Modifier.padding(horizontal = 4.dp))
        StageIndicator("Analyze", progress.stage >= AnalysisStage.ANALYZING_LINT)
        Text("→", modifier = Modifier.padding(horizontal = 4.dp))
        StageIndicator("Fix", progress.stage >= AnalysisStage.GENERATING_FIX)
    }
}

@Composable
private fun StageIndicator(label: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isActive) MaterialTheme.colors.primary else Color.Gray,
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = label,
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = if (isActive) MaterialTheme.colors.primary else Color.Gray
        )
    }
}

@Composable
private fun OutputSection(
    title: String,
    content: String,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = if (isActive) {
            MaterialTheme.colors.primary.copy(alpha = 0.05f)
        } else {
            MaterialTheme.colors.surface
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun FixResultCard(fix: FixResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = when (fix.status) {
            FixStatus.FIXED -> Color(0xFF1B4D2E)
            FixStatus.NO_ISSUE -> Color(0xFF2E4D1B)
            FixStatus.SKIPPED -> Color(0xFF4D4D1B)
            FixStatus.FAILED -> Color(0xFF5C1F1F)
        }.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status icon
                    Icon(
                        imageVector = when (fix.status) {
                            FixStatus.FIXED -> Icons.Default.CheckCircle
                            FixStatus.NO_ISSUE -> Icons.Default.Info
                            FixStatus.SKIPPED -> Icons.Default.Warning
                            FixStatus.FAILED -> Icons.Default.Error
                        },
                        contentDescription = fix.status.name,
                        tint = when (fix.status) {
                            FixStatus.FIXED -> Color(0xFF4CAF50)
                            FixStatus.NO_ISSUE -> Color(0xFF2196F3)
                            FixStatus.SKIPPED -> Color(0xFFFF9800)
                            FixStatus.FAILED -> Color(0xFFF44336)
                        },
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${fix.filePath}:${fix.line}",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Risk badge
                Text(
                    text = fix.risk.name,
                    style = MaterialTheme.typography.caption,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            when (fix.risk) {
                                RiskLevel.CRITICAL -> Color(0xFFD32F2F)
                                RiskLevel.HIGH -> Color(0xFFF57C00)
                                RiskLevel.MEDIUM -> Color(0xFFFBC02D)
                                RiskLevel.LOW -> Color(0xFF388E3C)
                                RiskLevel.INFO -> Color(0xFF1976D2)
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Issue
            Text(
                text = "Issue: ${fix.lintIssue}",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.error
            )

            Spacer(modifier = Modifier.height(4.dp))

            // AI fix description
            Text(
                text = "Fix: ${fix.aiFix}",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Fixed code
            fix.fixedCode?.let { code ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
            }
        }
    }
}

// Supporting views

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading git diff...")
        }
    }
}

@Composable
private fun ErrorView(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colors.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyDiffView(onLoadDiff: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "📄",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No diff available",
                style = MaterialTheme.typography.h6
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Make some changes or specify a commit to review",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onLoadDiff) {
                Icon(Icons.Default.Refresh, contentDescription = "Load")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Load Diff")
            }
        }
    }
}
