---
layout: default
title: AI Agent Quickstart
parent: AI Agent
nav_order: 1
---

AutoDev@1.7.0
{: .label .label-yellow }

Since AutoDev@1.7.0 version, we support custom AI agent, you can integrate your own AI agent into AutoDev.

<img src="https://unitmesh.cc/auto-dev/custom-agent-popup.png" alt="Custom AI Agent Dropdown" width="600px"/>

## Custom AI Agent

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`, select `Custom Agent`.
2. click `Enable Custom Agent`
3. fill JSON format config in `Custom Agent Config` textarea.
4. Apply and OK.
5. Click `x` and Close AutoDev panel and click `NewChat` in the AutoDev tool window.

### Custom Agent Examples

Demo:

```json
[
  {
    "name": "内部 API 集成",
    "description": "在一个组织或项目中，不同系统或组件之间的通信接口。",
    "url": "http://127.0.0.1:8765/api/agent/api-market",
    "responseAction": "Direct"
  },
  {
    "name": "组件库查询",
    "description": "从组件库中检索特定的 UI 组件，以便在开发的应用程序中使用。",
    "url": "http://127.0.0.1:8765/api/agent/component-list",
    "responseAction": "TextChunk"
  },
  {
    "name": "页面生成",
    "description": "使用 React 框架，基于组件和状态来生成页面。",
    "url": "http://127.0.0.1:8765/api/agent/ux",
    "auth": {
      "type": "Bearer",
      "token": "eyJhbGci"
    },
    "responseAction": "WebView"
  },
  {
    "name": "DevInInsert",
    "description": "Update，並指定20秒的timeout時間",
    "url": "http://127.0.0.1:8765/api/agent/devins-sample",
    "responseAction": "DevIns",
    "defaultTimeout": 20
  },
  {
    "name": "DifyAI",
    "description": "Dify Example",
    "url": "https://api.dify.ai/v1/completion-messages",
    "auth": {
      "type": "Bearer",
      "token": "app-abcd"
    },
    "connector": {
      "requestFormat": "{\"fields\": {\"inputs\": {\"feature\": \"$content\"}, \"response_mode\": \"streaming\", \"user\": \"phodal\" }}",
      "responseFormat": "$.answer"
    },
    "responseAction": "Stream"
  }
]
```

Notes: Dify API support by [#251](https://github.com/unit-mesh/auto-dev/issues/251), since 1.8.18 version

### responseAction

```kotlin
enum class CustomAgentResponseAction {
    /**
     * Direct display result
     */
    Direct,

    /**
     * Stream response
     */
    Stream,

    /**
     * Text splitting result
     */
    TextChunk,

    /**
     * Display result in WebView
     */
    WebView,

    /**
     * Handle by DevIns language compile and run in code block.
     * @since: AutoDev@1.8.2
     */
    DevIns
}
```

### interactive

```kotlin
enum class InteractionType {
    ChatPanel,
    AppendCursor,
    AppendCursorStream,
    OutputFile,
    ReplaceSelection,
    ReplaceCurrentFile,
    ;
}
```