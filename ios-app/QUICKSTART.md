# AutoDev iOS App - 快速开始

## 🚀 一键运行

```bash
cd ios-app
./build-and-run.sh
```

这个脚本会自动:
1. ✅ 编译 Kotlin Framework
2. ✅ 安装 CocoaPods 依赖
3. ✅ 打开 Xcode 项目

然后在 Xcode 中:
1. 选择模拟器 (例如: iPhone 15 Pro)
2. 点击 Run (⌘R)
3. 享受 AutoDev! 🎉

## 📋 前提条件

- ✅ macOS 14.0+
- ✅ Xcode 15.0+
- ✅ CocoaPods (`sudo gem install cocoapods`)

## 🔧 手动步骤

### 1. 编译 Framework

```bash
cd ios-app
./build-framework.sh
```

### 2. 安装依赖

```bash
pod install
```

### 3. 打开项目

```bash
open AutoDevApp.xcworkspace
```

**重要**: 必须打开 `.xcworkspace` 文件,不是 `.xcodeproj`!

### 4. 运行

在 Xcode 中选择模拟器并点击 Run (⌘R)

## 🎯 项目结构

```
ios-app/
├── AutoDevApp/              # Swift 源代码
│   ├── AutoDevApp.swift    # 应用入口
│   ├── ContentView.swift   # 主视图
│   └── ComposeView.swift   # Compose UI 包装器
├── Podfile                  # CocoaPods 配置
├── build-and-run.sh         # 一键构建脚本
└── build-framework.sh       # Framework 编译脚本
```

## 💡 常用命令

### 编译不同版本

```bash
# Debug 版本 (默认)
./build-framework.sh

# Release 版本
./build-framework.sh release

# 真机版本
./build-framework.sh device
```

### 清理和重建

```bash
# 清理 CocoaPods
pod deintegrate
pod install

# 重新编译 Framework
./build-framework.sh
```

## ❓ 常见问题

### Q: Pod install 失败?

```bash
pod cache clean --all
pod deintegrate
pod install
```

### Q: Framework 找不到?

确保已运行 `./build-framework.sh`

### Q: Xcode 编译错误?

1. Clean Build Folder (⇧⌘K)
2. 重新运行 `pod install`
3. 重启 Xcode

## 📚 更多文档

- [README.md](README.md) - 完整文档
- [../docs/ios-README.md](../docs/ios-README.md) - iOS 支持总览
- [../docs/ios-app-integration.md](../docs/ios-app-integration.md) - 集成指南

## 🎉 完成!

现在您可以在 iOS 模拟器或真机上运行 AutoDev 了!

如有问题,请查看 [README.md](README.md) 或提交 Issue。

