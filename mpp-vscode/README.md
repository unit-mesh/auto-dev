# mpp-vscode

基于 Kotlin Multiplatform (KMP) 的 VSCode 扩展，复用 mpp-core 的核心能力。

## 架构概述

```
mpp-vscode/
├── package.json              # VSCode 扩展配置
├── src/
│   ├── extension.ts          # 入口点
│   ├── services/
│   │   ├── ide-server.ts     # MCP 协议服务器
│   │   ├── diff-manager.ts   # Diff 管理
│   │   └── chat-service.ts   # Chat 服务
│   ├── providers/
│   │   ├── chat-view.ts      # Webview Provider
│   │   └── diff-content.ts   # Diff Content Provider
│   ├── commands/
│   │   └── index.ts          # 命令注册
│   └── bridge/
│       └── mpp-core.ts       # mpp-core 桥接层
├── webview/                  # Webview UI
│   ├── src/
│   │   ├── App.tsx
│   │   └── components/
│   └── package.json
└── tsconfig.json
```

## TODO List

### Phase 1: 项目基础设施 ✅
- [x] 创建项目目录结构
- [x] 创建 package.json (VSCode 扩展配置)
- [x] 创建 tsconfig.json
- [x] 配置 esbuild 打包
- [x] 配置 vitest 测试框架

### Phase 2: 核心服务 ✅
- [x] 实现 mpp-core 桥接层 (`src/bridge/mpp-core.ts`)
  - [x] 导入 @autodev/mpp-core
  - [x] 封装 LLMService (JsKoogLLMService)
  - [x] 封装 CodingAgent (JsCodingAgent)
  - [x] 封装 ToolRegistry (JsToolRegistry)
  - [x] 封装 CompletionManager (JsCompletionManager)
  - [x] 封装 DevInsCompiler (JsDevInsCompiler)
- [x] 实现 extension.ts 入口
  - [x] 扩展激活/停用
  - [x] 服务初始化
- [x] 添加单元测试 (`test/bridge/mpp-core.test.ts`)

### Phase 3: IDE 集成 ✅
- [x] 实现 IDE Server (MCP 协议)
  - [x] Express HTTP 服务器
  - [x] 端点: /health, /context, /diff/open, /diff/close, /file/read, /file/write
  - [x] 认证和 CORS 保护
  - [x] 端口文件写入 (~/.autodev/ide-server.json)
- [x] 实现 Diff Manager
  - [x] showDiff() - 显示差异
  - [x] acceptDiff() - 接受更改
  - [x] cancelDiff() - 取消更改
  - [x] closeDiffByPath() - 按路径关闭
  - [x] DiffContentProvider
- [x] 添加单元测试 (`test/services/`)

### Phase 4: Chat 界面 ✅
- [x] 实现 Chat Webview Provider
  - [x] Webview 创建和管理
  - [x] 消息桥接 (VSCode ↔ Webview)
  - [x] LLM 服务集成
- [x] 创建 Webview UI (内嵌 HTML)
  - [x] 聊天消息组件
  - [x] 输入框组件
  - [x] 流式响应显示

### Phase 5: 命令和功能 ✅
- [x] 注册 VSCode 命令
  - [x] autodev.chat - 打开聊天
  - [x] autodev.acceptDiff - 接受差异
  - [x] autodev.cancelDiff - 取消差异
  - [x] autodev.runAgent - 运行 Agent
- [x] 快捷键绑定 (Cmd+Shift+A)
- [x] 状态栏集成

### Phase 6: 高级功能 ✅
- [x] DevIns 语言支持
  - [x] 语法高亮 (TextMate grammar)
  - [x] 自动补全 (/, @, $ 触发)
- [ ] 代码索引集成
- [ ] 领域词典支持
- [ ] React Webview UI (替换内嵌 HTML)

## 参考项目

1. **autodev-vscode** - 早期 AutoDev VSCode 版本，全功能实现
2. **gemini-cli/vscode-ide-companion** - Gemini 的轻量级 MCP 桥接器
3. **mpp-ui** - 现有的 CLI 工具，展示如何使用 mpp-core

## 开发指南

### 构建 mpp-core

```bash
cd /Volumes/source/ai/autocrud
./gradlew :mpp-core:assembleJsPackage
```

### 安装依赖

```bash
cd mpp-vscode
npm install
```

### 开发模式

```bash
npm run watch
```

### 打包扩展

```bash
npm run package
```

## 技术栈

- **TypeScript** - 主要开发语言
- **mpp-core (Kotlin/JS)** - 核心 LLM 和 Agent 能力
- **React** - Webview UI
- **Express** - MCP 服务器
- **esbuild** - 打包工具

