package cc.unitmesh.devins.ui.compose.state

import androidx.compose.runtime.*
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.ui.state.UIStateManager

/**
 * Desktop UI State ViewModel
 * 管理桌面端 UI 的所有状态，同步全局 UIStateManager
 */
class DesktopUiState {
    // Agent Type
    var currentAgentType by mutableStateOf(AgentType.CODING)

    // Sidebar & TreeView - 从全局状态读取
    val showSessionSidebar: Boolean
        get() = UIStateManager.isSessionSidebarVisible.value

    val isTreeViewVisible: Boolean
        get() = UIStateManager.isTreeViewVisible.value

    val workspacePath: String
        get() = UIStateManager.workspacePath.value

    // Agent
    var selectedAgent by mutableStateOf("Default")

    var availableAgents by mutableStateOf(listOf("Default"))

    // Mode
    var useAgentMode by mutableStateOf(true)

    // Dialogs
    var showModelConfigDialog by mutableStateOf(false)

    var showToolConfigDialog by mutableStateOf(false)

    var showRemoteConfigDialog by mutableStateOf(false)

    // Actions
    fun updateAgentType(type: AgentType) {
        currentAgentType = type
    }

    fun toggleSessionSidebar() {
        UIStateManager.toggleSessionSidebar()
    }

    fun toggleTreeView() {
        UIStateManager.toggleTreeView()
    }

    fun updateWorkspacePath(path: String) {
        UIStateManager.setWorkspacePath(path)
    }

    fun updateSelectedAgent(agent: String) {
        selectedAgent = agent
    }

    fun updateAvailableAgents(agents: List<String>) {
        availableAgents = agents
    }

    fun toggleAgentMode() {
        useAgentMode = !useAgentMode
    }
}

/**
 * Remember DesktopUiState across recompositions
 */
@Composable
fun rememberDesktopUiState(): DesktopUiState {
    return remember { DesktopUiState() }
}
