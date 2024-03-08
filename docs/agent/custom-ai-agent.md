---
layout: default
title: AI Agent Quickstart
parent: AI Agent
nav_order: 1
---

AutoDev@1.7.0
{: .label .label-yellow }

Since AutoDev@1.7.0 version, we support custom AI agent, you can integrate your own AI agent into AutoDev.

<img src="https://unitmesh.cc/auto-dev/custom-agent-popup.png" alt="Custom AI Agent Dropdown" width="400px"/>

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
