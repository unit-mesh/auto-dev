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
- [ ] 创建 package.json (VSCode 扩展配置)
- [ ] 创建 tsconfig.json
- [ ] 配置 esbuild 打包

### Phase 2: 核心服务
- [ ] 实现 mpp-core 桥接层 (`src/bridge/mpp-core.ts`)
  - [ ] 导入 @autodev/mpp-core
  - [ ] 封装 JsKoogLLMService
  - [ ] 封装 JsCodingAgent
  - [ ] 封装 JsToolRegistry
  - [ ] 封装 JsCompletionManager
- [ ] 实现 extension.ts 入口
  - [ ] 扩展激活/停用
  - [ ] 服务初始化

### Phase 3: IDE 集成
- [ ] 实现 IDE Server (MCP 协议)
  - [ ] Express HTTP 服务器
  - [ ] MCP 工具注册 (openDiff, closeDiff)
  - [ ] 会话管理
- [ ] 实现 Diff Manager
  - [ ] showDiff() - 显示差异
  - [ ] acceptDiff() - 接受更改
  - [ ] cancelDiff() - 取消更改
  - [ ] Diff Content Provider

### Phase 4: Chat 界面
- [ ] 实现 Chat Webview Provider
  - [ ] Webview 创建和管理
  - [ ] 消息桥接 (VSCode ↔ Webview)
- [ ] 创建 Webview UI
  - [ ] React 项目配置
  - [ ] 聊天消息组件
  - [ ] 输入框组件
  - [ ] 代码高亮组件

### Phase 5: 命令和功能
- [ ] 注册 VSCode 命令
  - [ ] autodev.chat - 打开聊天
  - [ ] autodev.acceptDiff - 接受差异
  - [ ] autodev.cancelDiff - 取消差异
  - [ ] autodev.runAgent - 运行 Agent
- [ ] 快捷键绑定
- [ ] 状态栏集成

### Phase 6: 高级功能
- [ ] DevIns 语言支持
  - [ ] 语法高亮
  - [ ] 自动补全
- [ ] 代码索引集成
- [ ] 领域词典支持

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

