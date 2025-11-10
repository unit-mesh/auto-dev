//
//  AutoDevApp.swift
//  AutoDevApp
//
//  AutoDev iOS Application
//  Copyright © 2024 Unit Mesh. All rights reserved.
//

import SwiftUI

@main
struct AutoDevApp: App {
    init() {
        print("🚀 AutoDev iOS App starting...")
        setupApp()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
    
    private func setupApp() {
        // 应用初始化配置
        // 例如: 设置日志级别、主题等
    }
}

