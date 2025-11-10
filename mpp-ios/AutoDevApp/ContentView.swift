//
//  ContentView.swift
//  AutoDevApp
//
//  Main content view for AutoDev iOS App
//  Copyright © 2024 Unit Mesh. All rights reserved.
//

import SwiftUI

struct ContentView: View {
    var body: some View {
        ZStack {
            // 背景色 - 使用深色背景以匹配 AutoDev 主题
            Color.black
                .ignoresSafeArea()
            
            // Compose UI 视图
            ComposeView()
                .ignoresSafeArea()
        }
    }
}

#Preview {
    ContentView()
}

