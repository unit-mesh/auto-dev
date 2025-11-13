package cc.unitmesh.devins.ui.compose.agent.codereview.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.devins.ui.compose.agent.codereview.*
import cc.unitmesh.devins.workspace.DefaultWorkspace
import cc.unitmesh.devins.workspace.Workspace

/**
 * Demo application for Code Review UI
 *
 * Usage:
 * ./gradlew :mpp-ui:runCodeReviewDemo
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Code Review Demo - AutoDev",
        state = rememberWindowState(width = 1400.dp, height = 900.dp)
    ) {
        MaterialTheme {
            CodeReviewDemoApp()
        }
    }
}

@Composable
fun CodeReviewDemoApp() {
    var projectPath by remember { mutableStateOf("/Volumes/source/ai/autocrud") }
    var workspace: Workspace? by remember { mutableStateOf(null) }
    var viewModel: JvmCodeReviewViewModel? by remember { mutableStateOf(null) }
    var isInitialized by remember { mutableStateOf(false) }

    // Initialize workspace and viewModel
    LaunchedEffect(projectPath) {
        if (projectPath.isNotEmpty()) {
            println("=" * 60)
            println("🚀 Initializing Code Review Demo")
            println("📁 Project Path: $projectPath")
            println("=" * 60)

            try {
                val ws = DefaultWorkspace.create("Demo Workspace", projectPath)
                workspace = ws

                val gitService = GitService(projectPath)
                val vm = JvmCodeReviewViewModel(
                    workspace = ws,
                    gitService = gitService,
                    llmService = null,
                    codeReviewAgent = null
                )
                viewModel = vm
                isInitialized = true

                println("✅ Initialization complete")
            } catch (e: Exception) {
                println("❌ Initialization failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Code Review Demo") },
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !isInitialized -> {
                    LoadingScreen()
                }
                viewModel != null -> {
                    CodeReviewDemoContent(
                        viewModel = viewModel!!,
                        onProjectPathChange = { newPath ->
                            projectPath = newPath
                            isInitialized = false
                        }
                    )
                }
                else -> {
                    ErrorScreen("Failed to initialize")
                }
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(viewModel) {
        onDispose {
            viewModel?.dispose()
        }
    }
}

@Composable
private fun CodeReviewDemoContent(
    viewModel: JvmCodeReviewViewModel,
    onProjectPathChange: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar with controls
        Card(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .padding(8.dp),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📊 Demo Controls",
                    style = MaterialTheme.typography.h6
                )

                Divider()

                // Commit history section
                Text(
                    text = "Commits: ${state.commitHistory.size}",
                    style = MaterialTheme.typography.subtitle1
                )

                if (state.commitHistory.isNotEmpty()) {
                    Text(
                        text = "Selected: ${state.selectedCommitIndex + 1}/${state.commitHistory.size}",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Commit navigation buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val newIndex = (state.selectedCommitIndex - 1)
                                    .coerceIn(0, state.commitHistory.size - 1)
                                viewModel.loadDiffForCommit(newIndex)
                            },
                            enabled = state.selectedCommitIndex > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("◀ Prev")
                        }

                        Button(
                            onClick = {
                                val newIndex = (state.selectedCommitIndex + 1)
                                    .coerceIn(0, state.commitHistory.size - 1)
                                viewModel.loadDiffForCommit(newIndex)
                            },
                            enabled = state.selectedCommitIndex < state.commitHistory.size - 1,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Next ▶")
                        }
                    }
                }

                Divider()

                // File stats
                Text(
                    text = "Changed Files: ${state.diffFiles.size}",
                    style = MaterialTheme.typography.subtitle1
                )

                if (state.diffFiles.isNotEmpty()) {
                    Text(
                        text = "Selected: ${state.selectedFileIndex + 1}/${state.diffFiles.size}",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                Divider()

                // Actions
                Button(
                    onClick = { viewModel.startAnalysis() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.diffFiles.isNotEmpty() &&
                              state.aiProgress.stage != AnalysisStage.RUNNING_LINT &&
                              state.aiProgress.stage != AnalysisStage.ANALYZING_LINT &&
                              state.aiProgress.stage != AnalysisStage.GENERATING_FIX
                ) {
                    Text("🤖 Start AI Analysis")
                }

                Button(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔄 Refresh")
                }

                Spacer(modifier = Modifier.weight(1f))

                // Status
                Card(
                    backgroundColor = when {
                        state.isLoading -> MaterialTheme.colors.secondary.copy(alpha = 0.1f)
                        state.error != null -> MaterialTheme.colors.error.copy(alpha = 0.1f)
                        else -> MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = when {
                                state.isLoading -> "⏳ Loading..."
                                state.error != null -> "❌ Error"
                                else -> "✅ Ready"
                            },
                            style = MaterialTheme.typography.body2,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )

                        if (state.error != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.error ?: "",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.error
                            )
                        }
                    }
                }
            }
        }

        // Main content: Code Review UI
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            CodeReviewSideBySideView(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Initializing Code Review Demo...")
        }
    }
}

@Composable
private fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "❌",
                style = MaterialTheme.typography.h3
            )
            Text(
                text = message,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.error
            )
        }
    }
}

private operator fun String.times(n: Int): String = repeat(n)
