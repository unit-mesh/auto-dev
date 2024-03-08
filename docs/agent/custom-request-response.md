---
layout: default
title: Custom Request/Response 
parent: AI Agent
nav_order: 4
---

# Custom Request/Response

the Custom Request/Response is similar to [Custom LLM server](/custom/llm-server), which defines the request and 
response format under the `connector` field.

```json
{
  "name": "内部 API 集成",
  "url": "http://127.0.0.1:8765/api/agent/api-market",
  "auth": {
    "type": "Bearer",
    "token": "eyJhbGci"
  },
  "connector": {
    "requestFormat": "{\"customFields\": {\"model\": \"yi-34b-chat\", \"stream\": true}}",
    "responseFormat": "$.choices[0].delta.content"
  },
  "responseAction": "Direct",
  "interactive": "ChatPanel"
}
```

