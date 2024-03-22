---
layout: default
title: Prompt Strategy
parent: Development
nav_order: 98
---

# Prompt Strategy

AutoDev Prompt 生成策略是 AutoDev 的核心功能，它可以根据你的代码上下文，生成最佳的代码提示。

![AutoDev Prompt Example](https://unitmesh.cc/auto-dev/autodev-prompt-strategy-1.png)

通常来说，一个指令对应的 prompt 会由以下五部分组成：

- Action 类型。比如：`Code complete`，`Translate to Kotlin` 等。
- 语言上下文 （结合规范）。比如：`Java`，`Kotlin`，`Python` 对应的规范。
- 技术栈上下文 （结合规范）。比如 Controller，Service，Repository 对应的规范。
- 相关上下文(ClassProvider)。比如：当前文件，当前文件夹，当前项目，当前项目的所有文件等。
- 代码(PsiElement)。当前的代码

不同语言会基于自己的模块，实现 ContextPrompter，比如 JavaContextPrompter，KotlinContextPrompter 等。

## Prompt 架构

因此，AutoDev 参考了 Intellij Rust、JetBrains AI Assistant 的模块化架构方式，如下图所示：

![AutoDev Prompt Example](https://unitmesh.cc/auto-dev/autodev-prompt-strategy-2.png)

由每个语言模块基于抽象接口实现对应的：**语言上下文**、**技术栈上下文**，为此需要读取依赖相关的信息，如 gradle，maven，package.json 等。

## 相关上下文

AutoDev 提供了以下几种相关上下文：

- 基于静态代码分析的方式，即结合 import 语法和函数的输入、输出，生成对应的上下文信息。
  - 对应实现类：[JavaContextPrompter]
- 通过 Cosine Similarity 来计算最近打开 20 个文件代码块的相似度。即 GitHub Copilot、JetBrains AI Assistant 的实现方式之一。
  - 对应实现类：[SimilarChunksWithPaths]

![AutoDev Similar Chunk](https://unitmesh.cc/auto-dev/autodev-prompt-strategy-3.png)

## 隐藏细节的双 prompt

在 AutoDev 中，复杂的 prompt 会被分为两个 prompt 来实现，如下图所示：

```kotlin
abstract class ContextPrompter {
  open fun displayPrompt(): String = ""
  open fun requestPrompt(): String = ""
  ...
}
```

- displayPrompt: 用于展示给用户的 prompt，比如：`Code complete`，`Translate to Kotlin` 等。
- requestPrompt: 用于请求 AI 服务的 prompt，比如：`Code complete:\n${METHOD_INPUT_OUTPUT}\n${SPEC_controller}\n\n${SELECTION}`。

根据不同的情况，会在展示给用户的 prompt 中隐藏一些细节，比如相关代码块，输入输出等。

