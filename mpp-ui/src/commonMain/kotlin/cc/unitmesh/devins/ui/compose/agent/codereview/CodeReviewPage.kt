package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService

/**
 * Code Review Page - Entry point for the Side-by-Side code review UI
 *
 * This is a full-page view that replaces the chat interface when CODE_REVIEW agent is selected
 */
@Composable
fun CodeReviewPage(
    llmService: KoogLLMService?,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentWorkspace by WorkspaceManager.workspaceFlow.collectAsState()

    // Create ViewModel
    val viewModel = remember(currentWorkspace, llmService) {
        val workspace = currentWorkspace ?: WorkspaceManager.getCurrentOrEmpty()
        CodeReviewViewModel(
            workspace = workspace,
            llmService = llmService,
            codeReviewAgent = null // TODO: Initialize CodeReviewAgent if needed
        )
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.dispose()
        }
    }

    Scaffold(
        topBar = {
            CodeReviewTopBar(
                onBack = onBack,
                onRefresh = { viewModel.refresh() },
                workspace = currentWorkspace
            )
        },
        modifier = modifier
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
}

@Composable
private fun CodeReviewTopBar(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    workspace: Workspace?
) {
    TopAppBar(
        title = {
            Column {
                Text("Code Review")
                workspace?.let {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            TextButton(onClick = onRefresh) {
                Text("Refresh", color = MaterialTheme.colors.onPrimary)
            }
        },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary,
        elevation = 4.dp
    )
}
