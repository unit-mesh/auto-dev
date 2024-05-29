---
layout: default
title: Batch AI Action
parent: Customize Features
nav_order: 15
permalink: /custom/batch
---

AutoDev@1.8.6
{: .label .label-yellow }

# Batch AI Action

We use Team Prompts to provide a way to batch process files in the project. The default 
Team Prompts path is `prompts/`, which is the `prompts/` directory located in the project's root directory.

Through the `batchFileRegex` configuration, you can specify the file to be processed in batch.

**Scenes**: When you need to batch process files in the project, such as refactoring, formatting, etc.

- Vue2 to Vue3 migration
- Refactor the user's code
- Comments for all code

**Entry point**: `Right-click` on the project -> `AutoDev AI AutoAction` -> `Batch AI Action`

## 批量 AI 操作（Chinese example）

核心点：`batchFileRegex` 用于匹配批量文件，`codeOnly: true` 只将纯代码部分放入文件，`interaction: ReplaceCurrentFile`
用于替换当前文件。

    ---
    interaction: ReplaceCurrentFile
    batchFileRegex: "**/*.ts"
    codeOnly: true
    ---
    
    重构用户的代码。要求：
    
    - 请在一个代码块返回重构完的所有代码，方便直接复制粘贴。
    - 请不要添加额外的代码，只修改用户的代码。
    
    ${all}

## Batch AI Action (English Example)

Key points: `batchFileRegex` is used to match batch files, `codeOnly: true` only puts the pure code part into the file,
`interaction: ReplaceCurrentFile` is used to replace the current file.

    ---
    interaction: ReplaceCurrentFile
    batchFileRegex: "**/*.ts"
    codeOnly: true
    ---
    
    Refactor the user's code. Requirements:
    
    - Please return all the refactored code in one code block, so that it can be copied and pasted directly.
    - Please do not add extra code, only modify the user's code.
    
    ${all}

