package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.ThemeManager
import cc.unitmesh.llm.ModelConfig

/**
 * 桌面端优化的顶部工具栏
 * 使用 IconButton 风格，功能以图标形式排列
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
    // Remote Agent 相关参数
    selectedAgentType: String = "Local",
    useSessionManagement: Boolean = false,
    // Agent Task Type 相关参数 (Coding vs Code Review)
    selectedTaskAgentType: cc.unitmesh.devins.ui.compose.agent.AgentType = cc.unitmesh.devins.ui.compose.agent.AgentType.CODING,
    onTaskAgentTypeChange: (cc.unitmesh.devins.ui.compose.agent.AgentType) -> Unit = {},
    // Sidebar 相关参数
    showSessionSidebar: Boolean = true,
    onToggleSidebar: () -> Unit = {},
    onAgentTypeChange: (String) -> Unit = {},
    onConfigureRemote: () -> Unit = {},
    onSessionManagementToggle: () -> Unit = {},
    onOpenDirectory: () -> Unit,
    onClearHistory: () -> Unit,
    onShowDebug: () -> Unit,
    onAgentChange: (String) -> Unit,
    onModeToggle: () -> Unit = {},
    onToggleTreeView: () -> Unit = {},
    onShowModelConfig: () -> Unit,
    onShowToolConfig: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTheme = ThemeManager.currentTheme
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var agentMenuExpanded by remember { mutableStateOf(false) }
    var agentTypeMenuExpanded by remember { mutableStateOf(false) }
    var taskAgentTypeMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth().height(32.dp),
        shadowElevation = if (hasHistory) 1.dp else 0.dp,
        tonalElevation = if (hasHistory) 0.5.dp else 0.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Logo/Title + Sidebar Toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sidebar Toggle Button (always visible)
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

                Text(
                    text = "AutoDev",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            // Right: Action Icons (compact, 24dp buttons)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                // Task Agent Type Selector (Coding/Code Review) - Only in Agent Mode
                if (useAgentMode) {
                    Box {
                        OutlinedButton(
                            onClick = { taskAgentTypeMenuExpanded = true },
                            modifier = Modifier.height(24.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedTaskAgentType == cc.unitmesh.devins.ui.compose.agent.AgentType.CODE_REVIEW) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Icon(
                                imageVector = if (selectedTaskAgentType == cc.unitmesh.devins.ui.compose.agent.AgentType.CODE_REVIEW) {
                                    AutoDevComposeIcons.RateReview
                                } else {
                                    AutoDevComposeIcons.Code
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = selectedTaskAgentType.getDisplayName(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        DropdownMenu(
                            expanded = taskAgentTypeMenuExpanded,
                            onDismissRequest = { taskAgentTypeMenuExpanded = false }
                        ) {
                            Text(
                                text = "Task Type",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            HorizontalDivider()

                            // Coding Agent
                            DropdownMenuItem(
                                text = { Text("Coding Agent") },
                                onClick = {
                                    onTaskAgentTypeChange(cc.unitmesh.devins.ui.compose.agent.AgentType.CODING)
                                    taskAgentTypeMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = AutoDevComposeIcons.Code,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (selectedTaskAgentType == cc.unitmesh.devins.ui.compose.agent.AgentType.CODING) {
                                        Icon(
                                            imageVector = AutoDevComposeIcons.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )

                            // Code Review Agent
                            DropdownMenuItem(
                                text = { Text("Code Review") },
                                onClick = {
                                    onTaskAgentTypeChange(cc.unitmesh.devins.ui.compose.agent.AgentType.CODE_REVIEW)
                                    taskAgentTypeMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = AutoDevComposeIcons.RateReview,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (selectedTaskAgentType == cc.unitmesh.devins.ui.compose.agent.AgentType.CODE_REVIEW) {
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

                // Agent Type Selector (Local/Remote) - Only in Agent Mode
                if (useAgentMode) {
                    Box {
                        OutlinedButton(
                            onClick = { agentTypeMenuExpanded = true },
                            modifier = Modifier.height(24.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedAgentType == "Remote") {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Icon(
                                imageVector = if (selectedAgentType == "Remote") {
                                    AutoDevComposeIcons.Cloud
                                } else {
                                    AutoDevComposeIcons.Computer
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = selectedAgentType,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        DropdownMenu(
                            expanded = agentTypeMenuExpanded,
                            onDismissRequest = { agentTypeMenuExpanded = false }
                        ) {
                            Text(
                                text = "Agent Type",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            HorizontalDivider()

                            // Local Agent
                            DropdownMenuItem(
                                text = { Text("Local") },
                                onClick = {
                                    onAgentTypeChange("Local")
                                    agentTypeMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = AutoDevComposeIcons.Computer,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (selectedAgentType == "Local") {
                                        Icon(
                                            imageVector = AutoDevComposeIcons.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )

                            // Remote Agent
                            DropdownMenuItem(
                                text = { Text("Remote") },
                                onClick = {
                                    onAgentTypeChange("Remote")
                                    agentTypeMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = AutoDevComposeIcons.Cloud,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (selectedAgentType == "Remote") {
                                        Icon(
                                            imageVector = AutoDevComposeIcons.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )

                            HorizontalDivider()

                            // Configure Remote Server
                            if (selectedAgentType == "Remote") {
                                DropdownMenuItem(
                                    text = { Text("Configure Server...") },
                                    onClick = {
                                        onConfigureRemote()
                                        agentTypeMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = AutoDevComposeIcons.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )

                                // Session Management Toggle
                                DropdownMenuItem(
                                    text = { Text(if (useSessionManagement) "Agent Mode" else "Session Manager") },
                                    onClick = {
                                        onSessionManagementToggle()
                                        agentTypeMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (useSessionManagement) AutoDevComposeIcons.Custom.AI else AutoDevComposeIcons.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (useSessionManagement) {
                                            Icon(
                                                imageVector = AutoDevComposeIcons.Check,
                                                contentDescription = "Active",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
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

                // Debug Info
                if (hasDebugInfo) {
                    IconButton(
                        onClick = onShowDebug,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.BugReport,
                            contentDescription = "Debug Info",
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
    }
}
