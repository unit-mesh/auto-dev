package cc.unitmesh.devins.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.editor.EditorCallbacks
import cc.unitmesh.devins.ui.compose.agent.AgentMessageList
import cc.unitmesh.devins.ui.compose.agent.FileSystemTreeView
import cc.unitmesh.devins.ui.compose.agent.FileViewerPanelWrapper
import cc.unitmesh.devins.ui.compose.agent.ResizableSplitPane
import cc.unitmesh.devins.ui.compose.agent.createAgentCallbacks
import cc.unitmesh.devins.ui.compose.editor.DevInEditorInput
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.workspace.WorkspaceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Remote Agent Chat Interface for Compose
 *
 * This is similar to AgentChatInterface but uses RemoteCodingAgentViewModel
 * to connect to a remote mpp-server instead of running locally.
 */
@Composable
fun RemoteAgentChatInterface(
    serverUrl: String,
    useServerConfig: Boolean = false,
    isTreeViewVisible: Boolean = false,
    onConfigWarning: () -> Unit,
    onToggleTreeView: (Boolean) -> Unit = {},
    // TopBar 参数
    hasHistory: Boolean = false,
    hasDebugInfo: Boolean = false,
    currentModelConfig: cc.unitmesh.llm.ModelConfig? = null,
    selectedAgent: String = "Remote",
    availableAgents: List<String> = listOf("Remote"),
    useAgentMode: Boolean = true,
    onOpenDirectory: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onShowDebug: () -> Unit = {},
    onModelConfigChange: (cc.unitmesh.llm.ModelConfig) -> Unit = {},
    onAgentChange: (String) -> Unit = {},
    onModeToggle: () -> Unit = {},
    onShowModelConfig: () -> Unit = {},
    onShowToolConfig: () -> Unit = {},
    // Remote-specific parameters
    projectId: String = "",
    onProjectChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentWorkspace by WorkspaceManager.workspaceFlow.collectAsState()

    val viewModel = remember(serverUrl, useServerConfig) {
        RemoteCodingAgentViewModel(
            serverUrl = serverUrl,
            useServerConfig = useServerConfig
        )
    }

    // 同步外部 TreeView 状态到 ViewModel
    LaunchedEffect(isTreeViewVisible) {
        if (viewModel.isTreeViewVisible != isTreeViewVisible) {
            viewModel.isTreeViewVisible = isTreeViewVisible
        }
    }

    // 监听 ViewModel 状态变化并通知外部
    LaunchedEffect(viewModel.isTreeViewVisible) {
        onToggleTreeView(viewModel.isTreeViewVisible)
    }

    // Show connection status if not connected
    if (!viewModel.isConnected && viewModel.connectionError != null) {
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
                        text = "⚠️ Connection Error",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = viewModel.connectionError ?: "Failed to connect to server",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Server URL: $serverUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.Default).launch {
                                    viewModel.checkConnection()
                                }
                            }
                        ) {
                            Text("Retry Connection")
                        }
                        OutlinedButton(onClick = onConfigWarning) {
                            Text("Configure")
                        }
                    }
                }
            }
        }
        return
    }

    // Main UI - similar structure to AgentChatInterface
    if (viewModel.isTreeViewVisible) {
        ResizableSplitPane(
            modifier = modifier.fillMaxSize(),
            initialSplitRatio = 0.6f,
            minRatio = 0.3f,
            maxRatio = 0.8f,
            first = {
                Column(modifier = Modifier.fillMaxSize()) {
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
                        onClearHistory = {
                            viewModel.clearHistory()
                            onClearHistory()
                        },
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

                    // Project selector for remote mode
                    if (viewModel.availableProjects.isNotEmpty()) {
                        ProjectSelector(
                            projects = viewModel.availableProjects,
                            selectedProjectId = projectId,
                            onProjectChange = onProjectChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    // 输入框
                    val callbacks = remember(viewModel, projectId) {
                        object : EditorCallbacks {
                            override fun onSubmit(input: String) {
                                if (projectId.isBlank()) {
                                    viewModel.renderer.renderError("Please select a project first")
                                } else {
                                    viewModel.executeTask(projectId, input)
                                }
                            }
                        }
                    }

                    DevInEditorInput(
                        initialText = "",
                        placeholder = "Describe your coding task...",
                        callbacks = callbacks,
                        completionManager = currentWorkspace?.completionManager,
                        isCompactMode = true,
                        isExecuting = viewModel.isExecuting,
                        onStopClick = { viewModel.cancelTask() },
                        onModelConfigChange = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    // Connection status indicator
                    RemoteConnectionStatusBar(
                        isConnected = viewModel.isConnected,
                        serverUrl = serverUrl,
                        useServerConfig = useServerConfig,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            },
            second = {
                // 右侧：TreeView + FileViewer
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
                onClearHistory = {
                    viewModel.clearHistory()
                    onClearHistory()
                },
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

            // Project selector for remote mode
            if (viewModel.availableProjects.isNotEmpty()) {
                ProjectSelector(
                    projects = viewModel.availableProjects,
                    selectedProjectId = projectId,
                    onProjectChange = onProjectChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // 输入框
            val callbacks = remember(viewModel, projectId) {
                object : EditorCallbacks {
                    override fun onSubmit(input: String) {
                        if (projectId.isBlank()) {
                            viewModel.renderer.renderError("Please select a project first")
                        } else {
                            viewModel.executeTask(projectId, input)
                        }
                    }
                }
            }

            DevInEditorInput(
                initialText = "",
                placeholder = "Describe your coding task...",
                callbacks = callbacks,
                completionManager = currentWorkspace?.completionManager,
                isCompactMode = true,
                isExecuting = viewModel.isExecuting,
                onStopClick = { viewModel.cancelTask() },
                onModelConfigChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            // Connection status indicator
            RemoteConnectionStatusBar(
                isConnected = viewModel.isConnected,
                serverUrl = serverUrl,
                useServerConfig = useServerConfig,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Project selector dropdown for remote mode
 * 
 * Supports both:
 * - Selecting from available projects
 * - Manually entering a project ID or Git URL
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectSelector(
    projects: List<ProjectInfo>,
    selectedProjectId: String,
    onProjectChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isManualInput by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf(selectedProjectId) }

    // Update manual input when selected project changes
    LaunchedEffect(selectedProjectId) {
        if (!isManualInput) {
            manualInput = selectedProjectId
        }
    }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (!isManualInput) {
                // Dropdown mode
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = selectedProjectId.ifBlank { "Select a project or enter Git URL..." },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Project / Git URL") },
                        trailingIcon = { 
                            Row {
                                IconButton(onClick = { isManualInput = true }) {
                                    Icon(
                                        imageVector = AutoDevComposeIcons.Code,
                                        contentDescription = "Enter Git URL manually",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(project.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            project.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onProjectChange(project.id)
                                    expanded = false
                                }
                            )
                        }
                        
                        HorizontalDivider()
                        
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = AutoDevComposeIcons.Code,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("Enter Git URL manually")
                                }
                            },
                            onClick = {
                                isManualInput = true
                                expanded = false
                            }
                        )
                    }
                }
            } else {
                // Manual input mode
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text("Project ID or Git URL") },
                    placeholder = { Text("autocrud or https://github.com/user/repo.git") },
                    supportingText = {
                        Text(
                            "Enter a project ID or paste a Git repository URL",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    onProjectChange(manualInput.trim())
                                }
                            ) {
                                Icon(
                                    imageVector = AutoDevComposeIcons.Check,
                                    contentDescription = "Confirm",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    isManualInput = false
                                    manualInput = selectedProjectId
                                }
                            ) {
                                Icon(
                                    imageVector = AutoDevComposeIcons.Close,
                                    contentDescription = "Cancel"
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

/**
 * Connection status indicator
 */
@Composable
private fun RemoteConnectionStatusBar(
    isConnected: Boolean,
    serverUrl: String,
    useServerConfig: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        shape = CircleShape
                    )
            )

            Text(
                text = "Remote: $serverUrl",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (useServerConfig) {
                Text(
                    text = "• Using server's LLM config",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

