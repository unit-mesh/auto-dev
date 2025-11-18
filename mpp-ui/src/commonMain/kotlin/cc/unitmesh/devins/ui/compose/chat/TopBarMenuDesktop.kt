package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.AgentType
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.ThemeManager
import cc.unitmesh.llm.ModelConfig

/**
 * 桌面端优化的顶部工具栏
 * 使用 Window Tab 风格，功能以标签页形式排列（类似 Chrome）
 */
@Composable
fun TopBarMenuDesktop(
    hasHistory: Boolean,
    hasDebugInfo: Boolean,
    currentModelConfig: ModelConfig?,
    selectedAgent: String,
    availableAgents: List<String>,
    useAgentMode: Boolean = true,
    isTreeViewVisible: Boolean = false,
    // 统一的 Agent 类型（LOCAL, CODING, CODE_REVIEW, REMOTE）
    currentAgentType: AgentType = AgentType.CODING,
    onAgentTypeChange: (AgentType) -> Unit = {},
    useSessionManagement: Boolean = false,
    // Sidebar 相关参数
    showSessionSidebar: Boolean = true,
    onToggleSidebar: () -> Unit = {},
    onConfigureRemote: () -> Unit = {},
    onSessionManagementToggle: () -> Unit = {},
    onOpenDirectory: () -> Unit,
    onClearHistory: () -> Unit,
    onAgentChange: (String) -> Unit,
    onModeToggle: () -> Unit = {},
    onToggleTreeView: () -> Unit = {},
    onShowModelConfig: () -> Unit,
    onShowToolConfig: () -> Unit = {},
    onShowGitClone: () -> Unit = {}, // Wasm Git Clone
    modifier: Modifier = Modifier
) {
    val currentTheme = ThemeManager.currentTheme
    var agentMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth().height(48.dp),
        shadowElevation = if (hasHistory) 1.dp else 0.dp,
        tonalElevation = if (hasHistory) 0.5.dp else 0.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Top Row: Sidebar Toggle + Agent Type Tabs + Settings
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Sidebar Toggle + Agent Type Tabs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sidebar Toggle Button
                    IconButton(
                        onClick = onToggleSidebar,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showSessionSidebar) AutoDevComposeIcons.MenuOpen else AutoDevComposeIcons.Menu,
                            contentDescription = if (showSessionSidebar) "Hide Sidebar" else "Show Sidebar",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AgentType.entries.forEach { type ->
                        AgentTypeTab(
                            type = type,
                            isSelected = type == currentAgentType,
                            onClick = { onAgentTypeChange(type) }
                        )
                    }

                    if (currentAgentType == AgentType.REMOTE) {
                        IconButton(
                            onClick = onConfigureRemote,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Settings,
                                contentDescription = "Configure Remote Server",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Model Config Button
                    IconButton(
                        onClick = onShowModelConfig,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Settings,
                            contentDescription = "Model Configuration",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Tool Config Button
                    IconButton(
                        onClick = onShowToolConfig,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Build,
                            contentDescription = "Tool Configuration",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Agent Selector with Dropdown
                    Box {
                        IconButton(
                            onClick = { agentMenuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.SmartToy,
                                contentDescription = "Select Agent",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = agentMenuExpanded,
                            onDismissRequest = { agentMenuExpanded = false }
                        ) {
                            Text(
                                text = "Agent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            HorizontalDivider()
                            availableAgents.forEach { agent ->
                                DropdownMenuItem(
                                    text = { Text(agent) },
                                    onClick = {
                                        onAgentChange(agent)
                                        agentMenuExpanded = false
                                    },
                                    trailingIcon = {
                                        if (agent == selectedAgent) {
                                            Icon(
                                                imageVector = AutoDevComposeIcons.Check,
                                                contentDescription = "Selected",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Mode Toggle
                    IconButton(
                        onClick = onModeToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (useAgentMode) AutoDevComposeIcons.Custom.AI else AutoDevComposeIcons.Chat,
                            contentDescription = if (useAgentMode) "Switch to Chat Mode" else "Switch to Agent Mode",
                            tint = if (useAgentMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // New Chat
                    if (hasHistory) {
                        IconButton(
                            onClick = onClearHistory,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = AutoDevComposeIcons.Add,
                                contentDescription = "New Chat",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Project Explorer Toggle (只在 Agent 模式下显示，放在最右边)
                    if (useAgentMode) {
                        IconButton(
                            onClick = onToggleTreeView,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isTreeViewVisible) AutoDevComposeIcons.MenuOpen else AutoDevComposeIcons.Menu,
                                contentDescription = if (isTreeViewVisible) "Hide Explorer" else "Show Explorer",
                                tint =
                                    if (isTreeViewVisible) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Row: Divider (to separate tabs visually)
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Agent Type Tab (类似 Chrome 标签页)
 */
@Composable
private fun AgentTypeTab(
    type: AgentType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier.height(28.dp),
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 8.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        ),
        color = backgroundColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (type) {
                    AgentType.REMOTE -> AutoDevComposeIcons.Cloud
                    AgentType.CODE_REVIEW -> AutoDevComposeIcons.RateReview
                    AgentType.CODING -> AutoDevComposeIcons.Code
                    AgentType.LOCAL_CHAT -> AutoDevComposeIcons.Chat
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = type.getDisplayName(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}



