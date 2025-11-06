package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService

@Composable
fun AgentChatInterface(
    llmService: KoogLLMService?,
    onConfigWarning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentWorkspace by WorkspaceManager.workspaceFlow.collectAsState()
    val viewModel =
        remember(llmService, currentWorkspace?.rootPath) {
            val workspace = currentWorkspace
            val rootPath = workspace?.rootPath ?: return@remember null
            if (llmService != null) {
                CodingAgentViewModel(
                    llmService = llmService,
                    projectPath = rootPath,
                    maxIterations = 100
                )
            } else {
                null
            }
        }

    if (viewModel == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (viewModel.isExecuting || viewModel.renderer.currentIteration > 0) {
            AgentStatusBar(
                isExecuting = viewModel.isExecuting,
                currentIteration = viewModel.renderer.currentIteration,
                maxIterations = viewModel.renderer.maxIterations,
                executionTime = viewModel.renderer.currentExecutionTime,
                viewModel = viewModel,
                onCancel = { viewModel.cancelTask() }
            )
        }

        // Main content area with optional file viewer panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Message list (takes full width if no file viewer, or left side if viewer is open)
            AgentMessageList(
                renderer = viewModel.renderer,
                modifier = Modifier
                    .weight(if (viewModel.renderer.currentViewingFile != null) 0.5f else 1f)
                    .fillMaxHeight(),
                onOpenFileViewer = { filePath ->
                    viewModel.renderer.openFileViewer(filePath)
                }
            )
            
            // File viewer panel (only show on JVM when a file is selected)
            viewModel.renderer.currentViewingFile?.let { filePath ->
                FileViewerPanelWrapper(
                    filePath = filePath,
                    onClose = { viewModel.renderer.closeFileViewer() },
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                )
            }
        }

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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        ToolLoadingStatusBar(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AgentStatusBar(
    isExecuting: Boolean,
    currentIteration: Int,
    maxIterations: Int,
    executionTime: Long,
    viewModel: CodingAgentViewModel,
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy All button
                if (!isExecuting) {
                    CopyAllButton(viewModel = viewModel)
                }

                // Stop button
                if (isExecuting) {
                    Button(
                        onClick = onCancel,
                        colors =
                            ButtonDefaults.buttonColors(
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
}

@Composable
private fun CopyAllButton(viewModel: CodingAgentViewModel) {
    val clipboardManager = LocalClipboardManager.current

    OutlinedButton(
        onClick = {
            val allText =
                buildString {
                    viewModel.renderer.timeline.forEach { item ->
                        when (item) {
                            is ComposeRenderer.TimelineItem.MessageItem -> {
                                val role = if (item.message.role == MessageRole.USER) "User" else "Assistant"
                                appendLine("[$role]: ${item.message.content}")
                                appendLine()
                            }

                            is ComposeRenderer.TimelineItem.ToolCallItem -> {
                                appendLine("[Tool Call]: ${item.toolName}")
                                appendLine("Description: ${item.description}")
                                item.details?.let { appendLine("Parameters: $it") }
                                appendLine()
                            }

                            is ComposeRenderer.TimelineItem.ToolResultItem -> {
                                val status = if (item.success) "SUCCESS" else "FAILED"
                                appendLine("[Tool Result]: ${item.toolName} - $status")
                                appendLine("Summary: ${item.summary}")
                                item.output?.let { appendLine("Output: $it") }
                                appendLine()
                            }

                            is ComposeRenderer.TimelineItem.ErrorItem -> {
                                appendLine("[Error]: ${item.error}")
                                appendLine()
                            }

                            is ComposeRenderer.TimelineItem.TaskCompleteItem -> {
                                val status = if (item.success) "COMPLETED" else "FAILED"
                                appendLine("[Task $status]: ${item.message}")
                                appendLine()
                            }
                        }
                    }

                    if (viewModel.renderer.currentStreamingOutput.isNotEmpty()) {
                        appendLine("[Assistant - Streaming]: ${viewModel.renderer.currentStreamingOutput}")
                    }
                }
            clipboardManager.setText(AnnotatedString(allText))
        }
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy all",
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Copy All")
    }
}

private fun formatExecutionTime(timeMs: Long): String {
    val seconds = timeMs / 1000
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}

@Composable
private fun ToolLoadingStatusBar(
    viewModel: CodingAgentViewModel,
    modifier: Modifier = Modifier
) {
    // 直接观察状态变化，不使用 derivedStateOf
    val mcpPreloadingStatus = viewModel.mcpPreloadingStatus
    val mcpPreloadingMessage = viewModel.mcpPreloadingMessage
    val toolStatus by remember(mcpPreloadingStatus) {
        derivedStateOf { viewModel.getToolLoadingStatus() }
    }

    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Built-in Tools Status
            ToolStatusChip(
                label = "Built-in",
                count = toolStatus.builtinToolsEnabled,
                total = toolStatus.builtinToolsTotal,
                isLoading = false,
                color = MaterialTheme.colorScheme.primary,
                tooltip = "Core tools: read-file, write-file, grep, glob, shell"
            )

            // SubAgents Status
            ToolStatusChip(
                label = "SubAgents",
                count = toolStatus.subAgentsEnabled,
                total = toolStatus.subAgentsTotal,
                isLoading = false,
                color = MaterialTheme.colorScheme.secondary,
                tooltip = "AI agents: error-recovery, log-summary, codebase-investigator"
            )

            // MCP Tools Status (async)
            ToolStatusChip(
                label = "MCP Tools",
                count = toolStatus.mcpToolsEnabled,
                total = if (toolStatus.isLoading) "∞" else toolStatus.mcpToolsEnabled.toString(),
                isLoading = toolStatus.isLoading,
                color = if (!toolStatus.isLoading && toolStatus.mcpToolsEnabled > 0) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                tooltip = "External tools from MCP servers (${toolStatus.mcpServersLoaded}/${toolStatus.mcpServersTotal} servers)"
            )

            Spacer(modifier = Modifier.weight(1f))

            // Status message with icon
            if (mcpPreloadingMessage.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (toolStatus.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = mcpPreloadingMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            } else if (!toolStatus.isLoading && toolStatus.mcpServersLoaded > 0) {
                Text(
                    text = "✓ All tools ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ToolStatusChip(
    label: String,
    count: Int,
    total: Any, // Can be Int or String
    isLoading: Boolean,
    color: androidx.compose.ui.graphics.Color,
    tooltip: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Status indicator with better visual feedback
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = if (isLoading) MaterialTheme.colorScheme.outline.copy(alpha = 0.6f) else color,
                    shape = CircleShape
                )
        ) {
            // Add a subtle inner glow for loaded tools
            if (!isLoading && count > 0) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.Center)
                        .background(
                            color = color.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }

        // Label and count with better typography
        Text(
            text = "$label ($count/$total)",
            style = MaterialTheme.typography.labelMedium,
            color = if (isLoading) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        // Loading indicator - smaller and more subtle
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}


