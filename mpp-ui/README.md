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

### ⚠️ Android
配置完成，需要物理设备或模拟器测试。

### ⚠️ Web (JS)
配置完成，需要进一步测试。

### ✅ Node.js (CLI)
终端 UI，使用 React/Ink 构建，完全集成 mpp-core 的 AI Agent 功能。

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

## MarkdownSketchRenderer 功能

### 支持的 Markdown 特性

- ✅ 标题 (H1-H6)
- ✅ 粗体、斜体、删除线
- ✅ 列表（有序和无序）
- ✅ 引用块
- ✅ 代码块（带语法高亮）
- ✅ 链接和图片
- ✅ 表格
- ✅ 水平分隔线

### 特殊渲染

- **代码块**: 使用 Material Card 包装，显示语言标签
- **Diff 块**: 使用 DiffSketchRenderer 进行特殊渲染（绿色/红色行）
- **流式渲染**: 支持 LLM 响应的流式显示

### 使用示例

```kotlin
import cc.unitmesh.devins.ui.compose.sketch.MarkdownSketchRenderer

// 渲染完整的 LLM 响应（支持混合内容）
MarkdownSketchRenderer.RenderResponse(
    content = llmResponse,
    isComplete = true
)

// 渲染纯 Markdown
MarkdownSketchRenderer.RenderMarkdown(
    markdown = "# Hello\n\nThis is **bold**"
)

// 渲染纯文本（不解析 Markdown）
MarkdownSketchRenderer.RenderPlainText(
    text = "Raw text content"
)
```

## 演示应用

运行 `MarkdownDemoApp` 可以看到四个标签页：

1. **LLM 响应**: 模拟 AI 助手的混合内容响应
2. **代码示例**: 多种编程语言的代码块
3. **Markdown 完整**: 完整的 Markdown 语法展示
4. **纯文本**: 不解析 Markdown 的原始文本显示

## 已知问题

1. **Java 版本**: 项目需要 Java 17，Java 25 (EA) 会导致 Kotlin 编译器错误
2. **快照依赖**: `multiplatform-markdown-renderer 0.20.0` 需要快照仓库，当前使用 0.13.0 稳定版
3. **Android 测试**: Android 版本已配置但未在实际设备上测试
4. **Web 测试**: Web 版本已配置但需要进一步测试渲染效果

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

## 贡献

在添加新功能时，请确保：
1. 代码在 `commonMain` 中实现（除非是平台特定的）
2. 使用 Compose Multiplatform 的跨平台 API
3. 在多个平台上测试
4. 更新本 README

## FileChooser 平台支持

### JVM (Desktop) ✅
完全支持，使用 Swing 的 `JFileChooser`。

### Android ⚠️
当前为占位实现，调用文件选择器会返回 `null`。

**原因**：Android 的 Activity Result API 要求在 Activity 创建时注册 launcher，不能在运行时动态注册。

**推荐方案**：在 Compose UI 中使用 `rememberLauncherForActivityResult`：
```kotlin
// 文件选择示例
val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri: Uri? ->
    uri?.let {
        // 处理选中的文件
    }
}

Button(onClick = { launcher.launch(arrayOf("*/*")) }) {
    Text("选择文件")
}

// 目录选择示例
val dirLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
) { uri: Uri? ->
    uri?.let {
        // 处理选中的目录
    }
}
```

### Web (JS) ✅
支持浏览器原生的文件选择对话框。

---

## AutoDev CLI (@autodev/cli)

### 快速开始

```bash
# 安装依赖
npm install

# 构建项目（Kotlin/JS + TypeScript）
npm run build

# 运行 CLI
npm start
```

### 首次运行

首次运行时，会提示配置 LLM 提供商：

1. 选择 LLM 提供商（OpenAI、Anthropic、Google 等）
2. 输入 API Key
3. 选择模型（或使用默认值）

配置保存在 `~/.autodev/config.yaml`

### 聊天命令

- `/help` - 显示可用命令
- `/clear` - 清除聊天历史
- `/config` - 显示当前配置
- `/exit` - 退出应用
- `Ctrl+C` - 退出应用

### 支持的 LLM 提供商

- **OpenAI**: `gpt-4`, `gpt-3.5-turbo`
- **Anthropic**: `claude-3-5-sonnet-20241022`
- **Google**: `gemini-2.0-flash-exp`
- **DeepSeek**: `deepseek-chat`
- **Ollama**: `llama3.2` (本地运行)
- **OpenRouter**: 通过 OpenRouter API 访问各种模型

### 开发

```bash
# 仅构建 Kotlin/JS
npm run build:kotlin

# 仅构建 TypeScript
npm run build:ts

# 开发模式（监听 TypeScript 变化）
npm run dev

# 清理构建产物
npm run clean
```

---

## 许可证

与主项目 AutoDev 相同

