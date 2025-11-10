# AutoDev iOS App

这是 AutoDev 的 iOS 应用,使用 Compose Multiplatform 构建。

## 🚀 快速开始

### 方法 1: 一键运行 (推荐)

```bash
cd mpp-ios
./build-and-run.sh
```

这个脚本会自动:
1. ✅ 编译 Kotlin Framework
2. ✅ 安装 CocoaPods 依赖
3. ✅ 打开 Xcode 项目

然后在 Xcode 中选择模拟器并点击 Run (⌘R)。

### 方法 2: 手动步骤

#### 1. 安装依赖

确保您已安装:
- Xcode 15.0+
- CocoaPods (`sudo gem install cocoapods`)
- Gradle (项目已包含)

#### 2. 编译 Kotlin Framework

```bash
cd mpp-ios
./build-framework.sh
```

#### 3. 安装 CocoaPods 依赖

```bash
pod install
```

**重要**: `pod install` 会自动配置所有必要的编译参数,无需手动修改 Xcode 配置!

详见: [PODFILE-CONFIG.md](PODFILE-CONFIG.md)

#### 4. 打开 Xcode 项目

```bash
open AutoDevApp.xcworkspace
```

**注意**: 必须打开 `.xcworkspace` 文件,而不是 `.xcodeproj` 文件!

#### 5. 运行应用

1. 在 Xcode 中选择模拟器 (例如: iPhone 15 Pro)
2. 点击 Run 按钮 (⌘R)
3. 应用将启动并显示 AutoDev Compose UI

## 项目结构

```
mpp-ios/
├── AutoDevApp/                    # iOS 应用源代码
│   ├── AutoDevApp.swift          # 应用入口
│   ├── ContentView.swift         # 主视图
│   ├── ComposeView.swift         # Compose UI 包装器
│   ├── Info.plist                # 应用配置
│   └── Assets.xcassets/          # 资源文件
├── AutoDevApp.xcodeproj/         # Xcode 项目
├── AutoDevApp.xcworkspace/       # CocoaPods 工作空间 (pod install 后生成)
├── Podfile                       # CocoaPods 依赖配置
└── README.md                     # 本文件
```

## 自动化脚本

### 一键构建和运行

使用提供的脚本:

```bash
./build-and-run.sh
```

这个脚本会:
1. 编译 Kotlin Framework
2. 安装 CocoaPods 依赖
3. 打开 Xcode 项目

### 仅编译 Framework

```bash
./build-framework.sh
```

## 开发指南

### 修改 Kotlin 代码后

每次修改 `mpp-core` 或 `mpp-ui` 中的 Kotlin 代码后:

```bash
# 重新编译 framework
cd ..
./gradlew :mpp-ui:linkDebugFrameworkIosSimulatorArm64

# 返回 iOS 项目
cd mpp-ios

# 重新安装 pod (可选,如果 framework 没有更新)
pod install

# 在 Xcode 中重新运行
```

### 修改 Swift 代码后

直接在 Xcode 中修改并运行即可,无需重新编译 framework。

### 切换到真机

1. 编译真机 framework:
   ```bash
   cd ..
   ./gradlew :mpp-ui:linkDebugFrameworkIosArm64
   ```

2. 在 Xcode 中:
   - 连接 iPhone/iPad
   - 选择您的设备
   - 设置开发团队 (Signing & Capabilities)
   - 运行

## ⚙️ Podfile 自动配置

本项目的 Podfile 已配置为**自动管理所有编译参数**,包括:

- ✅ `FRAMEWORK_SEARCH_PATHS` - Framework 搜索路径
- ✅ `OTHER_LDFLAGS` - 链接器标志 (`-ObjC`, `-lc++`, `-framework AutoDevUI`, `-lsqlite3`)
- ✅ `ENABLE_BITCODE` - 禁用 Bitcode (Kotlin/Native 不支持)
- ✅ `IPHONEOS_DEPLOYMENT_TARGET` - iOS 最低版本

**这意味着您无需手动修改 Xcode 项目配置!**

每次运行 `pod install` 时,这些参数会自动应用到生成的 `.xcconfig` 文件中。

详细说明请参考: [PODFILE-CONFIG.md](PODFILE-CONFIG.md)

### 验证配置

运行 `pod install` 后,可以验证配置是否正确:

```bash
cat Pods/Target\ Support\ Files/Pods-AutoDevApp/Pods-AutoDevApp.debug.xcconfig | grep -E "OTHER_LDFLAGS|FRAMEWORK_SEARCH_PATHS"
```

应该看到类似输出:

```
FRAMEWORK_SEARCH_PATHS = $(inherited) "${PODS_ROOT}/../../mpp-core/build/bin/iosSimulatorArm64/debugFramework" "${PODS_ROOT}/../../mpp-ui/build/bin/iosSimulatorArm64/debugFramework"
OTHER_LDFLAGS = $(inherited) -ObjC -l"c++" -framework "AutoDevCore" -framework "AutoDevUI"
```

## 故障排除

### Pod install 失败

```bash
# 清理 CocoaPods 缓存
pod cache clean --all
pod deintegrate
pod install
```

### Framework 找不到

确保已编译对应架构的 framework:
- 模拟器 (Apple Silicon): `iosSimulatorArm64`
- 模拟器 (Intel): `iosX64`
- 真机: `iosArm64`

### Xcode 编译错误

1. 清理项目: Product -> Clean Build Folder (⇧⌘K)
2. 重新安装 pods: `pod install`
3. 重启 Xcode

### 运行时崩溃

检查:
1. Framework 是否正确嵌入
2. Bitcode 是否已禁用 (应该是 NO)
3. Deployment Target 是否正确 (14.0+)

## 配置

### Bundle Identifier

默认: `cc.unitmesh.AutoDevApp`

修改位置:
- Xcode: Target -> General -> Bundle Identifier
- 或修改 `project.pbxproj` 中的 `PRODUCT_BUNDLE_IDENTIFIER`

### Deployment Target

默认: iOS 14.0

修改位置:
- Xcode: Target -> General -> Deployment Info
- 或修改 `project.pbxproj` 中的 `IPHONEOS_DEPLOYMENT_TARGET`

### 开发团队

在 Xcode 中设置:
1. 选择 Target -> Signing & Capabilities
2. 选择您的 Team
3. Xcode 会自动配置证书和 Provisioning Profile

## 性能优化

### 使用 Release Framework

```bash
cd ..
./gradlew :mpp-ui:linkReleaseFrameworkIosSimulatorArm64
```

然后在 Podfile 中修改路径为 `release` 配置。

### 减小应用大小

1. 使用 Release 配置
2. 启用代码优化
3. 移除未使用的资源

## 相关文档

- [iOS 支持总结](../docs/ios-support-summary.md)
- [iOS 集成指南](../docs/mpp-ios-integration.md)
- [iOS 示例应用](../docs/ios-example-app.md)
- [iOS README](../docs/ios-README.md)

## 技术栈

- **Swift 5.0+** - iOS 应用代码
- **SwiftUI** - UI 框架
- **Kotlin Multiplatform** - 共享业务逻辑
- **Compose Multiplatform** - 跨平台 UI
- **CocoaPods** - 依赖管理

## 许可证

与 AutoDev 主项目相同。

## 支持

如有问题,请查看:
- [故障排除](#故障排除)
- [GitHub Issues](https://github.com/unit-mesh/auto-dev/issues)
- 项目文档

---

**享受在 iOS 上使用 AutoDev!** 🚀📱

