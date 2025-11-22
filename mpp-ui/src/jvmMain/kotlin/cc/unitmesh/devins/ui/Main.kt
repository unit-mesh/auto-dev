package cc.unitmesh.devins.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.devins.ui.compose.AutoDevApp
import cc.unitmesh.devins.ui.compose.state.rememberDesktopUiState
import cc.unitmesh.devins.ui.desktop.AutoDevMenuBar
import cc.unitmesh.devins.ui.desktop.AutoDevTray
import cc.unitmesh.devins.ui.desktop.DesktopWindowLayout

fun main(args: Array<String>) {
    AutoDevLogger.initialize()
    AutoDevLogger.info("AutoDevMain") { "🚀 AutoDev Desktop starting..." }
    AutoDevLogger.info("AutoDevMain") { "📁 Log files location: ${AutoDevLogger.getLogDirectory()}" }

    val mode = args.find { it.startsWith("--mode=") }?.substringAfter("--mode=") ?: "auto"

    application {
        val trayState = rememberTrayState()
        var isWindowVisible by remember { mutableStateOf(true) }
        var triggerFileChooser by remember { mutableStateOf(false) }

        val uiState = rememberDesktopUiState()

        val windowState =
            rememberWindowState(
                width = 1200.dp,
                height = 800.dp
            )

        AutoDevTray(
            trayState = trayState,
            isWindowVisible = isWindowVisible,
            onShowWindow = { isWindowVisible = true },
            onExit = ::exitApplication
        )

        if (isWindowVisible) {
            Window(
                onCloseRequest = { isWindowVisible = false },
                title = "AutoDev Desktop",
                state = windowState,
                undecorated = true,
            ) {
                DesktopWindowLayout(
                    onMinimize = { windowState.isMinimized = true },
                    onMaximize = {
                        windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Maximized
                        }
                    },
                    onClose = { isWindowVisible = false },
                    titleBarContent = {
                        cc.unitmesh.devins.ui.compose.chat.DesktopTitleBarTabs(
                            currentAgentType = uiState.currentAgentType,
                            onAgentTypeChange = { newType ->
                                uiState.updateAgentType(newType)
                                AutoDevLogger.info("AutoDevMain") { "🔄 Switch Agent Type: $newType" }
                            },
                            onConfigureRemote = {
                                uiState.showRemoteConfigDialog = true
                                AutoDevLogger.info("AutoDevMain") { "☁️ Configure Remote" }
                            },
                            onDoubleClick = {
                                windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                    WindowPlacement.Floating
                                } else {
                                    WindowPlacement.Maximized
                                }
                            }
                        )
                    }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AutoDevMenuBar(
                            onOpenFile = {
                                triggerFileChooser = true
                                AutoDevLogger.info("AutoDevMain") { "Open File menu clicked" }
                            },
                            onExit = ::exitApplication
                        )

                        AutoDevApp(
                            triggerFileChooser = triggerFileChooser,
                            onFileChooserHandled = { triggerFileChooser = false },
                            initialMode = mode,
                            showTopBarInContent = false,
                            initialAgentType = uiState.currentAgentType,
                            initialTreeViewVisible = uiState.isTreeViewVisible,
                            onAgentTypeChanged = { type ->
                                uiState.updateAgentType(type)
                            },
                            onTreeViewVisibilityChanged = { visible ->
                                // 已由全局状态管理，无需额外操作
                            },
                            onSidebarVisibilityChanged = { visible ->
                                // 已由全局状态管理，无需额外操作
                            },
                            onWorkspacePathChanged = { path ->
                                uiState.updateWorkspacePath(path)
                            },
                            onNotification = { title, message ->
                                trayState.sendNotification(androidx.compose.ui.window.Notification(title, message))
                            }
                        )
                    }
                }
            }
        }
    }
}
