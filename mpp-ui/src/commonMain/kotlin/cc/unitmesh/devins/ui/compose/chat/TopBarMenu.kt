package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cc.unitmesh.agent.Platform
import cc.unitmesh.llm.ModelConfig

/**
 * 平台自适应的顶部工具栏
 * - Android: 使用 Dropdown Menu 风格（移动端优化）
 * - WASM: 使用左侧可收起侧边栏风格（Web 优化）
 * - Desktop (JVM): 使用 IconButton 风格（桌面端优化）
 */
@Composable
fun TopBarMenu(
    hasHistory: Boolean,
    hasDebugInfo: Boolean,
    currentModelConfig: ModelConfig?,
    selectedAgent: String,
    availableAgents: List<String>,
    useAgentMode: Boolean = true,
    isTreeViewVisible: Boolean = false,
    // Remote Agent 相关参数
    selectedAgentType: String = "Local", // "Local" or "Remote"
    useSessionManagement: Boolean = false, // Session Management mode
    // Agent Task Type 相关参数 (Coding vs Code Review)
    selectedTaskAgentType: cc.unitmesh.devins.ui.compose.agent.AgentType = cc.unitmesh.devins.ui.compose.agent.AgentType.CODING,
    onTaskAgentTypeChange: (cc.unitmesh.devins.ui.compose.agent.AgentType) -> Unit = {},
    // Sidebar 相关参数
    showSessionSidebar: Boolean = false,
    onToggleSidebar: () -> Unit = {},
    onAgentTypeChange: (String) -> Unit = {},
    onConfigureRemote: () -> Unit = {},
    onSessionManagementToggle: () -> Unit = {},
    onOpenDirectory: () -> Unit,
    onClearHistory: () -> Unit,
    onShowDebug: () -> Unit,
    onModelConfigChange: (ModelConfig) -> Unit,
    onAgentChange: (String) -> Unit,
    onModeToggle: () -> Unit = {},
    onToggleTreeView: () -> Unit = {},
    onShowModelConfig: () -> Unit,
    onShowToolConfig: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (Platform.isAndroid) {
        // Android: 使用紧凑的 Dropdown 菜单风格
        TopBarMenuMobile(
            hasHistory = hasHistory,
            hasDebugInfo = hasDebugInfo,
            currentModelConfig = currentModelConfig,
            selectedAgent = selectedAgent,
            availableAgents = availableAgents,
            useAgentMode = useAgentMode,
            isTreeViewVisible = isTreeViewVisible,
            selectedAgentType = selectedAgentType,
            useSessionManagement = useSessionManagement,
            selectedTaskAgentType = selectedTaskAgentType,
            onAgentTypeChange = onAgentTypeChange,
            onTaskAgentTypeChange = onTaskAgentTypeChange,
            onConfigureRemote = onConfigureRemote,
            onSessionManagementToggle = onSessionManagementToggle,
            onOpenDirectory = onOpenDirectory,
            onClearHistory = onClearHistory,
            onShowDebug = onShowDebug,
            onAgentChange = onAgentChange,
            onModeToggle = onModeToggle,
            onToggleTreeView = onToggleTreeView,
            onShowModelConfig = onShowModelConfig,
            onShowToolConfig = onShowToolConfig,
            modifier = modifier
        )
    } else {
        // Desktop (JVM): 使用 IconButton 排列风格
        TopBarMenuDesktop(
            hasHistory = hasHistory,
            hasDebugInfo = hasDebugInfo,
            currentModelConfig = currentModelConfig,
            selectedAgent = selectedAgent,
            availableAgents = availableAgents,
            useAgentMode = useAgentMode,
            isTreeViewVisible = isTreeViewVisible,
            selectedAgentType = selectedAgentType,
            useSessionManagement = useSessionManagement,
            selectedTaskAgentType = selectedTaskAgentType,
            showSessionSidebar = showSessionSidebar,
            onToggleSidebar = onToggleSidebar,
            onAgentTypeChange = onAgentTypeChange,
            onTaskAgentTypeChange = onTaskAgentTypeChange,
            onConfigureRemote = onConfigureRemote,
            onSessionManagementToggle = onSessionManagementToggle,
            onOpenDirectory = onOpenDirectory,
            onClearHistory = onClearHistory,
            onShowDebug = onShowDebug,
            onAgentChange = onAgentChange,
            onModeToggle = onModeToggle,
            onToggleTreeView = onToggleTreeView,
            onShowModelConfig = onShowModelConfig,
            onShowToolConfig = onShowToolConfig,
            modifier = modifier
        )
    }
}
