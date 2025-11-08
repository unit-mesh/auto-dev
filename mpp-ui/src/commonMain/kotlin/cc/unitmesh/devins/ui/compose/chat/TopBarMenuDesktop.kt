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
    val currentTheme = ThemeManager.currentTheme
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var agentMenuExpanded by remember { mutableStateOf(false) }

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
                    .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Logo/Title
            Text(
                text = "AutoDev",
                style = MaterialTheme.typography.titleLarge
            )

            // Right: Action Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Switcher
                LanguageSwitcher(
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Model Config Button
                IconButton(
                    onClick = onShowModelConfig,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Settings,
                        contentDescription = "Model Configuration",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Tool Config Button
                IconButton(
                    onClick = onShowToolConfig,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Build,
                        contentDescription = "Tool Configuration",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Agent Selector with Dropdown
                Box {
                    IconButton(
                        onClick = { agentMenuExpanded = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.SmartToy,
                            contentDescription = "Select Agent",
                            tint = MaterialTheme.colorScheme.onSurface
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
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (useAgentMode) AutoDevComposeIcons.Custom.AI else AutoDevComposeIcons.Chat,
                        contentDescription = if (useAgentMode) "Switch to Chat Mode" else "Switch to Agent Mode",
                        tint = if (useAgentMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Theme Switcher with Dropdown
                Box {
                    IconButton(
                        onClick = { themeMenuExpanded = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector =
                                when (currentTheme) {
                                    ThemeManager.ThemeMode.LIGHT -> AutoDevComposeIcons.LightMode
                                    ThemeManager.ThemeMode.DARK -> AutoDevComposeIcons.DarkMode
                                    ThemeManager.ThemeMode.SYSTEM -> AutoDevComposeIcons.Brightness4
                                },
                            contentDescription = "Theme",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = themeMenuExpanded,
                        onDismissRequest = { themeMenuExpanded = false }
                    ) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider()
                        ThemeManager.ThemeMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(ThemeManager.getThemeDisplayName(mode)) },
                                onClick = {
                                    ThemeManager.setTheme(mode)
                                    themeMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector =
                                            when (mode) {
                                                ThemeManager.ThemeMode.LIGHT -> AutoDevComposeIcons.LightMode
                                                ThemeManager.ThemeMode.DARK -> AutoDevComposeIcons.DarkMode
                                                ThemeManager.ThemeMode.SYSTEM -> AutoDevComposeIcons.Brightness4
                                            },
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
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

                // Open Directory
                IconButton(
                    onClick = onOpenDirectory,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.FolderOpen,
                        contentDescription = "Open Project",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // New Chat
                if (hasHistory) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.Add,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Debug Info
                if (hasDebugInfo) {
                    IconButton(
                        onClick = onShowDebug,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = AutoDevComposeIcons.BugReport,
                            contentDescription = "Debug Info",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Project Explorer Toggle (只在 Agent 模式下显示，放在最右边)
                if (useAgentMode) {
                    IconButton(
                        onClick = onToggleTreeView,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isTreeViewVisible) AutoDevComposeIcons.MenuOpen else AutoDevComposeIcons.Menu,
                            contentDescription = if (isTreeViewVisible) "Hide Explorer" else "Show Explorer",
                            tint =
                                if (isTreeViewVisible) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                        )
                    }
                }
            }
        }
    }
}
