package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.AgentType
import cc.unitmesh.agent.Platform
import cc.unitmesh.agent.render.TaskStatus
import cc.unitmesh.devins.ui.base.ResizableSplitPane
import cc.unitmesh.devins.ui.compose.chat.TopBarMenu
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.ui.state.UIStateManager
import cc.unitmesh.devins.workspace.WorkspaceManager
import cc.unitmesh.llm.KoogLLMService

@Composable
fun AgentChatInterface(
    llmService: KoogLLMService?,
    isTreeViewVisible: Boolean = false, // 保留供外部读取，但内部使用全局状态
    onConfigWarning: () -> Unit,
    onToggleTreeView: (Boolean) -> Unit = {}, // 保留供外部回调，但不再主动调用
    // 会话管理
    chatHistoryManager: cc.unitmesh.devins.llm.ChatHistoryManager? = null,
    // Agent 类型（LOCAL or CODING）
    selectedAgentType: AgentType = AgentType.CODING,
    onAgentTypeChange: (AgentType) -> Unit = {},
    // TopBar 参数
    hasHistory: Boolean = false,
    hasDebugInfo: Boolean = false,
    currentModelConfig: cc.unitmesh.llm.ModelConfig? = null,
    selectedAgent: String = "Default",
    availableAgents: List<String> = listOf("Default"),
    useAgentMode: Boolean = true,
    onOpenDirectory: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onModelConfigChange: (cc.unitmesh.llm.ModelConfig) -> Unit = {},
    onAgentChange: (String) -> Unit = {},
    onModeToggle: () -> Unit = {},
    onConfigureRemote: () -> Unit = {},
    onShowModelConfig: () -> Unit = {},
    onShowToolConfig: () -> Unit = {},
    showTopBar: Boolean = true,
    // 会话切换支持
    onSessionSelected: ((String) -> Unit)? = null,
    onNewChat: (() -> Unit)? = null,
    onInternalSessionSelected: (((String) -> Unit) -> Unit)? = null,
    onInternalNewChat: ((() -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isTreeViewVisibleState by UIStateManager.isTreeViewVisible.collectAsState()

    val currentWorkspace by WorkspaceManager.workspaceFlow.collectAsState()
    val viewModel =
        remember(llmService, currentWorkspace?.rootPath, chatHistoryManager) {
            val workspace = currentWorkspace
            val rootPath = workspace?.rootPath ?: Platform.getUserHomeDir()

            CodingAgentViewModel(
                llmService = llmService,
                projectPath = rootPath,
                maxIterations = 100,
                chatHistoryManager = chatHistoryManager
            )
        }

    LaunchedEffect(selectedAgentType) {
        viewModel.switchAgent(selectedAgentType)
    }

    val handleSessionSelected: (String) -> Unit = remember(viewModel) {
        { sessionId ->
            viewModel.switchSession(sessionId)
            onSessionSelected?.invoke(sessionId)
        }
    }

    val handleNewChat: () -> Unit = remember(viewModel) {
        {
            viewModel.newSession()
            onNewChat?.invoke()
        }
    }

    LaunchedEffect(handleSessionSelected, handleNewChat) {
        onInternalSessionSelected?.invoke(handleSessionSelected)
        onInternalNewChat?.invoke(handleNewChat)
    }

    if (isTreeViewVisibleState) {
        ResizableSplitPane(
            modifier = modifier.fillMaxSize(),
            initialSplitRatio = 0.6f,
            minRatio = 0.3f,
            maxRatio = 0.8f,
            first = {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (showTopBar) {
                        TopBarMenu(
                            hasHistory = hasHistory,
                            hasDebugInfo = hasDebugInfo,
                            currentModelConfig = currentModelConfig,
                            selectedAgent = selectedAgent,
                            availableAgents = availableAgents,
                            isTreeViewVisible = isTreeViewVisible,
                            currentAgentType = selectedAgentType,
                            onAgentTypeChange = onAgentTypeChange,
                            onOpenDirectory = onOpenDirectory,
                            onClearHistory = onClearHistory,
                            onModelConfigChange = onModelConfigChange,
                            onAgentChange = onAgentChange,
                            onModeToggle = onModeToggle,
                            onToggleTreeView = { onToggleTreeView(!isTreeViewVisible) },
                            onConfigureRemote = onConfigureRemote,
                            onShowModelConfig = onShowModelConfig,
                            onShowToolConfig = onShowToolConfig,
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    // Chat 消息列表 和 Task Panel
                    val activeTasks = remember(viewModel.renderer.tasks) {
                        viewModel.renderer.tasks.filter {
                            it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED
                        }
                    }

                    if (activeTasks.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AgentMessageList(
                                renderer = viewModel.renderer,
                                modifier = Modifier
                                    .weight(0.65f)
                                    .fillMaxHeight(),
                                onOpenFileViewer = { filePath ->
                                    viewModel.renderer.openFileViewer(filePath)
                                }
                            )

                            TaskPanel(
                                tasks = activeTasks,
                                modifier = Modifier
                                    .weight(0.35f)
                                    .fillMaxHeight()
                                    .padding(end = 12.dp),
                                onClose = { /* Tasks auto-hide when completed */ }
                            )
                        }
                    } else {
                        AgentMessageList(
                            renderer = viewModel.renderer,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            onOpenFileViewer = { filePath ->
                                viewModel.renderer.openFileViewer(filePath)
                            }
                        )
                    }

                    val callbacks =
                        remember(viewModel) {
                            createAgentCallbacks(
                                viewModel = viewModel,
                                onConfigWarning = onConfigWarning
                            )
                        }

                    when (selectedAgentType) {
                        AgentType.LOCAL_CHAT,
                        AgentType.CODING -> {
                            DevInEditorInput(
                                initialText = "",
                                placeholder = "Describe your coding task...",
                                callbacks = callbacks,
                                completionManager = currentWorkspace?.completionManager,
                                isCompactMode = true,
                                isExecuting = viewModel.isExecuting,
                                onStopClick = { viewModel.cancelTask() },
                                onModelConfigChange = { /* Handle model config change if needed */ },
                                renderer = viewModel.renderer,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .imePadding()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        AgentType.CODE_REVIEW,
                        AgentType.KNOWLEDGE -> {
                            // CODE_REVIEW and DOCUMENT_READER have their own full-page interfaces
                            // They should not reach here - handled by AgentInterfaceRouter
                        }

                        AgentType.REMOTE -> {
                            // REMOTE type should not reach here - it's handled by AgentInterfaceRouter
                            // This is a fallback to prevent compilation errors
                        }
                    }

                    ToolLoadingStatusBar(
                        viewModel = viewModel,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            },
            second = {
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
                                onClose = { UIStateManager.setTreeViewVisible(false) },
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
                    FileSystemTreeView(
                        rootPath = currentWorkspace?.rootPath ?: "",
                        onFileClick = { filePath ->
                            viewModel.renderer.openFileViewer(filePath)
                        },
                        onClose = { UIStateManager.setTreeViewVisible(false) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            if (showTopBar) {
                cc.unitmesh.devins.ui.compose.chat.TopBarMenu(
                    hasHistory = hasHistory,
                    hasDebugInfo = hasDebugInfo,
                    currentModelConfig = currentModelConfig,
                    selectedAgent = selectedAgent,
                    availableAgents = availableAgents,
                    isTreeViewVisible = isTreeViewVisibleState,
                    currentAgentType = selectedAgentType,
                    onAgentTypeChange = onAgentTypeChange,
                    onOpenDirectory = onOpenDirectory,
                    onClearHistory = onClearHistory,
                    onModelConfigChange = onModelConfigChange,
                    onAgentChange = onAgentChange,
                    onModeToggle = onModeToggle,
                    onToggleTreeView = { UIStateManager.toggleTreeView() },
                    onConfigureRemote = onConfigureRemote,
                    onShowModelConfig = onShowModelConfig,
                    onShowToolConfig = onShowToolConfig,
                    modifier = Modifier.statusBarsPadding()
                )
            }

            // Chat 消息列表 和 Task Panel
            val activeTasks = remember(viewModel.renderer.tasks) {
                viewModel.renderer.tasks.filter {
                    it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED
                }
            }

            if (activeTasks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AgentMessageList(
                        renderer = viewModel.renderer,
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxHeight(),
                        onOpenFileViewer = { filePath ->
                            viewModel.renderer.openFileViewer(filePath)
                        }
                    )

                    TaskPanel(
                        tasks = activeTasks,
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight()
                            .padding(end = 12.dp),
                        onClose = { /* Tasks auto-hide when completed */ }
                    )
                }
            } else {
                AgentMessageList(
                    renderer = viewModel.renderer,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    onOpenFileViewer = { filePath ->
                        viewModel.renderer.openFileViewer(filePath)
                    }
                )
            }

            val callbacks =
                remember(viewModel) {
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
                renderer = viewModel.renderer,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            ToolLoadingStatusBar(
                viewModel = viewModel,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ToolLoadingStatusBar(
    viewModel: CodingAgentViewModel,
    modifier: Modifier = Modifier
) {
    val mcpPreloadingStatus = viewModel.mcpPreloadingStatus
    val mcpPreloadingMessage = viewModel.mcpPreloadingMessage
    val toolStatus by remember(mcpPreloadingStatus) {
        derivedStateOf { viewModel.getToolLoadingStatus() }
    }

    Card(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolStatusChip(
                label = "SubAgents",
                count = toolStatus.subAgentsEnabled,
                total = toolStatus.subAgentsTotal,
                isLoading = false,
                color = MaterialTheme.colorScheme.secondary,
                tooltip = "AI agents: error-recovery, log-summary, codebase-investigator"
            )

            ToolStatusChip(
                label = "MCP Tools",
                count = toolStatus.mcpToolsEnabled,
                total = if (toolStatus.isLoading) "∞" else toolStatus.mcpToolsEnabled.toString(),
                isLoading = toolStatus.isLoading,
                color =
                    if (!toolStatus.isLoading && toolStatus.mcpToolsEnabled > 0) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                tooltip = "External tools from MCP servers (${toolStatus.mcpServersLoaded}/${toolStatus.mcpServersTotal} servers)"
            )

            Spacer(modifier = Modifier.weight(1f))

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
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(
                        color = if (isLoading) MaterialTheme.colorScheme.outline.copy(alpha = 0.6f) else color,
                        shape = CircleShape
                    )
        ) {
            if (!isLoading && count > 0) {
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .align(Alignment.Center)
                            .background(
                                color = color.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                )
            }
        }

        Text(
            text = "$label ($count/$total)",
            style = MaterialTheme.typography.labelMedium,
            color =
                if (isLoading) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}
