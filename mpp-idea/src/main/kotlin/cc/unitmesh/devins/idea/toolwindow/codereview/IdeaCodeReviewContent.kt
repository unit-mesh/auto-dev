package cc.unitmesh.devins.idea.toolwindow.codereview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cc.unitmesh.devins.idea.compose.IdeaLaunchedEffect
import cc.unitmesh.devins.idea.components.IdeaResizableSplitPane
import cc.unitmesh.devins.ui.compose.agent.codereview.CodeReviewState
import com.intellij.openapi.Disposable

/**
 * Main Code Review content composable for IntelliJ IDEA plugin.
 * Uses Jewel UI components for IntelliJ-native look and feel.
 *
 * Features a three-column resizable layout:
 * - Left: Commit history list
 * - Center: Diff viewer with file tabs
 * - Right: AI Analysis with Plan, User Input, and Fix generation sections
 *
 * Note: Uses IdeaLaunchedEffect-based manual collection instead of collectAsState()
 * to avoid ClassLoader conflicts between plugin's coroutines and IntelliJ's
 * bundled Compose runtime.
 */
@Composable
fun IdeaCodeReviewContent(
    viewModel: IdeaCodeReviewViewModel,
    parentDisposable: Disposable
) {
    // Use manual state collection to avoid ClassLoader conflicts with collectAsState()
    var state by remember { mutableStateOf(CodeReviewState()) }
    IdeaLaunchedEffect(viewModel) {
        viewModel.state.collect { state = it }
    }

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
