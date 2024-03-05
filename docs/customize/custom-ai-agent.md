---
layout: default
title: Custom AI Agent
parent: Customize Features
nav_order: 19
---

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
    "url": "https://rag.unitmesh.cc/api/agent/market",
    "auth": {
        "type": "Bearer",
        "token": "eyJhbGci"
    },
    "responseAction": "Direct",
    "interactive": "ChatPanel"
  },
  {
    "name": "前面页面生成",
    "url": "https://rag.unitmesh.cc/api/agent/frontend",
    "auth": {
      "type": "Bearer",
      "token": "eyJhbGci"
    },
    "responseAction": "WebView",
    "interactive": "ChatPanel"
  }
]
```