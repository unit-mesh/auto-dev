---
layout: default
title: Batch AI Action
parent: Customize Features
nav_order: 15
permalink: /custom/batch
---

# Batch AI Action

We use Team Prompts to provide a way to batch process files in the project. The default 
Team Prompts path is `prompts/`, which is the `prompts/` directory located in the project's root directory.

Through the `batchFileRegex` configuration, you can specify the file to be processed in batch.

## Batch File example

核心点：`batchFileRegex` 用于匹配批量文件，`codeOnly: true` 只将纯代码部分放入文件，`interaction: ReplaceCurrentFile`
用于替换当前文件。

    ---
    interaction: ReplaceCurrentFile
    batchFileRegex: "**/*.ts"
    codeOnly: true
    ---
    
    重构用户的代码。
    要求：只在一个 markdown 代码块返回重构完的代码
    
    ${all}




