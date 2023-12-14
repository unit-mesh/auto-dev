---
layout: default
title: Custom Quick Action
parent: Customize Features
nav_order: 10
permalink: /custom/quick-action
---

# Custom Quick Action

QuickAction will trigger by `control` + BACK_SLASH(`\`) key. The default behavior is to show a Input Dialog, and you can
input your prompt.

## Customize

For more, see in example: [https://github.com/unit-mesh/untitled/tree/master/prompts/quick](https://github.com/unit-mesh/untitled/tree/master/prompts/quick)

You can custom task by your own, by put prompts files under `prompts/quick`:

    ---
    type: QuickAction
    name: Continue Writing
    category: Default
    language: Markdown
    interaction: AppendCursorStream
    ---
    **system**
    
    You are an assistant helping a user write a document. Output how the document continues, no more than 3 sentences. Output in markdown format. Do not use links.
    
    Use this format, replacing text in brackets with the result. Do not include the brackets in the output:
    
    [Continuation of the document in markdown format, no more than 3 sentences.]
    
    **user**
    
    ${beforeCursor}
