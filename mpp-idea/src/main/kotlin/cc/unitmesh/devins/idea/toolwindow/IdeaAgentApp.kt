package cc.unitmesh.devins.idea.toolwindow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.idea.editor.IdeaBottomToolbar
import cc.unitmesh.devins.idea.editor.IdeaDevInInput
import cc.unitmesh.devins.idea.editor.IdeaInputListener
import cc.unitmesh.devins.idea.editor.IdeaInputTrigger
import cc.unitmesh.devins.idea.editor.IdeaModelConfigDialog
import cc.unitmesh.devins.idea.toolwindow.codereview.IdeaCodeReviewContent
import cc.unitmesh.devins.idea.toolwindow.codereview.IdeaCodeReviewViewModel
import cc.unitmesh.devins.idea.toolwindow.header.IdeaAgentTabsHeader
import cc.unitmesh.devins.idea.toolwindow.knowledge.IdeaKnowledgeContent
import cc.unitmesh.devins.idea.toolwindow.knowledge.IdeaKnowledgeViewModel
import cc.unitmesh.devins.idea.toolwindow.status.IdeaToolLoadingStatusBar
import cc.unitmesh.devins.idea.toolwindow.timeline.IdeaEmptyStateMessage
import cc.unitmesh.devins.idea.toolwindow.timeline.IdeaTimelineContent
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.NamedModelConfig
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel

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

    // Auto-scroll to bottom when new items arrive
    LaunchedEffect(timeline.size, streamingOutput) {
        if (timeline.isNotEmpty() || streamingOutput.isNotEmpty()) {
            val targetIndex = if (streamingOutput.isNotEmpty()) timeline.size else timeline.lastIndex.coerceAtLeast(0)
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Create CodeReviewViewModel when switching to CODE_REVIEW tab
    LaunchedEffect(currentAgentType) {
        if (currentAgentType == AgentType.CODE_REVIEW && codeReviewViewModel == null) {
            codeReviewViewModel = IdeaCodeReviewViewModel(project, coroutineScope)
        }
        if (currentAgentType == AgentType.KNOWLEDGE && knowledgeViewModel == null) {
            knowledgeViewModel = IdeaKnowledgeViewModel(project, coroutineScope)
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

        // Content based on agent type
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (currentAgentType) {
                AgentType.CODING, AgentType.REMOTE, AgentType.LOCAL_CHAT -> {
                    IdeaTimelineContent(
                        timeline = timeline,
                        streamingOutput = streamingOutput,
                        listState = listState
                    )
                }
                AgentType.CODE_REVIEW -> {
                    codeReviewViewModel?.let { vm ->
                        IdeaCodeReviewContent(
                            viewModel = vm,
                            parentDisposable = viewModel
                        )
                    } ?: IdeaEmptyStateMessage("Loading Code Review...")
                }
                AgentType.KNOWLEDGE -> {
                    knowledgeViewModel?.let { vm ->
                        IdeaKnowledgeContent(viewModel = vm)
                    } ?: IdeaEmptyStateMessage("Loading Knowledge Agent...")
                }
            }
        }

        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth().height(1.dp))

        // Input area (only for chat-based modes)
        if (currentAgentType == AgentType.CODING || currentAgentType == AgentType.REMOTE || currentAgentType == AgentType.LOCAL_CHAT) {
            IdeaDevInInputArea(
                project = project,
                parentDisposable = viewModel,
                isProcessing = isExecuting,
                onSend = { viewModel.sendMessage(it) },
                onAbort = { viewModel.cancelTask() },
                workspacePath = project.basePath,
                totalTokens = null, // TODO: integrate token counting from renderer
                onSettingsClick = { viewModel.setShowConfigDialog(true) },
                onAtClick = {
                    // @ click triggers agent completion - placeholder for now
                },
                availableConfigs = availableConfigs,
                currentConfigName = currentConfigName,
                onConfigSelect = { config ->
                    viewModel.setActiveConfig(config.name)
                },
                onConfigureClick = { viewModel.setShowConfigDialog(true) }
            )
        }

        // Tool loading status bar
        IdeaToolLoadingStatusBar(
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }

    // Model Configuration Dialog
    if (showConfigDialog) {
        val dialogConfig = currentModelConfig ?: ModelConfig()
        IdeaModelConfigDialog(
            currentConfig = dialogConfig,
            currentConfigName = currentConfigName,
            onDismiss = { viewModel.setShowConfigDialog(false) },
            onSave = { name, config ->
                val namedConfig = NamedModelConfig.fromModelConfig(name, config)
                viewModel.saveModelConfig(namedConfig, setActive = true)
                viewModel.setShowConfigDialog(false)
            }
        )
    }
}

/**
 * Advanced chat input area with full DevIn language support.
 *
 * Uses IdeaDevInInput (EditorTextField-based) embedded via SwingPanel for:
 * - DevIn language syntax highlighting and completion
 * - IntelliJ's native completion popup integration
 * - Enter to submit, Shift+Enter for newline
 * - @ trigger for agent completion
 * - Token usage display
 * - Settings access
 * - Stop/Send button based on execution state
 * - Model selector for switching between LLM configurations
 */
@Composable
private fun IdeaDevInInputArea(
    project: Project,
    parentDisposable: Disposable,
    isProcessing: Boolean,
    onSend: (String) -> Unit,
    onAbort: () -> Unit,
    workspacePath: String? = null,
    totalTokens: Int? = null,
    onSettingsClick: () -> Unit = {},
    onAtClick: () -> Unit = {},
    availableConfigs: List<NamedModelConfig> = emptyList(),
    currentConfigName: String? = null,
    onConfigSelect: (NamedModelConfig) -> Unit = {},
    onConfigureClick: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var devInInput by remember { mutableStateOf<IdeaDevInInput?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        // DevIn Editor via SwingPanel
        SwingPanel(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            factory = {
                val input = IdeaDevInInput(
                    project = project,
                    disposable = parentDisposable,
                    showAgent = true
                ).apply {
                    recreateDocument()

                    addInputListener(object : IdeaInputListener {
                        override fun editorAdded(editor: EditorEx) {
                            // Editor is ready
                        }

                        override fun onSubmit(text: String, trigger: IdeaInputTrigger) {
                            if (text.isNotBlank() && !isProcessing) {
                                onSend(text)
                                clearInput()
                                inputText = ""
                            }
                        }

                        override fun onStop() {
                            onAbort()
                        }

                        override fun onTextChanged(text: String) {
                            inputText = text
                        }
                    })
                }

                // Register for disposal
                Disposer.register(parentDisposable, input)
                devInInput = input

                // Wrap in a JPanel to handle sizing
                JPanel(BorderLayout()).apply {
                    add(input, BorderLayout.CENTER)
                    preferredSize = Dimension(800, 120)
                    minimumSize = Dimension(200, 80)
                }
            },
            update = { panel ->
                // Update panel if needed
            }
        )

        // Bottom toolbar with Compose
        IdeaBottomToolbar(
            onSendClick = {
                val text = devInInput?.text?.trim() ?: inputText.trim()
                if (text.isNotBlank() && !isProcessing) {
                    onSend(text)
                    devInInput?.clearInput()
                    inputText = ""
                }
            },
            sendEnabled = inputText.isNotBlank() && !isProcessing,
            isExecuting = isProcessing,
            onStopClick = onAbort,
            onAtClick = {
                devInInput?.appendText("@")
                onAtClick()
            },
            onSlashClick = {
                // Insert / at current cursor position to trigger slash commands
                devInInput?.appendText("/")
            },
            onSettingsClick = onSettingsClick,
            workspacePath = workspacePath,
            totalTokens = totalTokens,
            availableConfigs = availableConfigs,
            currentConfigName = currentConfigName,
            onConfigSelect = onConfigSelect,
            onConfigureClick = onConfigureClick
        )
    }
}

