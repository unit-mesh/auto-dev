package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cc.unitmesh.agent.AgentType
import cc.unitmesh.agent.Platform
import cc.unitmesh.llm.ModelConfig

/**
 * 平台自适应的顶部工具栏
 * - Android: 使用 Dropdown Menu 风格（移动端优化）
 * - WASM: 使用左侧可收起侧边栏风格（Web 优化）
 * - Desktop (JVM): 使用 Window Tab 风格（桌面端优化，类似 Chrome）
 */
@Composable
fun TopBarMenu(
    hasHistory: Boolean,
    hasDebugInfo: Boolean,
    currentModelConfig: ModelConfig?,
    selectedAgent: String,
    availableAgents: List<String>,
    isTreeViewVisible: Boolean = false,
    // 统一的 Agent 类型（LOCAL, CODING, CODE_REVIEW, REMOTE）
    currentAgentType: AgentType = AgentType.CODING,
    onAgentTypeChange: (AgentType) -> Unit = {},
    useSessionManagement: Boolean = false, // Session Management mode (仅 Remote 有效)
    // Sidebar 相关参数
    showSessionSidebar: Boolean = false,
    onToggleSidebar: () -> Unit = {},
    onConfigureRemote: () -> Unit = {},
    onSessionManagementToggle: () -> Unit = {},
    onOpenDirectory: () -> Unit,
    onClearHistory: () -> Unit,
    onModelConfigChange: (ModelConfig) -> Unit,
    onAgentChange: (String) -> Unit,
    onModeToggle: () -> Unit = {},
    onToggleTreeView: () -> Unit = {},
    onShowModelConfig: () -> Unit,
    onShowToolConfig: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (Platform.isAndroid) {
        TopBarMenuMobile(
            hasHistory = hasHistory,
            hasDebugInfo = hasDebugInfo,
            currentModelConfig = currentModelConfig,
            selectedAgent = selectedAgent,
            availableAgents = availableAgents,
            isTreeViewVisible = isTreeViewVisible,
            currentAgentType = currentAgentType,
            onAgentTypeChange = onAgentTypeChange,
            useSessionManagement = useSessionManagement,
            onConfigureRemote = onConfigureRemote,
            onSessionManagementToggle = onSessionManagementToggle,
            onOpenDirectory = onOpenDirectory,
            onClearHistory = onClearHistory,
            onAgentChange = onAgentChange,
            onModeToggle = onModeToggle,
            onToggleTreeView = onToggleTreeView,
            onShowModelConfig = onShowModelConfig,
            onShowToolConfig = onShowToolConfig,
            modifier = modifier
        )
    } else if (Platform.isWasm) {
        TopBarMenuDesktop(
            hasHistory = hasHistory,
            hasDebugInfo = hasDebugInfo,
            currentModelConfig = currentModelConfig,
            selectedAgent = selectedAgent,
            availableAgents = availableAgents,
            isTreeViewVisible = isTreeViewVisible,
            currentAgentType = currentAgentType,
            onAgentTypeChange = onAgentTypeChange,
            useSessionManagement = useSessionManagement,
            showSessionSidebar = showSessionSidebar,
            onToggleSidebar = onToggleSidebar,
            onConfigureRemote = onConfigureRemote,
            onSessionManagementToggle = onSessionManagementToggle,
            onOpenDirectory = onOpenDirectory,
            onClearHistory = onClearHistory,
            onAgentChange = onAgentChange,
            onModeToggle = onModeToggle,
            onToggleTreeView = onToggleTreeView,
            onShowModelConfig = onShowModelConfig,
            onShowToolConfig = onShowToolConfig,
            modifier = modifier
        )
    }
}
