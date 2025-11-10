//
//  ComposeView.swift
//  AutoDevApp
//
//  Wrapper for Kotlin Compose Multiplatform UI
//  Copyright © 2024 Unit Mesh. All rights reserved.
//

import SwiftUI
import AutoDevUI

/// SwiftUI 包装器,用于显示 Kotlin Compose UI
struct ComposeView: UIViewControllerRepresentable {
    
    /// 创建 UIViewController
    /// - Parameter context: 上下文
    /// - Returns: Kotlin Compose UI 的 UIViewController
    func makeUIViewController(context: Context) -> UIViewController {
        // 调用 Kotlin 的 MainViewController 函数
        // 这个函数在 mpp-ui/src/iosMain/kotlin/cc/unitmesh/devins/ui/Main.kt 中定义
        return MainKt.MainViewController()
    }
    
    /// 更新 UIViewController
    /// - Parameters:
    ///   - uiViewController: 要更新的视图控制器
    ///   - context: 上下文
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Compose UI 是声明式的,状态管理在 Kotlin 侧
        // 这里不需要手动更新
    }
}

#Preview {
    ComposeView()
}

