package cc.unitmesh.devins.idea.toolwindow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.idea.editor.IdeaModelConfigDialogWrapper
import cc.unitmesh.devins.idea.toolwindow.codereview.IdeaCodeReviewContent
import cc.unitmesh.devins.idea.toolwindow.codereview.IdeaCodeReviewViewModel
import cc.unitmesh.devins.idea.components.header.IdeaAgentTabsHeader
import cc.unitmesh.devins.idea.components.IdeaVerticalResizableSplitPane
import cc.unitmesh.devins.idea.toolwindow.knowledge.IdeaKnowledgeContent
import cc.unitmesh.devins.idea.toolwindow.knowledge.IdeaKnowledgeViewModel
import cc.unitmesh.devins.idea.toolwindow.remote.IdeaRemoteAgentContent
import cc.unitmesh.devins.idea.toolwindow.remote.IdeaRemoteAgentViewModel
import cc.unitmesh.devins.idea.toolwindow.remote.getEffectiveProjectId
import cc.unitmesh.devins.idea.components.status.IdeaToolLoadingStatusBar
import cc.unitmesh.devins.idea.components.timeline.IdeaEmptyStateMessage
import cc.unitmesh.devins.idea.components.timeline.IdeaTimelineContent
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.NamedModelConfig
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider

/**
 * Main Compose application for Agent ToolWindow.
 *
 * Features:
 * - Tab-based agent type switching (Agentic, Review, Knowledge, Remote)
 * - Timeline-based chat interface with tool calls
 * - LLM configuration support via mpp-ui's ConfigManager
 * - Real agent execution using mpp-core's CodingAgent
 * - Tool loading status bar
 *
 * Aligned with AgentChatInterface from mpp-ui for feature parity.
 */
