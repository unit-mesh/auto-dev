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
    onAgentTypeChange: (String) -> Unit = {},
    onConfigureRemote: () -> Unit = {},
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
    // 根据平台选择合适的 UI 风格
    if (Platform.isWasm) {
        // WASM: 使用左侧可收起侧边栏风格
        TopBarMenuWasm(
            hasHistory = hasHistory,
            hasDebugInfo = hasDebugInfo,
            currentModelConfig = currentModelConfig,
            selectedAgent = selectedAgent,
            availableAgents = availableAgents,
            useAgentMode = useAgentMode,
            isTreeViewVisible = isTreeViewVisible,
            selectedAgentType = selectedAgentType,
            onAgentTypeChange = onAgentTypeChange,
            onConfigureRemote = onConfigureRemote,
            onOpenDirectory = onOpenDirectory,
            onClearHistory = onClearHistory,
            onShowDebug = onShowDebug,
            onModelConfigChange = onModelConfigChange,
            onAgentChange = onAgentChange,
            onModeToggle = onModeToggle,
            onToggleTreeView = onToggleTreeView,
            onShowModelConfig = onShowModelConfig,
            onShowToolConfig = onShowToolConfig,
            modifier = modifier
        )
    } else if (Platform.isAndroid) {
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
            onAgentTypeChange = onAgentTypeChange,
            onConfigureRemote = onConfigureRemote,
            onOpenDirectory = onOpenDirectory,
            onClearHistory = onClearHistory,
            onShowDebug = onShowDebug,
            onModelConfigChange = onModelConfigChange,
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
            onAgentTypeChange = onAgentTypeChange,
            onConfigureRemote = onConfigureRemote,
            onOpenDirectory = onOpenDirectory,
            onClearHistory = onClearHistory,
            onShowDebug = onShowDebug,
            onModelConfigChange = onModelConfigChange,
            onAgentChange = onAgentChange,
            onModeToggle = onModeToggle,
            onToggleTreeView = onToggleTreeView,
            onShowModelConfig = onShowModelConfig,
            onShowToolConfig = onShowToolConfig,
            modifier = modifier
        )
    }
}
