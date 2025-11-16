package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.unitmesh.devins.ui.compose.agent.codereview.CodeReviewPage
import cc.unitmesh.devins.ui.remote.RemoteAgentChatInterface
import cc.unitmesh.llm.KoogLLMService

/**
 * Agent Interface Router
 *
 * Unified router for all agent types:
 * - LOCAL: Simple local chat interface
 * - CODING: Full-featured coding agent with tools
 * - CODE_REVIEW: Dedicated code review interface
 * - REMOTE: Remote agent connected to mpp-server
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
    onOpenDirectory: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onShowDebug: () -> Unit = {},
    onModelConfigChange: (cc.unitmesh.llm.ModelConfig) -> Unit = {},
    onAgentChange: (String) -> Unit = {},
    onModeToggle: () -> Unit = {},
    onConfigureRemote: () -> Unit = {},
    onShowModelConfig: () -> Unit = {},
    onShowToolConfig: () -> Unit = {},
    showTopBar: Boolean = true,
    onSessionSelected: ((String) -> Unit)? = null,
    onNewChat: (() -> Unit)? = null,
    onInternalSessionSelected: (((String) -> Unit) -> Unit)? = null,
    onInternalNewChat: ((() -> Unit) -> Unit)? = null,
    // Remote-specific parameters
    serverUrl: String = "http://localhost:8080",
    useServerConfig: Boolean = false,
    projectId: String = "",
    gitUrl: String = "",
    onProjectChange: (String) -> Unit = {},
    onGitUrlChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (selectedAgentType) {
        AgentType.CODE_REVIEW -> {
            CodeReviewPage(
                llmService = llmService,
                onBack = {
                    onAgentTypeChange(AgentType.CODING)
                },
                modifier = modifier
            )
        }

        AgentType.REMOTE -> {
            RemoteAgentChatInterface(
                serverUrl = serverUrl,
                useServerConfig = useServerConfig,
                isTreeViewVisible = isTreeViewVisible,
                onToggleTreeView = onToggleTreeView,
                hasHistory = false, // Remote agent manages its own history
                hasDebugInfo = hasDebugInfo,
                currentModelConfig = currentModelConfig,
                selectedAgent = selectedAgent,
                availableAgents = availableAgents,
                useAgentMode = useAgentMode,
                selectedAgentType = selectedAgentType.getDisplayName(),
                onOpenDirectory = onOpenDirectory,
                onClearHistory = onClearHistory,
                onModelConfigChange = onModelConfigChange,
                onAgentChange = onAgentChange,
                onModeToggle = onModeToggle,
                onAgentTypeChange = { typeName ->
                    val newType = when (typeName) {
                        "Local Chat", "Local" -> AgentType.LOCAL_CHAT
                        "Remote Agent", "Remote" -> AgentType.REMOTE
                        "Code Review" -> AgentType.CODE_REVIEW
                        else -> AgentType.CODING
                    }
                    onAgentTypeChange(newType)
                },
                onConfigureRemote = onConfigureRemote,
                onShowModelConfig = onShowModelConfig,
                onShowToolConfig = onShowToolConfig,
                projectId = projectId,
                gitUrl = gitUrl,
                onProjectChange = onProjectChange,
                onGitUrlChange = onGitUrlChange,
                modifier = modifier
            )
        }

        AgentType.LOCAL_CHAT,
        AgentType.CODING -> {
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
                onOpenDirectory = onOpenDirectory,
                onClearHistory = onClearHistory,
                onModelConfigChange = onModelConfigChange,
                onAgentChange = onAgentChange,
                onModeToggle = onModeToggle,
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
