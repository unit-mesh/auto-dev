package cc.unitmesh.devins.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.devins.ui.compose.AutoDevApp
import cc.unitmesh.devins.ui.compose.state.rememberDesktopUiState
import cc.unitmesh.devins.ui.desktop.AutoDevMenuBar
import cc.unitmesh.devins.ui.desktop.AutoDevTray
import cc.unitmesh.devins.ui.desktop.DesktopWindowLayout
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

fun main(args: Array<String>) {
    AutoDevLogger.initialize()

    AutoDevLogger.info("AutoDevMain") { "🚀 AutoDev Desktop starting..." }
    AutoDevLogger.info("AutoDevMain") { "📁 Log files location: ${AutoDevLogger.getLogDirectory()}" }

    // 支持通过命令行参数指定模式：--mode=remote 或 --mode=local
    val mode = args.find { it.startsWith("--mode=") }?.substringAfter("--mode=") ?: "auto"

    application {
        var isWindowVisible by remember { mutableStateOf(true) }
        var triggerFileChooser by remember { mutableStateOf(false) }

        // KCEF 初始化状态
        var kcefInitialized by remember { mutableStateOf(false) }
        var kcefDownloading by remember { mutableStateOf(0F) }
        var kcefError by remember { mutableStateOf<String?>(null) }

        // 使用 ViewModel 管理 UI 状态
        val uiState = rememberDesktopUiState()

        // 初始化 KCEF（用于 WebView/Mermaid 渲染）
        // 从 v1.9.40 开始，如果使用 JetBrains Runtime JDK，可以直接使用 bundled JCEF，无需下载
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    AutoDevLogger.info("AutoDevMain") { "🌐 Initializing KCEF for WebView support..." }
                    KCEF.init(builder = {
                        // 不指定 installDir，让 KCEF 自动检测并使用 JBR 的 bundled JCEF
                        // 如果找不到 JBR，才会下载到默认位置
                        progress {
                            onDownloading {
                                kcefDownloading = max(it, 0F)
                                if (kcefDownloading > 0F && kcefDownloading % 10 == 0F) {
                                    AutoDevLogger.info("AutoDevMain") { "🌐 Preparing WebView: ${kcefDownloading.toInt()}%" }
                                }
                            }
                            onInitialized {
                                val javaHome = System.getProperty("java.home", "unknown")
                                val isJbr = javaHome.contains("jbr", ignoreCase = true) ||
                                            javaHome.contains("jetbrains", ignoreCase = true)
                                if (isJbr) {
                                    AutoDevLogger.info("AutoDevMain") { "✅ KCEF initialized using JBR bundled JCEF (no download needed)" }
                                } else {
                                    AutoDevLogger.info("AutoDevMain") { "✅ KCEF initialized successfully" }
                                }
                                kcefInitialized = true
                            }
                        }
                        settings {
                            cachePath = File("kcef-cache").absolutePath
                        }
                    }, onError = {
                        val errorMsg = "KCEF initialization failed: ${it?.message}"
                        AutoDevLogger.error("AutoDevMain") { errorMsg }
                        kcefError = errorMsg
                        // 即使 KCEF 失败也允许应用启动（只是 WebView 功能不可用）
                        kcefInitialized = true
                    }, onRestartRequired = {
                        AutoDevLogger.warn("AutoDevMain") { "⚠️ KCEF requires restart" }
                    })
                } catch (e: Exception) {
                    AutoDevLogger.error("AutoDevMain") { "❌ KCEF initialization error: ${e.message}" }
                    kcefError = e.message
                    kcefInitialized = true // 允许应用继续启动
                }
            }
        }

        // 清理 KCEF
        DisposableEffect(Unit) {
            onDispose {
                try {
                    AutoDevLogger.info("AutoDevMain") { "🧹 Disposing KCEF..." }
                    KCEF.disposeBlocking()
                } catch (e: Exception) {
                    AutoDevLogger.error("AutoDevMain") { "Failed to dispose KCEF: ${e.message}" }
                }
            }
        }

        val windowState =
            rememberWindowState(
                width = 1200.dp,
                height = 800.dp
            )

        AutoDevTray(
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
                // 显示 KCEF 初始化进度（使用 JBR 时瞬间完成）
                if (!kcefInitialized) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            if (kcefDownloading > 0F) {
                                Text("Preparing: ${kcefDownloading.toInt()}%")
                            } else {
                                Text("Initializing...")
                            }
                            Text(
                                "(First launch may take a moment)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    return@Window
                }

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
                            }
                        )
                    }
                }
            }
        }
    }
}
