package cc.unitmesh.devins.ui.compose

import androidx.compose.runtime.Composable

/**
 * 平台特定的 AutoDevApp 入口
 * 
 * 使用 expect/actual 模式支持不同平台的 UI 实现：
 * - Android: BottomNavigation + Drawer
 * - Desktop: SessionSidebar + TopBar
 * - WASM: Minimal UI
 */
@Composable
expect fun PlatformAutoDevApp(
    triggerFileChooser: Boolean = false,
    onFileChooserHandled: () -> Unit = {},
    initialMode: String = "auto"
)

