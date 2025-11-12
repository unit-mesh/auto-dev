package cc.unitmesh.devins.ui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cc.unitmesh.devins.ui.app.UnifiedApp

/**
 * SessionDemoMain - 会话管理演示应用
 * 支持用户登录、项目管理、任务管理和 AI Agent 执行
 *
 * 使用统一的侧边栏布局：
 * - 顶部：新建按钮 + 本地 Chat
 * - 中间：Sessions/Projects 列表
 * - 底部：Settings/Profile/退出
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AutoDev - 会话管理"
    ) {
        UnifiedApp(
            serverUrl = "http://localhost:8080",
            onOpenLocalChat = null // 这个 Demo 不需要本地 Chat
        )
    }
}

