package cc.unitmesh.devins.ui.compose.state

import androidx.compose.runtime.*
import cc.unitmesh.devins.ui.compose.agent.AgentType

/**
 * Desktop UI State ViewModel
 * 管理桌面端 UI 的所有状态，方便跨平台状态管理
 */
class DesktopUiState {
    // Agent Type
    var currentAgentType by mutableStateOf(AgentType.CODING)

    // Sidebar & TreeView
    var showSessionSidebar by mutableStateOf(true)

    var isTreeViewVisible by mutableStateOf(false)

    // Workspace
    var workspacePath by mutableStateOf("")

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
        showSessionSidebar = !showSessionSidebar
    }

    fun toggleTreeView() {
        isTreeViewVisible = !isTreeViewVisible
    }

    fun updateWorkspacePath(path: String) {
        workspacePath = path
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
