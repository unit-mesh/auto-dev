package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService

@Composable
fun AgentChatInterface(
    llmService: KoogLLMService?,
    isTreeViewVisible: Boolean = false,
    onConfigWarning: () -> Unit,
    onToggleTreeView: (Boolean) -> Unit = {},
    // TopBar 参数
    hasHistory: Boolean = false,
    hasDebugInfo: Boolean = false,
    currentModelConfig: cc.unitmesh.llm.ModelConfig? = null,
    selectedAgent: String = "Default",
    availableAgents: List<String> = listOf("Default"),
    useAgentMode: Boolean = true,
    onOpenDirectory: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onShowDebug: () -> Unit = {},
    onModelConfigChange: (cc.unitmesh.llm.ModelConfig) -> Unit = {},
    onAgentChange: (String) -> Unit = {},
    onModeToggle: () -> Unit = {},
    onShowModelConfig: () -> Unit = {},
    onShowToolConfig: () -> Unit = {},
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

    // 同步外部 TreeView 状态到 ViewModel
    LaunchedEffect(isTreeViewVisible) {
        viewModel?.let {
            if (it.isTreeViewVisible != isTreeViewVisible) {
                it.isTreeViewVisible = isTreeViewVisible
            }
        }
    }

    // 监听 ViewModel 状态变化并通知外部
    LaunchedEffect(viewModel?.isTreeViewVisible) {
        viewModel?.let {
            onToggleTreeView(it.isTreeViewVisible)
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

    // 使用 ResizableSplitPane 分割 Chat 区域和 TreeView 区域
    if (viewModel.isTreeViewVisible) {
        ResizableSplitPane(
            modifier = modifier.fillMaxSize(),
            initialSplitRatio = 0.6f,
            minRatio = 0.3f,
            maxRatio = 0.8f,
            first = {
                // 左侧：TopBar + Chat + Input 完整区域
                Column(modifier = Modifier.fillMaxSize()) {
                    // TopBar 放在左侧列顶部
                    cc.unitmesh.devins.ui.compose.chat.TopBarMenu(
                        hasHistory = hasHistory,
                        hasDebugInfo = hasDebugInfo,
                        currentModelConfig = currentModelConfig,
                        selectedAgent = selectedAgent,
                        availableAgents = availableAgents,
                        useAgentMode = useAgentMode,
                        isTreeViewVisible = isTreeViewVisible,
                        onOpenDirectory = onOpenDirectory,
                        onClearHistory = onClearHistory,
                        onShowDebug = onShowDebug,
                        onModelConfigChange = onModelConfigChange,
                        onAgentChange = onAgentChange,
                        onModeToggle = onModeToggle,
                        onToggleTreeView = { onToggleTreeView(!isTreeViewVisible) },
                        onShowModelConfig = onShowModelConfig,
                        onShowToolConfig = onShowToolConfig,
                        modifier = Modifier.statusBarsPadding()
                    )

                    // Chat 消息列表
                    AgentMessageList(
                        renderer = viewModel.renderer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        onOpenFileViewer = { filePath ->
                            viewModel.renderer.openFileViewer(filePath)
                        }
                    )

                    val callbacks = remember(viewModel) {
                        createAgentCallbacks(
                            viewModel = viewModel,
                            onConfigWarning = onConfigWarning
                        )
                    }

                    // 输入框
                    DevInEditorInput(
                        initialText = "",
                        placeholder = "Describe your coding task...",
                        callbacks = callbacks,
                        completionManager = currentWorkspace?.completionManager,
                        isCompactMode = true,
                        isExecuting = viewModel.isExecuting,
                        onStopClick = { viewModel.cancelTask() },
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
            },
            second = {
                // 右侧：TreeView + FileViewer（也使用 ResizableSplitPane）
                val hasFileViewer = viewModel.renderer.currentViewingFile != null
                if (hasFileViewer) {
                    ResizableSplitPane(
                        modifier = Modifier.fillMaxSize(),
                        initialSplitRatio = 0.4f,
                        minRatio = 0.2f,
                        maxRatio = 0.6f,
                        first = {
                            FileSystemTreeView(
                                rootPath = currentWorkspace?.rootPath ?: "",
                                onFileClick = { filePath ->
                                    viewModel.renderer.openFileViewer(filePath)
                                },
                                onClose = { viewModel.closeTreeView() },
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        second = {
                            viewModel.renderer.currentViewingFile?.let { filePath ->
                                FileViewerPanelWrapper(
                                    filePath = filePath,
                                    onClose = { viewModel.renderer.closeFileViewer() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    )
                } else {
                    // 只有 TreeView
                    FileSystemTreeView(
                        rootPath = currentWorkspace?.rootPath ?: "",
                        onFileClick = { filePath ->
                            viewModel.renderer.openFileViewer(filePath)
                        },
                        onClose = { viewModel.closeTreeView() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        )
    } else {
        // TreeView 未打开时的布局
        Column(modifier = modifier.fillMaxSize()) {
            // TopBar
            cc.unitmesh.devins.ui.compose.chat.TopBarMenu(
                hasHistory = hasHistory,
                hasDebugInfo = hasDebugInfo,
                currentModelConfig = currentModelConfig,
                selectedAgent = selectedAgent,
                availableAgents = availableAgents,
                useAgentMode = useAgentMode,
                isTreeViewVisible = isTreeViewVisible,
                onOpenDirectory = onOpenDirectory,
                onClearHistory = onClearHistory,
                onShowDebug = onShowDebug,
                onModelConfigChange = onModelConfigChange,
                onAgentChange = onAgentChange,
                onModeToggle = onModeToggle,
                onToggleTreeView = { onToggleTreeView(!isTreeViewVisible) },
                onShowModelConfig = onShowModelConfig,
                onShowToolConfig = onShowToolConfig,
                modifier = Modifier.statusBarsPadding()
            )

            // Chat 消息列表
            AgentMessageList(
                renderer = viewModel.renderer,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onOpenFileViewer = { filePath ->
                    viewModel.renderer.openFileViewer(filePath)
                }
            )

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
                isExecuting = viewModel.isExecuting,
                onStopClick = { viewModel.cancelTask() },
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


