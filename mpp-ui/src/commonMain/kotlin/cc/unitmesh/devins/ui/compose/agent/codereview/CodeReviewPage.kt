package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService

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
    modifier: Modifier = Modifier
) {
    val currentWorkspace by WorkspaceManager.workspaceFlow.collectAsState()

    val viewModel = remember(currentWorkspace, llmService) {
        val workspace = currentWorkspace ?: WorkspaceManager.getCurrentOrEmpty()
        CodeReviewViewModel(
            workspace = workspace
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
                onRefresh = { viewModel.refresh() },
                workspace = currentWorkspace
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeReviewTopBar(
    onRefresh: () -> Unit,
    workspace: Workspace?
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Code Review",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                workspace?.let {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = AutoDevComposeIcons.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
