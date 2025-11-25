package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer
import cc.unitmesh.devins.ui.compose.agent.ResizableSplitPane
import cc.unitmesh.devins.ui.compose.agent.codereview.diff.CommitListView
import cc.unitmesh.devins.ui.compose.agent.codereview.diff.DiffCenterView
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.devins.ui.wasm.WasmGitCloneScreen
import kotlinx.coroutines.launch

/**
 * Main Side-by-Side Code Review UI (redesigned)
 *
 * Three-column layout using ResizableSplitPane:
 * - Left: Commit history list (like GitHub commits view)
 * - Center: Diff viewer with collapsible file changes (using DiffSketchRenderer)
 * - Right: AI code review messages (using AgentMessageList)
 */
@Composable
fun CodeReviewSideBySideView(viewModel: CodeReviewViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    var showConfigDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

            state.commitHistory.isEmpty() -> {
                if (Platform.name == "WebAssembly") {
                    WasmGitCloneScreen(
                        onCommitsFetched = {
                            viewModel.refresh()
                        }
                    )
                } else {
                    EmptyCommitView(
                        onLoadDiff = { viewModel.refresh() }
                    )
                }
            }

            else -> {
                ThreeColumnLayout(
                    state = state,
                    viewModel = viewModel,
                    onShowConfigDialog = { showConfigDialog = true }
                )
            }
        }
    }

    // Issue Tracker Configuration Dialog
    if (showConfigDialog) {
        var currentConfig by remember {
            mutableStateOf(cc.unitmesh.devins.ui.config.IssueTrackerConfig())
        }
        var autoDetectedRepo by remember {
            mutableStateOf<Pair<String, String>?>(null)
        }

        LaunchedEffect(Unit) {
            currentConfig = cc.unitmesh.devins.ui.config.ConfigManager.getIssueTracker()
            autoDetectedRepo = viewModel.detectRepositoryFromGit()
        }

        IssueTrackerConfigDialog(
            onDismiss = { showConfigDialog = false },
            onConfigured = {
                scope.launch {
                    try {
                        viewModel.reloadIssueService()
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            },
            initialConfig = currentConfig,
            autoDetectedRepo = autoDetectedRepo
        )
    }
}

/**
 * Three-column layout with ResizableSplitPane
 */
@Composable
private fun ThreeColumnLayout(
    state: CodeReviewState,
    viewModel: CodeReviewViewModel,
    onShowConfigDialog: () -> Unit
) {
    val renderer = remember { ComposeRenderer() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    ResizableSplitPane(
        modifier = Modifier.fillMaxSize(),
        initialSplitRatio = 0.18f,
        minRatio = 0.12f,
        maxRatio = 0.35f,
        saveKey = "code_review_outer_split",
        first = {
            // Left: Commit history list
            CommitListView(
                commits = state.commitHistory,
                selectedIndices = state.selectedCommitIndices,
                onCommitSelected = { index, isToggle ->
                    viewModel.selectCommit(index, toggle = isToggle)
                },
                hasMoreCommits = state.hasMoreCommits,
                isLoadingMore = state.isLoadingMore,
                totalCommitCount = state.totalCommitCount,
                onLoadMore = {
                    scope.launch {
                        viewModel.loadMoreCommits()
                    }
                }
            )
        },
        second = {
            // Center + Right: Diff view and AI messages
            ResizableSplitPane(
                modifier = Modifier.fillMaxSize(),
                initialSplitRatio = 0.35f,
                minRatio = 0.25f,
                maxRatio = 0.65f,
                saveKey = "code_review_inner_split",
                first = {
                    // Center: Diff viewer
                    var fileToView by remember { mutableStateOf<String?>(null) }
                    var lineRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
                    val onViewFile: (String) -> Unit = { filePath ->
                        fileToView = filePath
                        lineRange = null
                    }
                    val onViewFileWithLines: (String, Int, Int) -> Unit = { filePath, startLine, endLine ->
                        fileToView = filePath
                        lineRange = startLine to endLine
                    }
                    val workspaceRoot = viewModel.workspace.rootPath
                    val onConfigureToken = onShowConfigDialog

                    DiffCenterView(
                        diffFiles = state.diffFiles,
                        selectedCommits = state.selectedCommitIndices.mapNotNull { state.commitHistory.getOrNull(it) },
                        onViewFile = onViewFile,
                        onViewFileWithLines = onViewFileWithLines,
                        workspaceRoot = workspaceRoot,
                        isLoadingDiff = state.isLoadingDiff,
                        onConfigureToken = onConfigureToken,
                        relatedTests = state.relatedTests,
                        isLoadingTests = state.isLoadingTests
                    )

                    // File viewer dialog
                    fileToView?.let { path ->
                        FileViewerDialog(
                            filePath = path,
                            onClose = {
                                fileToView = null
                                lineRange = null
                            },
                            startLine = lineRange?.first,
                            endLine = lineRange?.second
                        )
                    }
                },
                second = {
                    CodeReviewAgentPanel(
                        state = state,
                        viewModel = viewModel,
                        renderer = renderer
                    )
                }
            )
        }
    )
}


@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = AutoDevColors.Indigo.c600
            )
            Text(
                text = "Loading commit history...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Error,
                contentDescription = "Error",
                tint = AutoDevColors.Red.c600,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            FilledTonalButton(onClick = onRetry) {
                Icon(
                    imageVector = AutoDevComposeIcons.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyCommitView(onLoadDiff: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = AutoDevComposeIcons.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No commits available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Make sure you have commits in your repository",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            FilledTonalButton(onClick = onLoadDiff) {
                Icon(
                    imageVector = AutoDevComposeIcons.Refresh,
                    contentDescription = "Load",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load Commits")
            }
        }
    }
}
