---
layout: default
title: AI Agent Response Action
parent: AI Agent
nav_order: 2
---

## Response Action

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

### Direct

> Render the result directly in the chat window.

<img src="https://unitmesh.cc/auto-dev/custom-agent-example.png" alt="Custom AI Agent Dropdown" width="600px"/>

### TextChunk

> Display the result in the AutoDev input box for continuous processing.

<img src="https://unitmesh.cc/auto-dev/custom-agent-text-chunk.png" alt="Custom AI Agent Dropdown" width="600px"/>

### WebView

> Show the result in a WebView for front-end rendering.

<img src="https://unitmesh.cc/auto-dev/custom-agent-webview.png" alt="Custom AI Agent Dropdown" width="600px"/>

### DevIns

AutoDev@1.8.2
{: .label .label-yellow }

> The DevIns response action will handle the response in the DevIns language.

just like the following example. 

    /write:HelloWorld.java#L1-L12
    
    ```java
    public class HelloWorld {
        public static void main(String[] args) {
            System.out.println("Hello, World!");
        }
    }
    ```

{: .highlight }
The DevIns response content is different from the Custom Agent Response content. In Custom Agent Response, the code
content should inside \`\`\`DevIns code block.
