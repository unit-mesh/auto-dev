package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cc.unitmesh.agent.Platform
import cc.unitmesh.devins.ui.compose.agent.AgentTopAppBar
import cc.unitmesh.devins.ui.compose.agent.AgentTopAppBarActions
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.launch

/**
 * Code Review Page - Entry point for the Side-by-Side code review UI (redesigned)
 *
 * This is a full-page view that replaces the chat interface when CODE_REVIEW agent is selected.
 * Features a three-column layout with commit history, diff viewer, and AI review panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeReviewPage(
    llmService: KoogLLMService?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onNotification: (String, String) -> Unit = { _, _ -> }
) {
    val currentWorkspace by WorkspaceManager.workspaceFlow.collectAsState()

    val viewModel = remember(currentWorkspace, llmService) {
        val workspace = currentWorkspace ?: WorkspaceManager.getCurrentOrEmpty()
        CodeReviewViewModel(
            workspace = workspace
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.notificationEvent.collect { (title, message) ->
            onNotification(title, message)
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.dispose()
        }
    }

    val notMobile = (Platform.isAndroid || Platform.isIOS).not()
    if (notMobile) {
        Scaffold(
            topBar = {
                CodeReviewTopBar(
                    onRefresh = { viewModel.refresh() },
                    workspace = currentWorkspace,
                    onBack = onBack,
                    viewModel = viewModel
                )
            },
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CodeReviewSideBySideView(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        CodeReviewSideBySideView(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CodeReviewTopBar(
    onRefresh: () -> Unit,
    workspace: Workspace?,
    onBack: () -> Unit,
    viewModel: CodeReviewViewModel
) {
    var showIssueTrackerDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AgentTopAppBar(
        title = "Code Review",
        subtitle = workspace?.name,
        onBack = onBack,
        actions = {
            AgentTopAppBarActions.SettingsButton(
                onClick = { showIssueTrackerDialog = true }
            )
            AgentTopAppBarActions.RefreshButton(onClick = onRefresh)
        }
    )

    // Issue Tracker Configuration Dialog
    if (showIssueTrackerDialog) {
        var currentConfig by remember {
            mutableStateOf(cc.unitmesh.devins.ui.config.IssueTrackerConfig())
        }
        var autoDetectedRepo by remember {
            mutableStateOf<Pair<String, String>?>(null)
        }

        LaunchedEffect(Unit) {
            currentConfig = cc.unitmesh.devins.ui.config.ConfigManager.getIssueTracker()
            // Try to auto-detect repo from Git
            autoDetectedRepo = viewModel.detectRepositoryFromGit()
        }

        IssueTrackerConfigDialog(
            onDismiss = { showIssueTrackerDialog = false },
            onConfigured = {
                // Reload issue service when configuration changes
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
