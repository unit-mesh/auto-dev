---
layout: default
title: Custom AI Agent
parent: Agent
nav_order: 1
---

AutoDev@1.7.0
{: .label .label-yellow }

Since AutoDev@1.7.0 version, we support custom AI agent, you can integrate your own AI agent into AutoDev.

## Custom AI Agent

1. open AutoDev Config in `Settings` -> `Tools` -> `AutoDev`, select `Custom Agent`.
2. click `Enable Custom Agent`
3. fill JSON format config in `Custom Agent Config` textarea.
4. Apply and OK.
5. Click `x` and Close AutoDev panel and click `NewChat` in the AutoDev tool window.

Examples:

```json
[
  {
    "name": "内部 API 集成",
    "url": "http://127.0.0.1:8765/api/agent/api-market",
    "responseAction": "Direct"
  },
  {
    "name": "组件库查询",
    "url": "http://127.0.0.1:8765/api/agent/component-list",
    "responseAction": "TextChunk"
  },
  {
    "name": "页面生成",
    "url": "http://127.0.0.1:8765/api/agent/ux",
    "auth": {
      "type": "Bearer",
      "token": "eyJhbGci"
    },
    "responseAction": "WebView"
  }
]
```

## Json format

ResponseAction:

```kotlin
enum class ResponseAction {
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
     * will be handled by the client
     */
    Flow,

    /**
     * Display result in WebView
     */
    WebView
}
```

## Server

## Request Body

same to Chat API

```json
{
  "messages": [
    {
      "role": "user",
      "message": "str"
    }
  ]
}
```

### Server API example

see in [example/custom_agent](https://github.com/unit-mesh/auto-dev/tree/master/example/custom_agent)