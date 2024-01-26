---
layout: default
title: AutoDev 1.0 发布，全流程 AI 辅助编程
nav_order: 2
parent: What's New
---

四月，在那篇《**AutoDev：AI 突破研发效能，探索平台工程新机遇》**，我们初步拟定了 AI 对于研发的影响。我们有了几个基本的假设：

- 中大型企业将**至少拥有一个**私有化的大语言模型。
- 只有构建端到端工具才能借助 AI 实现增质提效。

围绕于这些假设，我们开始构建 AutoDev，将并将它开源。也在我的博客里，写下开发中的所有心得，以期望能帮助到国内的企业构建自己的
AI 辅助编程能力。

作为一个开源项目，还是先上 GitHub 地址：https://github.com/unit-mesh/auto-dev 。

## 围绕开发者体验，设计三种辅助模式

起初，我并没有一个明确的开发蓝图。作为一个天天写代码的、所谓的专家级程序员，我是看我缺什么功能便写什么功能。

随后，围绕于所有的功能，我将其总结为三种辅助模式：

- 聊天模式
- Copilot 模式
- 补全模式

### 自动模式：规范化的代码生成

触发方式：自动模式都在 Context Actions 下，即与上下文相关的 actions。方式自然是那个那能的快捷键：**⌥⏎** (macOS) 或者 *
*Alt+Enter** (Windows/Linux)。

设计的初衷是：类似于我们在先前设计 ClickPrompt 时的一键模式。而代码并不是像网的各种炫酷的
demo，你需要考虑团队已有的软件规范和约定，否则生成的代码依旧是不可用的。于是，围绕于可配置，以及一些隐性知识的场景，我们构建了三个体现
AutoDev 的 auto 的场景：

- 自动 CRUD。读取需求系统的需求，由一个手动编码的 agent，来不断进行交互。寻找合适的 controller，修改方法，添加新的方法等等。当前支持
  Kotlin、JavaScript 语言。
- 自动生成测试。根据选定的类、方法，一键生成测试，并自动运行（在 RunConfiguration 合适的情况下）。当前支持
  JavaScript、Kotlin、Java 语言。
- 自动代码补全。根据光标位置，自动进行代码填充。由于精力不够，在不同语言能力有些差异，在 Java 语言下，会结合读取**代码规范**；在
  Kotlin、Java 语言会根据参数、返回值自动添加类作为上下文；在其它语言下，会通过“类似”（不要问是不是抄的）于 GitHub
  Copilot、JetBrains AI Assistant 的相似度算法进行计算。

每个自动模式都包含了一系列的**自动上下文**工作。如下图为**可见的**、自动代码补全的上下文示例：

![Untitled](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/4896c2bb-7356-4d15-a7d8-344e61b7b8db/Untitled.png)

在这个上下文里，结合了一些配置好的规范，以及 BlogController 类相关的 field、parameters、return type，诸如 BlogService 等。

除此，还有一些隐藏的上下文，诸如于，我们在 AutoDev 配置中声名的语言：

```kotlin
You MUST Use 中文 to return your answer !
```

所以，其实吧，因为只有这么两个 “中文”，目测有大概 50% 的机率不会触发，我在考虑要不要重复三遍。

### 伴随模式：围绕日常体验设计

在设计伴随模式时，除了围绕于自己的需求设计，还调研、参考了一系列现有工具的实现，诸如于 AI Commit 等等。

由于，伴随模式都需要等待 LLM 返回结果，所以就都扔到 `AutoDev Chat` 模式下了。

不过，我现在发现了在 JetBrains AI Assistant 出来之后，它成了 AutoDev 的最大竞争对手，当然也是参考对象。诸如于，下图的 Explain
with AI、Explain error message with AI 的体验就做得很好。在这一点上，确实有待我进一步学习的。

像 AutoDev，你只能选中，然后再 Fix This。

除了上述的功能，你还可以用 AutoDev 来：

- 生成提交信息
- 生成 release note
- 解释代码
- 重构代码
- …………

总之，别人有的，AutoDev 都可以有，还可以让你直接 create DDL。

### 聊天模式：一个边缘的功能

在腾出了时间之后，我们重新设计（其实是借鉴了 JetBrains，谁让他不支持广大的中国区用户）了 AutoDev 的 UI，并且支持一键 Chat
的方式，如图一中的 Context Actions。

你可以在这里和它聊天。

## LLM as Copilot 的思考

对于现阶段来说，LLM 是一个 Copilot。它不会不改变软件工程的专业分工，但增强每个专业技术，基于AI的研发工具平台辅助工程师完成任务，影响个体工作。

它应该**解决“我懒得做”及“我重复做”的事儿**，诸如于写单元测试、编写代码、解决 issue、提交代码等等。作为一个程序员，我们应该多挖一些坑，多做一些有创造性的设计。

在 AutoDev 里，我们关注的是：AI 如何更好地辅助人类完成工作，并且它应该是伴随在工程师的 IDE 旅程上，尽可能让工程师不离开 IDE
就可以工作。

而对于 LLM as Copilot 这一理念来说，越来越多的工具将完善一点。

作为一个资深的 AI 应用工程师，我们正在思考 **LLM as Co-Integrator** 将如何真正的提升效能。

## FAQ

### 如何接入国产、私有化 LLM ？

在项目的源码里，我们提供了一个 Custom LLM Server 的 Python 接口示例，需要将接口转为 AutoDev 所能接受的。由于精力有限，我只测试过公司内部部署的
ChatGLM2，所以接口并不是很完善。如果大家有其它需要，可以来 GitHub issue 讨论。

### 为什么只有 Intellij 版本？

作为一个开发过新的语言插件、深入过 Intellij Community、Android Studio 源码，并且优化过 Harmony OS IDE 架构的人，我真的只擅长
JetBrains IDE 的开发。

### 什么时候会有 VS Code 版？

简单来说：短期内不会有。

虽然，我也认真研究过 VS Code、X Editor 等编辑器的源码，但是兄弟姐妹们，VS Code 只是一个编辑器，不是一个 IDE
啊，它缺少太多的接口了。而如果只是简单的功能，现有的开源版本已经有很好的实现了。

除了上面的原因，还有：

其一：集成度低，开发困难。方式 1：VS Code 的 Tokenization 引擎是基于 TextMate 语法，那由 ****Oniguruma 结合又长又臭的正则表达式实现，非常不
靠谱；方式 2：基于 LSP 引擎，据我先前所试的，远景很美好。

其二：没有可供参考的代码和实现样板。如我们的 README 所提及：JetBrain plugin development is no walk in the park! Oops, we
cheekily borrowed some code from the GitHub Copilot, JetBrains Community version and the super cool JetBrains AI
Assistant plugin in our codebase. But fret not, we are working our magic to clean it up diligently!

所以，理想的方式是像 GitHub Copilot 一样，开发一套 IDE 无关的 Agent 机制，结合 TreeSitter 来实现编程语言相关的处理。

## 其它

AutoDev 的思想是将 LLM（Large Language Model）作为辅助开发者的 Copilot，通过提供辅助工具来解决一些繁琐的任务，让工程师能够更专注于有创造性的设计和思考。
