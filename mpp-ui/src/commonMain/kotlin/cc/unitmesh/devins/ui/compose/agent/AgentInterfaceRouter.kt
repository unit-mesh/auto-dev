package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.unitmesh.devins.ui.compose.agent.codereview.CodeReviewPage
import cc.unitmesh.llm.KoogLLMService

/**
 * Agent Interface Router
 *
 * Routes to different UI based on selected agent type:
 * - CODING: Traditional chat interface
 * - CODE_REVIEW: Side-by-side diff + AI fix view
 */
@Composable
fun AgentInterfaceRouter(
    llmService: KoogLLMService?,
    isTreeViewVisible: Boolean = false,
    onConfigWarning: () -> Unit,
    onToggleTreeView: (Boolean) -> Unit = {},
    chatHistoryManager: cc.unitmesh.devins.llm.ChatHistoryManager? = null,
    selectedAgentType: AgentType = AgentType.CODING,
    onAgentTypeChange: (AgentType) -> Unit = {},
    hasHistory: Boolean = false,
    hasDebugInfo: Boolean = false,
    currentModelConfig: cc.unitmesh.llm.ModelConfig? = null,
    selectedAgent: String = "Default",
    availableAgents: List<String> = listOf("Default"),
    useAgentMode: Boolean = true,
    selectedRemoteAgentType: String = "Local",
    onOpenDirectory: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onShowDebug: () -> Unit = {},
    onModelConfigChange: (cc.unitmesh.llm.ModelConfig) -> Unit = {},
    onAgentChange: (String) -> Unit = {},
    onModeToggle: () -> Unit = {},
    onRemoteAgentTypeChange: (String) -> Unit = {},
    onConfigureRemote: () -> Unit = {},
    onShowModelConfig: () -> Unit = {},
    onShowToolConfig: () -> Unit = {},
    showTopBar: Boolean = true,
    onSessionSelected: ((String) -> Unit)? = null,
    onNewChat: (() -> Unit)? = null,
    onInternalSessionSelected: (((String) -> Unit) -> Unit)? = null,
    onInternalNewChat: ((() -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (selectedAgentType) {
        AgentType.CODE_REVIEW -> {
            // Show dedicated code review page with side-by-side view
            CodeReviewPage(
                llmService = llmService,
                onBack = {
                    // Switch back to CODING agent
                    onAgentTypeChange(AgentType.CODING)
                },
                modifier = modifier
            )
        }
        AgentType.CODING -> {
            // Show traditional chat interface
            AgentChatInterface(
                llmService = llmService,
                isTreeViewVisible = isTreeViewVisible,
                onConfigWarning = onConfigWarning,
                onToggleTreeView = onToggleTreeView,
                chatHistoryManager = chatHistoryManager,
                selectedAgentType = selectedAgentType,
                onAgentTypeChange = onAgentTypeChange,
                hasHistory = hasHistory,
                hasDebugInfo = hasDebugInfo,
                currentModelConfig = currentModelConfig,
                selectedAgent = selectedAgent,
                availableAgents = availableAgents,
                useAgentMode = useAgentMode,
                selectedRemoteAgentType = selectedRemoteAgentType,
                onOpenDirectory = onOpenDirectory,
                onClearHistory = onClearHistory,
                onShowDebug = onShowDebug,
                onModelConfigChange = onModelConfigChange,
                onAgentChange = onAgentChange,
                onModeToggle = onModeToggle,
                onRemoteAgentTypeChange = onRemoteAgentTypeChange,
                onConfigureRemote = onConfigureRemote,
                onShowModelConfig = onShowModelConfig,
                onShowToolConfig = onShowToolConfig,
                showTopBar = showTopBar,
                onSessionSelected = onSessionSelected,
                onNewChat = onNewChat,
                onInternalSessionSelected = onInternalSessionSelected,
                onInternalNewChat = onInternalNewChat,
                modifier = modifier
            )
        }
    }
}
