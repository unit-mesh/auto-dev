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
    var viewModel: CodeReviewViewModel? by remember { mutableStateOf(null) }
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

                val vm = CodeReviewViewModel(
                    workspace = ws,
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
                        viewModel = viewModel!!
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
private fun CodeReviewDemoContent(viewModel: CodeReviewViewModel) {
    val state by viewModel.state.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
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
