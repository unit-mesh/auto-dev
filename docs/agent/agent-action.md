---
layout: default
title: AI Agent Response Action
parent: AI Agent
nav_order: 2
---

## Json format

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


