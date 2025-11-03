package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService

/**
 * Agent Chat Interface
 * Uses the new ComposeRenderer architecture for consistent rendering
 */
@Composable
fun AgentChatInterface(
    llmService: KoogLLMService?,
    onConfigWarning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentWorkspace by WorkspaceManager.workspaceFlow.collectAsState()
    
    // Create ViewModel with current workspace
    val viewModel = remember(llmService, currentWorkspace?.rootPath) {
        val workspace = currentWorkspace
        val rootPath = workspace?.rootPath
        if (llmService != null && workspace != null && rootPath != null) {
            CodingAgentViewModel(
                llmService = llmService,
                projectPath = rootPath,
                maxIterations = 100
            )
        } else null
    }
    
    if (viewModel == null) {
        // Show configuration prompt
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⚠️ Configuration Required",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Please configure your LLM model and select a workspace to use the Coding Agent.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onConfigWarning) {
                        Text("Configure Now")
                    }
                }
            }
        }
        return
    }
    
    // Main agent interface
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Agent status bar
        if (viewModel.isExecuting || viewModel.renderer.currentIteration > 0) {
            AgentStatusBar(
                isExecuting = viewModel.isExecuting,
                currentIteration = viewModel.renderer.currentIteration,
                maxIterations = viewModel.renderer.maxIterations,
                executionTime = viewModel.renderer.currentExecutionTime,
                onCancel = { viewModel.cancelTask() }
            )
        }
        
        // Messages and tool calls display
        AgentMessageList(
            renderer = viewModel.renderer,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        
        // Input area
        val callbacks = remember(viewModel) {
            createAgentCallbacks(
                viewModel = viewModel,
                onConfigWarning = onConfigWarning
            )
        }
        
        DevInEditorInput(
            initialText = "",
            placeholder = "Describe your coding task...",
            callbacks = callbacks,
            completionManager = currentWorkspace?.completionManager,
            isCompactMode = true,
            onModelConfigChange = { /* Handle model config change if needed */ },
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AgentStatusBar(
    isExecuting: Boolean,
    currentIteration: Int,
    maxIterations: Int,
    executionTime: Long,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
                Column {
                    Text(
                        text = if (isExecuting) "Executing..." else "Ready",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentIteration > 0) {
                            Text(
                                text = "($currentIteration/$maxIterations)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        if (executionTime > 0) {
                            Text(
                                text = "• ${formatExecutionTime(executionTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            if (isExecuting) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
            }
        }
    }
}

// Helper function to format execution time
private fun formatExecutionTime(timeMs: Long): String {
    val seconds = timeMs / 1000
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
