---
layout: default
title: AutoDev 1.4 规模化 AI 研发辅助
nav_order: 3
parent: What's New
---

在过去的两个月里，随着 Thoughtworks 内部的大规模 AI 辅助软件交付（AI4SoftwareDelivery）的展开 —— 在全球，有上千名的
Thoughtworker 这一个涉及不同角色、不同地区，以及几十场内部分享的活动。

我们也在 AutoDev 加入了更多的新特性，以持续探索如何在 IDE 里更好的协助团队进行提效。为此，作为目前国内最好的开源 AI
辅助编程工具，我们在 AutoDev 1.4.0 引入了几个比较有趣的特性，以探索规模化的 AI 研发提效。

AutoDev GitHub：https://github.com/unit-mesh/auto-dev

## 团队 Prompts：代码化 Prompt，以在团队扩散

为了响应我同事们对于 TDD （测试驱动开发）的热情，即 #49 issue 中对于《支持TDD开发模式，根据指定测试生成对应实现》，我们构建了
Team Prompts 的功能。现在，你可以在你的代码库里，直接编写 Prompt，AutoDev 将读取您编写的 Prompt，并成为 AI 辅助功能的一部分。

![Untitled](https://prod-files-secure.s3.us-west-2.amazonaws.com/ba3432d7-a5ac-428b-9d05-6d088dd5940a/5cecc645-e9fd-466a-8adc-2f69b15299e3/Untitled.png)

这意味着：

- 您可以在团队里，共享你的 prompt，而不再是个性化的配置。
- 您组织里的不同团队，可以在各自的团队里分享自己的 AI 经验。
- 您不再需要定制更多的 IDE 需求，只需要提供接口能力即可。

### Team Prompts 示例

让我们来看一个简单的示例，首先你需要在你的代码库里创建（或者配置） Prompt 文件夹，然后使用编写你的一系列 Prompt，诸如于 TDD
里可以是：

- Tasking.vm，用于根据需求拆分出对应的测试用例。
- TDD-Red.vm，根据生成的测试用例，编写第一个失败的测试。
- TDD-Green.vm，根据生成的测试，编写、优化对应的实现代码。
- TDD-Refactor.vm，重构实现的代码。

在这些 prompt 文件里，只需要根据 AutoDev 的配置文件引入对应的上下文变量（参考：https://ide.unitmesh.cc/variables ） 即可。诸如：

```
---
priority: 2023
interaction: ChatPanel
---
```user```

你是一个资深的软件开发工程师，你擅长使用 TDD 的方式来开发软件，你需要根据新的测试用例，来改进原有的代码实现。

原有的实现代码是：$context.underTestFileCode($methodName)

新的测试代码是：

${selection}

请根据新的测试，优化 class under test 部分的代码。请返回对应的方法的代码，使用 ``` 开始你的代码块：
```

Prompt 开头的部分是一个 Markdown 的 YAML FrontMatter，用于做一些简单的配置，在这里的 priority 用于配置菜单中的优先级，interaction
即是用于配置交互方式，如：

- `ChatPanel` 用于直接输出在右侧的聊天窗口；
- `AppendCursorStream` 则是用 Stream （打字机效果）的方式在当前文档输出。

Context 则是内置的一些系统函数，用于提供额外的能力支持。

### Team Prompts vs Custom Prompt

在 AutoDev 1.1 中，我们提供了 Custom Prompt 的功能，它的主要意图是为个人提供一些个性化的配置，而 Team Prompts
则是针对于团队来提供团队统一的配置能力。

通过 Team Prompts 这样的方式，我们可以编写一系列适用于不同场景的 AI 指令，并快速分享给团队的所有人。

我们将持续演进 Team Prompts，以更方便地让大家使用。

## 自定义活文档：持续辅助遗留系统重构

与普通的文档生成、注释生成相对，我们觉得从底层支持对于代码的注释生成，进而辅助系统进行重构显得更有意义。

### AutoDev 文档生成

在参考了 JetBrains AI Assistant 的文档生成思想之后，我们也在 AutoDev 中添加了文档生成这种聊胜于无的功能 —— 从个人角度而言，在有了
AIGC 之后，这种功能象征意义大于实际意义。直到我需要我为 Chocolate Factory 添加文档的时候，发现这个功能真好用。

没啥说的，选中一个类、方法、变量，右键一下，或者按一下 Alt + Enter 就可以生成了。如果原先的方法和类中已经有文档，那么将会根据现有的代码和文档重新生成（大概率，取决于
AI 的脾气了）。

如果您在实现的一个对外的 SDK，那么我更建议你采用我们在《*
*[开发者体验：探索与重塑](https://dx.phodal.com/docs/patterns/document-engineering.html)**》中定义的《**文档工程**
》的方式。诸如于我们在 Chocolate Factory 中提供的，根据测试用例代码和注释来生成真正可靠的代码。

### 自定义活文档生成

作为曾经的遗留系统重构专家，写过几个流行的重构工具、电子书，以及我们公司同事在大型保险公司的经历来看，直接根据代码生成注解形式的文档，可以大大节省阅读大量的成本。并且在已有的代码 +
新的文档的注释基础上，我们可以更好地构建 RAG 能力，进而快速从代码中梳理出真正有用的知识。

为此在 AutoDev 里，只需要添加一些 examples，就可以让 LLM 来生成对应的文档。示例：

```json
"documentations": [
    {
        "title": "Living Documentation",
        "prompt": "编写 Living Documentation。按如下的格式返回：",
        "start": "",
        "end": "",
        "type": "annotated",
        "example": {
        "question": "...",
        "answer": "..."
    }
}
```

再根据不同的场景，生成对应的注解格式，所以你也可以用它来生成 Swagger 注解，这样就可以直接生成 API 文档了。

## 代码检视

如我们在先前的文档《*
*[AIGC 重塑软件工程 Code Review 篇](https://www.phodal.com/blog/llm-empowered-software-engineering-code-review/)**
》所介绍，我们是通过在 AutoDev 结合 DevOps 平台来共同完成代码检视的。

### IDE 侧应该如何检视代码

在 IDE 侧，我们更推荐的方式是理解业务场景，结合部分的语法问题进行 review。其主要原则是，从我们日常的工作习惯来说，我们会选取多次提交（诸如一个需求的所有代码提交），再进行
Code Review。又或者是单个文件在历史周期上的变化，所以我们在设计上也是围绕于日常的使用习惯来配置的。

### 结合需求系统的 Code Review

对于考虑 AIGC 来进行研发提效的团队而言，大部分的团队已经具备了相当 DevOps 成熟度，诸如于在提交信息里结合需求 ID
来进行提交，诸如于 `feat(devops): init first review command #8`。

在这种场景之下，AutoDev 会根据这里的 8 去获取对应的需求系统的信息，以此作为业务上下文，来补充我们所需要的业务上下文，进而作为
LLM 的补充信息。

## 总结

作为一个开源项目，我们依旧有大量地不足，如果你遇到什么问题，欢迎在 GitHub 提出
issue：https://github.com/unit-mesh/auto-dev 。