@Composable
fun IdeaAgentApp(
    viewModel: IdeaAgentViewModel,
    project: Project,
    coroutineScope: CoroutineScope
) {
    val currentAgentType by viewModel.currentAgentType.collectAsState()
    val timeline by viewModel.renderer.timeline.collectAsState()
    val streamingOutput by viewModel.renderer.currentStreamingOutput.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val showConfigDialog by viewModel.showConfigDialog.collectAsState()
    val mcpPreloadingMessage by viewModel.mcpPreloadingMessage.collectAsState()
    val configWrapper by viewModel.configWrapper.collectAsState()
    val currentModelConfig by viewModel.currentModelConfig.collectAsState()
    val listState = rememberLazyListState()

    // Get available configs and current config name
    val availableConfigs = remember(configWrapper) {
        configWrapper?.getAllConfigs() ?: emptyList()
    }
    val currentConfigName = remember(configWrapper) {
        configWrapper?.getActiveName()
    }

    // Code Review ViewModel (created lazily when needed)
    var codeReviewViewModel by remember { mutableStateOf<IdeaCodeReviewViewModel?>(null) }

    // Knowledge ViewModel (created lazily when needed)
    var knowledgeViewModel by remember { mutableStateOf<IdeaKnowledgeViewModel?>(null) }

    // Remote Agent ViewModel (created lazily when needed)
    var remoteAgentViewModel by remember { mutableStateOf<IdeaRemoteAgentViewModel?>(null) }

    // Remote agent state for input handling
    var remoteProjectId by remember { mutableStateOf("") }
    var remoteGitUrl by remember { mutableStateOf("") }

    // Auto-scroll to bottom when new items arrive
    LaunchedEffect(timeline.size, streamingOutput) {
        if (timeline.isNotEmpty() || streamingOutput.isNotEmpty()) {
            val targetIndex = if (streamingOutput.isNotEmpty()) timeline.size else timeline.lastIndex.coerceAtLeast(0)
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Create ViewModels when switching tabs
    LaunchedEffect(currentAgentType) {
        if (currentAgentType == AgentType.CODE_REVIEW && codeReviewViewModel == null) {
            codeReviewViewModel = IdeaCodeReviewViewModel(project, coroutineScope)
        }
        if (currentAgentType == AgentType.KNOWLEDGE && knowledgeViewModel == null) {
            knowledgeViewModel = IdeaKnowledgeViewModel(project, coroutineScope)
        }
        if (currentAgentType == AgentType.REMOTE && remoteAgentViewModel == null) {
            remoteAgentViewModel = IdeaRemoteAgentViewModel(
                project = project,
                coroutineScope = coroutineScope,
                serverUrl = "http://localhost:8080"
            )
        }
    }

    // Dispose ViewModels when leaving their tabs
    DisposableEffect(currentAgentType) {
        onDispose {
            if (currentAgentType != AgentType.CODE_REVIEW) {
                codeReviewViewModel?.dispose()
                codeReviewViewModel = null
            }
            if (currentAgentType != AgentType.KNOWLEDGE) {
                knowledgeViewModel?.dispose()
                knowledgeViewModel = null
            }
            if (currentAgentType != AgentType.REMOTE) {
                remoteAgentViewModel?.dispose()
                remoteAgentViewModel = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        // Agent Type Tabs Header
        IdeaAgentTabsHeader(
            currentAgentType = currentAgentType,
            onAgentTypeChange = { viewModel.onAgentTypeChange(it) },
            onNewChat = { viewModel.clearHistory() },
            onSettings = { viewModel.setShowConfigDialog(true) }
        )

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Main content area with resizable split pane for chat-based modes
        when (currentAgentType) {
            AgentType.CODING, AgentType.LOCAL_CHAT -> {
                IdeaVerticalResizableSplitPane(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    initialSplitRatio = 0.75f,
                    minRatio = 0.3f,
                    maxRatio = 0.9f,
                    top = {
                        IdeaTimelineContent(
                            timeline = timeline,
                            streamingOutput = streamingOutput,
                            listState = listState,
                            project = project,
                            onProcessCancel = { cancelEvent ->
                                viewModel.handleProcessCancel(cancelEvent)
                            }
                        )
                    },
                    bottom = {
                        IdeaDevInInputArea(
                            project = project,
                            parentDisposable = viewModel,
                            isProcessing = isExecuting,
                            onSend = { viewModel.sendMessage(it) },
                            onAbort = { viewModel.cancelTask() },
                            workspacePath = project.basePath,
                            totalTokens = null,
                            onAtClick = {},
                            availableConfigs = availableConfigs,
                            currentConfigName = currentConfigName,
                            onConfigSelect = { config ->
                                viewModel.setActiveConfig(config.name)
                            },
                            onConfigureClick = { viewModel.setShowConfigDialog(true) }
                        )
                    }
                )
            }
            AgentType.REMOTE -> {
                remoteAgentViewModel?.let { remoteVm ->
                    val remoteIsExecuting by remoteVm.isExecuting.collectAsState()
                    val remoteIsConnected by remoteVm.isConnected.collectAsState()

                    IdeaVerticalResizableSplitPane(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        initialSplitRatio = 0.75f,
                        minRatio = 0.3f,
                        maxRatio = 0.9f,
                        top = {
                            IdeaRemoteAgentContent(
                                viewModel = remoteVm,
                                listState = listState,
                                onProjectIdChange = { remoteProjectId = it },
                                onGitUrlChange = { remoteGitUrl = it }
                            )
                        },
                        bottom = {
                            IdeaDevInInputArea(
                                project = project,
                                parentDisposable = viewModel,
                                isProcessing = remoteIsExecuting,
                                onSend = { task ->
                                    val effectiveProjectId = getEffectiveProjectId(remoteProjectId, remoteGitUrl)
                                    if (effectiveProjectId.isNotBlank()) {
                                        remoteVm.executeTask(effectiveProjectId, task, remoteGitUrl)
                                    } else {
                                        remoteVm.renderer.renderError("Please provide a project or Git URL")
                                    }
                                },
                                onAbort = { remoteVm.cancelTask() },
                                workspacePath = project.basePath,
                                totalTokens = null,
                                onAtClick = {},
                                availableConfigs = availableConfigs,
                                currentConfigName = currentConfigName,
                                onConfigSelect = { config ->
                                    viewModel.setActiveConfig(config.name)
                                },
                                onConfigureClick = { viewModel.setShowConfigDialog(true) }
                            )
                        }
                    )
                } ?: IdeaEmptyStateMessage("Loading Remote Agent...")
            }
            AgentType.CODE_REVIEW -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    codeReviewViewModel?.let { vm ->
                        IdeaCodeReviewContent(
                            viewModel = vm,
                            parentDisposable = viewModel
                        )
                    } ?: IdeaEmptyStateMessage("Loading Code Review...")
                }
            }
            AgentType.KNOWLEDGE -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    knowledgeViewModel?.let { vm ->
                        IdeaKnowledgeContent(viewModel = vm)
                    } ?: IdeaEmptyStateMessage("Loading Knowledge Agent...")
                }
            }
        }

        // Tool loading status bar
        IdeaToolLoadingStatusBar(
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }

    // Model Configuration Dialog using DialogWrapper for proper z-index handling
    LaunchedEffect(showConfigDialog) {
        if (showConfigDialog) {
            val dialogConfig = currentModelConfig ?: ModelConfig()
            IdeaModelConfigDialogWrapper.show(
                project = project,
                currentConfig = dialogConfig,
                currentConfigName = currentConfigName,
                onSave = { configName, newModelConfig ->
                    // If creating a new config (not editing current), ensure unique name
                    val existingNames = availableConfigs.map { it.name }
                    val finalConfigName =
                        if (currentConfigName != configName && configName in existingNames) {
                            // Auto-increment: my-glm -> my-glm-1 -> my-glm-2, etc.
                            ConfigManager.generateUniqueConfigName(configName, existingNames)
                        } else {
                            configName
                        }

                    // Convert ModelConfig to NamedModelConfig
                    val namedConfig = NamedModelConfig.fromModelConfig(
                        name = finalConfigName,
                        config = newModelConfig
                    )

                    // Save to file
                    viewModel.saveModelConfig(namedConfig, setActive = true)

                    if (finalConfigName != configName) {
                        println("✅ 配置名称已存在，自动重命名为: $finalConfigName")
                    }
                }
            )
            viewModel.setShowConfigDialog(false)
        }
    }
}

