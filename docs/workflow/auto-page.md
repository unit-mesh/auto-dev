---
layout: default
title: AutoPage
nav_order: 3
parent: Workflow
---

Required plugin:

JavaScript
{: .label .label-yellow }

Demo Video: [https://www.bilibili.com/video/BV1Ye411h7Qu/](https://www.bilibili.com/video/BV1Ye411h7Qu/)

implementation: [cc.unitmesh.database.flow.AutoPageFlow]

## Design Flow

Common flow for frontend development:

- getRoutes
- getComponents
- getDesignSystemComponents
- sampleRemoteCall
- sampleStateManagement

## Prompt Override

Steps:

- step 1: `prompts/genius/page/page-gen-clarify.vm`
- step 2: `prompts/genius/page/page-gen-design.vm`

Context:

```kotlin
data class AutoPageContext(
    val requirement: String,
    var pages: List<String>,
    val pageNames: List<String>,
    var components: List<String>,
    val componentNames: List<String>,
    val routes: List<String>,
    val frameworks: List<String> = listOf("React"),
    val language: String = "JavaScript",
)
```

### Current Prompt

Clarify:

    You are a professional Frontend developer.
    According to the user's requirements, you should choose the best components for the user in List.

    - Framework: ${context.frameworks}
    - Language: ${context.language}
    - User component: ${context.componentNames}, ${context.pageNames}
    
    For example:
    
    - Question(requirements): Build a form for user to fill in their information.
    - You should anwser: [Input, Select, Radio, Checkbox, Button, Form]
    
    ----
    
    Here are the User requirements:
    
    ```markdown
    ${context.requirement}
    ```
    
    Please choose the best Components for the user, just return the components names in a list, no explain.

Design:

    You are a professional Frontend developer.
    According to the user's requirements, and Components info, write Component for the user.
    
    - Framework: ${context.frameworks}
    - Language: ${context.language}
    - User Components Infos: ${context.components}
    
    For example:
    
    - Question(requirements): Build a form for user to fill in their information.
      // componentName: Form, props: { fields: [{name: 'name', type: 'text'}, {name: 'age', type: 'number'}] }
      // componentName: Input, props: { name: 'name', type: 'text' }
      // componentName: Input, props: { name: 'age', type: 'number' }
    - Answer:
    ```react
    <Form>
        <Input name="name" type="text" />
        <Input name="age" type="number" />
    </Form>
    ```
    
    ----
    
    Here are the requirements:
    
    ```markdown
    ${context.requirement}
    ```
    
    Please write your code with Markdown syntax, no explanation is needed:
    
## Theory of AutoPage 

Common frontend flow:

1. Functional bootstrap
2. Request Transform / Data validation, IO Handing.
3. Process IPC/RPC Calling
4. Output Transform / Render


