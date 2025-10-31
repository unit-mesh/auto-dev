package cc.unitmesh.devins.ui.compose.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.Platform
import cc.unitmesh.llm.ModelConfig

/**
 * 移动端优化的顶部工具栏
 * 左侧：标题/Logo
 * 右侧：菜单（包含所有设置和操作）
 */
@Composable
fun TopBarMenu(
    hasHistory: Boolean,
    hasDebugInfo: Boolean,
    currentModelConfig: ModelConfig?,
    availableConfigs: List<ModelConfig>,
    selectedAgent: String,
    availableAgents: List<String>,
    onOpenDirectory: () -> Unit,
    onClearHistory: () -> Unit,
    onShowDebug: () -> Unit,
    onModelConfigChange: (ModelConfig) -> Unit,
    onAgentChange: (String) -> Unit,
    onShowModelConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAndroid = Platform.isAndroid
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = if (hasHistory) 4.dp else 0.dp,
        tonalElevation = if (hasHistory) 2.dp else 0.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isAndroid) 16.dp else 32.dp,
                    vertical = if (isAndroid) 12.dp else 16.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：标题
            Text(
                text = if (isAndroid) "AutoDev" else "AutoDev - DevIn AI",
                style = if (isAndroid) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // 右侧：菜单按钮
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
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
                                imageVector = Icons.Default.Settings,
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
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (agentMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
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
                                                imageVector = Icons.Default.Check,
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

                    // 3. Open Directory
                    DropdownMenuItem(
                        text = { Text("Open Project") },
                        onClick = {
                            menuExpanded = false
                            onOpenDirectory()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    // 4. New Chat
                    if (hasHistory) {
                        DropdownMenuItem(
                            text = { Text("New Chat") },
                            onClick = {
                                menuExpanded = false
                                onClearHistory()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    // 5. Debug Info
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
                                    imageVector = Icons.Outlined.BugReport,
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

