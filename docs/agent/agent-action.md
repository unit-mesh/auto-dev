---
layout: default
title: AI Agent Response Action
parent: AI Agent
nav_order: 2
---

## Response Action

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

### Direct

> Render the result directly in the chat window.

<img src="https://unitmesh.cc/auto-dev/custom-agent-example.png" alt="Custom AI Agent Dropdown" width="400px"/>

### TextChunk

> Display the result in the the AutoDev input box for continuous processing.

<img src="https://unitmesh.cc/auto-dev/custom-agent-text-chunk.png" alt="Custom AI Agent Dropdown" width="400px"/>

### WebView

> Show the result in a WebView for front-end rendering.

<img src="https://unitmesh.cc/auto-dev/custom-agent-webview.png" alt="Custom AI Agent Dropdown" width="400px"/>


