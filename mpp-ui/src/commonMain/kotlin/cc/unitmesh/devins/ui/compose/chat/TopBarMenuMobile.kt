package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.settings.LanguageSwitcher
import cc.unitmesh.devins.ui.compose.theme.ThemeManager
import cc.unitmesh.llm.ModelConfig

/**
 * 移动端优化的顶部工具栏
 * 使用 Dropdown Menu 风格，所有功能集中在一个菜单中
 */
@Composable
fun TopBarMenuMobile(
    hasHistory: Boolean,
    hasDebugInfo: Boolean,
    currentModelConfig: ModelConfig?,
    selectedAgent: String,
    availableAgents: List<String>,
    useAgentMode: Boolean = true,
    isTreeViewVisible: Boolean = false,
    selectedAgentType: String = "Local",
    useSessionManagement: Boolean = false,
    // Agent Task Type 相关参数
    selectedTaskAgentType: cc.unitmesh.devins.ui.compose.agent.AgentType = cc.unitmesh.devins.ui.compose.agent.AgentType.CODING,
    onTaskAgentTypeChange: (cc.unitmesh.devins.ui.compose.agent.AgentType) -> Unit = {},
    onOpenDirectory: () -> Unit,
    onClearHistory: () -> Unit,
    onShowDebug: () -> Unit,
    onModelConfigChange: (ModelConfig) -> Unit,
    onAgentChange: (String) -> Unit,
    onModeToggle: () -> Unit = {},
    onToggleTreeView: () -> Unit = {},
    onAgentTypeChange: (String) -> Unit = {},
    onConfigureRemote: () -> Unit = {},
    onSessionManagementToggle: () -> Unit = {},
    onShowModelConfig: () -> Unit,
    onShowToolConfig: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTheme = ThemeManager.currentTheme
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = if (hasHistory) 4.dp else 0.dp,
        tonalElevation = if (hasHistory) 2.dp else 0.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo/Title
            Text(
                text = "AutoDev",
                style = MaterialTheme.typography.titleMedium
            )

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    // 1. 当前模型显示 + 配置
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    "Model",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    currentModelConfig?.let { "${it.provider.displayName} / ${it.modelName}" }
                                        ?: "Configure Model",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onShowModelConfig()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = AutoDevComposeIcons.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    HorizontalDivider()

                    // Tool Configuration
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Tool Configuration",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onShowToolConfig()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = AutoDevComposeIcons.Build,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    HorizontalDivider()

                    // 2. Agent 子菜单
                    var agentMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        "Agent",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        selectedAgent,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            onClick = { agentMenuExpanded = !agentMenuExpanded },
                            leadingIcon = {
                                Icon(
                                    imageVector = AutoDevComposeIcons.SmartToy,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (agentMenuExpanded) AutoDevComposeIcons.KeyboardArrowUp else AutoDevComposeIcons.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        // Agent 子菜单
                        DropdownMenu(
                            expanded = agentMenuExpanded,
                            onDismissRequest = { agentMenuExpanded = false }
                        ) {
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

                    HorizontalDivider()

                    // 3. Agent Type 子菜单 (只在 Agent 模式下显示)
                    if (useAgentMode) {
                        var agentTypeMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            "Agent Type",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            selectedAgentType,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                },
                                onClick = { agentTypeMenuExpanded = !agentTypeMenuExpanded },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (selectedAgentType == "Remote") AutoDevComposeIcons.Cloud else AutoDevComposeIcons.Computer,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (agentTypeMenuExpanded) AutoDevComposeIcons.KeyboardArrowUp else AutoDevComposeIcons.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )

                            // Agent Type 子菜单
                            DropdownMenu(
                                expanded = agentTypeMenuExpanded,
                                onDismissRequest = { agentTypeMenuExpanded = false }
                            ) {
                                listOf("Local", "Remote").forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            onAgentTypeChange(type)
                                            agentTypeMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (type == "Remote") AutoDevComposeIcons.Cloud else AutoDevComposeIcons.Computer,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (type == selectedAgentType) {
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

                                // Configure Server (只在选择 Remote 时显示)
                                if (selectedAgentType == "Remote") {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Configure Server",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        onClick = {
                                            agentTypeMenuExpanded = false
                                            onConfigureRemote()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = AutoDevComposeIcons.Settings,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider()
                    }

                    // 4. 模式切换
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    "Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    if (useAgentMode) "Coding Agent" else "Chat Mode",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onModeToggle()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (useAgentMode) AutoDevComposeIcons.SmartToy else AutoDevComposeIcons.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    HorizontalDivider()

                    // 5. 主题切换子菜单
                    var themeMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        "Theme",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        ThemeManager.getCurrentThemeDisplayName(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            onClick = { themeMenuExpanded = !themeMenuExpanded },
                            leadingIcon = {
                                Icon(
                                    imageVector =
                                        when (currentTheme) {
                                            ThemeManager.ThemeMode.LIGHT -> AutoDevComposeIcons.LightMode
                                            ThemeManager.ThemeMode.DARK -> AutoDevComposeIcons.DarkMode
                                            ThemeManager.ThemeMode.SYSTEM -> AutoDevComposeIcons.Brightness4
                                        },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (themeMenuExpanded) AutoDevComposeIcons.KeyboardArrowUp else AutoDevComposeIcons.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        // 主题子菜单
                        DropdownMenu(
                            expanded = themeMenuExpanded,
                            onDismissRequest = { themeMenuExpanded = false }
                        ) {
                            ThemeManager.ThemeMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(ThemeManager.getThemeDisplayName(mode)) },
                                    onClick = {
                                        ThemeManager.setTheme(mode)
                                        themeMenuExpanded = false
                                    },
                                    trailingIcon = {
                                        if (mode == currentTheme) {
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

                    HorizontalDivider()

                    // 6. Language Switcher
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LanguageSwitcher()
                            }
                        },
                        onClick = { /* LanguageSwitcher handles its own clicks */ }
                    )

                    HorizontalDivider()

                    // 7. Project Explorer Toggle (只在 Agent 模式下显示)
                    if (useAgentMode) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        "Project Explorer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        if (isTreeViewVisible) "Hide Sidebar" else "Show Sidebar",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleTreeView()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isTreeViewVisible) AutoDevComposeIcons.MenuOpen else AutoDevComposeIcons.Menu,
                                    contentDescription = null,
                                    tint =
                                        if (isTreeViewVisible) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    // 8. Open Directory
                    DropdownMenuItem(
                        text = { Text("Open Project") },
                        onClick = {
                            menuExpanded = false
                            onOpenDirectory()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = AutoDevComposeIcons.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    // 9. New Chat
                    if (hasHistory) {
                        DropdownMenuItem(
                            text = { Text("New Chat") },
                            onClick = {
                                menuExpanded = false
                                onClearHistory()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = AutoDevComposeIcons.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    // 10. Debug Info
                    if (hasDebugInfo) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Debug Info") },
                            onClick = {
                                menuExpanded = false
                                onShowDebug()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = AutoDevComposeIcons.BugReport,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
