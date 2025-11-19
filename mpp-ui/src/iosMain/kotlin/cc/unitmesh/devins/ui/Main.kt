package cc.unitmesh.devins.ui

import androidx.compose.ui.window.ComposeUIViewController
import cc.unitmesh.devins.ui.compose.PlatformAutoDevApp
import platform.UIKit.UIViewController

/**
 * iOS 应用入口点
 *
 * 这个函数会被 iOS 应用调用来创建 Compose UI 视图控制器
 *
 * 在 Swift 中使用:
 * ```swift
 * import AutoDevUI
 *
 * let viewController = MainKt.MainViewController()
 * ```
 */
fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        PlatformAutoDevApp()
    }
}

