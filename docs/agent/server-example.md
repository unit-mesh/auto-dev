---
layout: default
title: AI Agent Server 
parent: AI Agent
nav_order: 3
---

## Server

- ResponseAction.Direct should return "SSE<String>"
- ResponseAction.TextChunk should return "JSON"
- ResponseAction.WebView should return "HTML" code

## Request Body

same to Chat API

```json
{
  "messages": [
    {
      "role": "user",
      "content": "Hi, I want to book a flight from Hangzhou to Shanghai."
    }
  ]
}
```

### Server API example

see in [example/custom_agent](https://github.com/unit-mesh/auto-dev/tree/master/example/custom_agent)
