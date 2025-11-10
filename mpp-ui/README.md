# mpp-ui - Multiplatform UI Module

## 概述

`mpp-ui` 是 AutoDev 的跨平台 UI 模块，基于 Compose Multiplatform 构建，支持 JVM (Desktop)、Android、JS (Web) 和 **Node.js (CLI)** 平台。

本模块包含：
- **SketchRenderer**: 原有的 LLM 响应渲染器
- **MarkdownSketchRenderer**: 新实现的 Markdown 渲染器，使用 `multiplatform-markdown-renderer` 库
- **MarkdownDemo**: 演示应用，展示 MarkdownSketchRenderer 的各种渲染能力
- **FileChooser**: 跨平台文件选择器，支持 JVM、Android 和 JS 平台
- **AutoDev CLI**: 终端 UI，使用 React/Ink 构建，集成 mpp-core 的 AI Agent 功能

## 技术栈

- **Kotlin 2.2.0** - Multiplatform
- **Compose Multiplatform 1.8.0** - UI 框架
- **multiplatform-markdown-renderer 0.13.0** - Markdown 渲染
- **Material 3** - 设计系统

## 平台支持

### ✅ JVM (Desktop)
完全支持，已测试运行成功。

### JVM CLI

```bash
./gradlew :mpp-ui:runRemoteAgentCli --args="--server http://localhost:8080 --project-id https://github.com/unit-mesh/untitled --task '你的任务' --use-server-config --branch master"
```

### ⚠️ Android
配置完成，需要物理设备或模拟器测试。

### ⚠️ Web (JS)

### ✅ Node.js (CLI)

Local mode

```bash
node dist/jsMain/typescript/index.js code --task "add Spring ai to project and also a service example, I use deepseek, here it's Spring AI documentation https://docs.spring.io/spring-ai/reference/api/chat/deepseek-chat.html 请先阅读文档！！" -p /Users/phodal/IdeaProjects/untitled --max-iterations 100
```

Server

```bash
 node dist/jsMain/typescript/index.js server \\n  --task "编写 BlogService 测试" \\n  --project-id https://github.com/unit-mesh/untitled \\n  -s http://localhost:8080
```

## 构建和运行

### 前提条件

确保 `gradle.properties` 中已配置：
```properties
# 强制使用 Java 17（避免 Kotlin 编译器与 Java 25 的兼容性问题）
org.gradle.java.home=/path/to/jdk-17

# Android 配置
android.useAndroidX=true
android.enableJetifier=true

# Compose 实验性功能
org.jetbrains.compose.experimental.jscanvas.enabled=true
```

### Desktop (JVM)

运行演示应用：
```bash
./gradlew :mpp-ui:run
```

构建 JAR：
```bash
./gradlew :mpp-ui:jvmJar
# 输出: mpp-ui/build/libs/mpp-ui-jvm.jar
```

构建原生安装包：
```bash
# macOS
./gradlew :mpp-ui:packageDmg

# Windows
./gradlew :mpp-ui:packageMsi

# Linux
./gradlew :mpp-ui:packageDeb
```

### Web (JS)

构建 Web 版本：
```bash
./gradlew :mpp-ui:jsBrowserProductionWebpack
# 输出: mpp-ui/build/dist/js/productionExecutable/
```

运行开发服务器：
```bash
./gradlew :mpp-ui:jsBrowserDevelopmentRun --continuous
# 访问: http://localhost:8080
```

### Android

构建 APK：
```bash
./gradlew :mpp-ui:assembleDebug
# 输出: mpp-ui/build/outputs/apk/debug/
```

在连接的设备上安装和运行：
```bash
./gradlew :mpp-ui:installDebug
```

## 架构

```
mpp-ui/
├── src/
│   ├── commonMain/          # 共享代码
│   │   └── kotlin/
│   │       └── cc/unitmesh/devins/ui/compose/
│   │           ├── sketch/
│   │           │   ├── SketchRenderer.kt            # 原有渲染器
│   │           │   ├── MarkdownSketchRenderer.kt    # 新 Markdown 渲染器
│   │           │   └── DiffSketchRenderer.kt        # Diff 渲染器
│   │           └── MarkdownDemo.kt                  # 演示应用
│   │
│   ├── jvmMain/             # Desktop 特定代码
│   │   └── kotlin/
│   │       └── cc/unitmesh/devins/ui/
│   │           ├── Main.kt                  # 原主应用入口
│   │           └── MarkdownDemoMain.kt      # Markdown 演示入口
│   │
│   ├── androidMain/         # Android 特定代码
│   │   ├── AndroidManifest.xml
│   │   └── kotlin/
│   │       └── cc/unitmesh/devins/ui/
│   │           └── MainActivity.kt
│   │
│   └── jsMain/              # Web 特定代码
│       ├── kotlin/
│       │   └── cc/unitmesh/devins/ui/
│       │       └── Main.kt
│       └── resources/
│           └── index.html
│
└── build.gradle.kts         # Multiplatform 配置
```

## 国际化 (i18n)

mpp-ui 支持多语言，目前支持：
- **English** (en)
- **中文** (zh)

### 语言切换

**CLI**：在 `~/.autodev/config.yaml` 中设置 `language` 字段：
```yaml
language: zh
```

**Desktop/Android**：使用 UI 中的语言切换组件

详细文档请参阅：[I18N.md](docs/I18N.md)

## 贡献

在添加新功能时，请确保：
1. 代码在 `commonMain` 中实现（除非是平台特定的）
2. 使用 Compose Multiplatform 的跨平台 API
3. 在多个平台上测试
4. 更新本 README
5. 为所有用户可见的文本添加翻译（参见 [I18N.md](docs/I18N.md)）
