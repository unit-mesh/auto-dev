package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.settings.LanguageSwitcher
import cc.unitmesh.devins.ui.compose.theme.ThemeManager
import cc.unitmesh.llm.ModelConfig

/**
 * WASM 平台优化的侧边栏工具栏
 * 类似于 IDE 的项目视图，可收起、可弹出
 * - 左侧边栏布局
 * - 设置按钮在底部
 * - 可收起/展开
 */
@Composable
fun TopBarMenuWasm(
    hasHistory: Boolean,
    hasDebugInfo: Boolean,
    currentModelConfig: ModelConfig?,
    selectedAgent: String,
    availableAgents: List<String>,
    useAgentMode: Boolean = true,
    isTreeViewVisible: Boolean = false,
    // Remote Agent
    selectedAgentType: String = "Local",
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
    var isExpanded by remember { mutableStateOf(true) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 280.dp else 48.dp,
        label = "sidebar_width"
    )

    Box(modifier = modifier.fillMaxHeight()) {
        // Sidebar
        Surface(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                // Toggle button at top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = if (isExpanded) Arrangement.End else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) AutoDevComposeIcons.ChevronLeft else AutoDevComposeIcons.ChevronRight,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Main content area (scrollable)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isExpanded) {
                        // Project/Directory
                        SidebarSection(
                            title = "Project",
                            icon = AutoDevComposeIcons.Folder
                        ) {
                            SidebarButton(
                                text = "Open Directory",
                                icon = AutoDevComposeIcons.FolderOpen,
                                onClick = onOpenDirectory
                            )
                        }

                        // Agent Mode
                        SidebarSection(
                            title = "Mode",
                            icon = AutoDevComposeIcons.SmartToy
                        ) {
                            SidebarButton(
                                text = if (useAgentMode) "Agent Mode" else "Chat Mode",
                                icon = if (useAgentMode) AutoDevComposeIcons.SmartToy else AutoDevComposeIcons.Chat,
                                onClick = onModeToggle,
                                isSelected = true
                            )
                        }

                        // Agent Selection (if in agent mode)
                        if (useAgentMode) {
                            SidebarSection(
                                title = "Agent",
                                icon = AutoDevComposeIcons.SmartToy
                            ) {
                                availableAgents.forEach { agent ->
                                    SidebarButton(
                                        text = agent,
                                        icon = AutoDevComposeIcons.SmartToy,
                                        onClick = { onAgentChange(agent) },
                                        isSelected = agent == selectedAgent
                                    )
                                }
                            }

                            // Agent Type (Local/Remote)
                            SidebarSection(
                                title = "Agent Type",
                                icon = AutoDevComposeIcons.Cloud
                            ) {
                                SidebarButton(
                                    text = "Local",
                                    icon = AutoDevComposeIcons.Computer,
                                    onClick = { onAgentTypeChange("Local") },
                                    isSelected = selectedAgentType == "Local"
                                )
                                SidebarButton(
                                    text = "Remote",
                                    icon = AutoDevComposeIcons.Cloud,
                                    onClick = { onAgentTypeChange("Remote") },
                                    isSelected = selectedAgentType == "Remote"
                                )
                                if (selectedAgentType == "Remote") {
                                    SidebarButton(
                                        text = "Configure",
                                        icon = AutoDevComposeIcons.Settings,
                                        onClick = onConfigureRemote
                                    )
                                }
                            }

                            // TreeView Toggle
                            SidebarSection(
                                title = "View",
                                icon = AutoDevComposeIcons.AccountTree
                            ) {
                                SidebarButton(
                                    text = if (isTreeViewVisible) "Hide Tree" else "Show Tree",
                                    icon = AutoDevComposeIcons.AccountTree,
                                    onClick = onToggleTreeView,
                                    isSelected = isTreeViewVisible
                                )
                            }
                        }

                        // History
                        if (hasHistory) {
                            SidebarSection(
                                title = "History",
                                icon = AutoDevComposeIcons.History
                            ) {
                                SidebarButton(
                                    text = "Clear History",
                                    icon = AutoDevComposeIcons.Delete,
                                    onClick = onClearHistory
                                )
                            }
                        }

                        // Debug
                        if (hasDebugInfo) {
                            SidebarSection(
                                title = "Debug",
                                icon = AutoDevComposeIcons.BugReport
                            ) {
                                SidebarButton(
                                    text = "Show Debug",
                                    icon = AutoDevComposeIcons.BugReport,
                                    onClick = onShowDebug
                                )
                            }
                        }
                    } else {
                        // Collapsed state - show icons only
                        CollapsedIconButton(
                            icon = AutoDevComposeIcons.FolderOpen,
                            contentDescription = "Open Directory",
                            onClick = onOpenDirectory
                        )

                        if (useAgentMode) {
                            CollapsedIconButton(
                                icon = AutoDevComposeIcons.SmartToy,
                                contentDescription = "Agent Mode",
                                onClick = onModeToggle,
                                isSelected = true
                            )

                            CollapsedIconButton(
                                icon = AutoDevComposeIcons.AccountTree,
                                contentDescription = "Tree View",
                                onClick = onToggleTreeView,
                                isSelected = isTreeViewVisible
                            )
                        } else {
                            CollapsedIconButton(
                                icon = AutoDevComposeIcons.Chat,
                                contentDescription = "Chat Mode",
                                onClick = onModeToggle,
                                isSelected = true
                            )
                        }

                        if (hasHistory) {
                            CollapsedIconButton(
                                icon = AutoDevComposeIcons.Delete,
                                contentDescription = "Clear History",
                                onClick = onClearHistory
                            )
                        }

                        if (hasDebugInfo) {
                            CollapsedIconButton(
                                icon = AutoDevComposeIcons.BugReport,
                                contentDescription = "Show Debug",
                                onClick = onShowDebug
                            )
                        }
                    }
                }

                // Bottom settings section
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isExpanded) {
                        // Language Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            LanguageSwitcher()
                        }

                        // Model Configuration
                        SidebarButton(
                            text = "Model Config",
                            icon = AutoDevComposeIcons.Tune,
                            onClick = onShowModelConfig
                        )

                        // Tool Configuration
                        SidebarButton(
                            text = "Tool Config",
                            icon = AutoDevComposeIcons.Build,
                            onClick = onShowToolConfig
                        )

                        // Theme Toggle
                        val currentTheme = ThemeManager.currentTheme
                        SidebarButton(
                            text = when (currentTheme) {
                                ThemeManager.ThemeMode.LIGHT -> "Light Theme"
                                ThemeManager.ThemeMode.DARK -> "Dark Theme"
                                ThemeManager.ThemeMode.SYSTEM -> "Auto Theme"
                            },
                            icon = when (currentTheme) {
                                ThemeManager.ThemeMode.LIGHT -> AutoDevComposeIcons.LightMode
                                ThemeManager.ThemeMode.DARK -> AutoDevComposeIcons.DarkMode
                                ThemeManager.ThemeMode.SYSTEM -> AutoDevComposeIcons.AutoMode
                            },
                            onClick = {
                                ThemeManager.toggleTheme()
                            }
                        )
                    } else {
                        // Collapsed settings icons
                        CollapsedIconButton(
                            icon = AutoDevComposeIcons.Tune,
                            contentDescription = "Model Config",
                            onClick = onShowModelConfig
                        )

                        CollapsedIconButton(
                            icon = AutoDevComposeIcons.Build,
                            contentDescription = "Tool Config",
                            onClick = onShowToolConfig
                        )

                        val currentTheme = ThemeManager.currentTheme
                        CollapsedIconButton(
                            icon = when (currentTheme) {
                                ThemeManager.ThemeMode.LIGHT -> AutoDevComposeIcons.LightMode
                                ThemeManager.ThemeMode.DARK -> AutoDevComposeIcons.DarkMode
                                ThemeManager.ThemeMode.SYSTEM -> AutoDevComposeIcons.AutoMode
                            },
                            contentDescription = "Theme",
                            onClick = { ThemeManager.toggleTheme() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section content
        content()
    }
}

@Composable
private fun SidebarButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isSelected: Boolean = false
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = contentColor
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}

@Composable
private fun CollapsedIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isSelected: Boolean = false
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = contentColor
        )
    }
}
