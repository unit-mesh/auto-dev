---
layout: default
title: AutoDev 1.5.3 精准测试生成
nav_order: 5
parent: What's New
---

去年年初，我们开源 AutoDev 的初衷是：

> AutoDev 是一款基于 JetBrains IDE 的开源 AI 辅助编程插件。AutoDev 能够与您的需求管理系统（例如 Jira、Trello、Github Issue
> 等）直接对接。在 IDE 中，您只需简单点击，AutoDev 会根据您的需求自动为您生成代码。您所需做的，仅仅是对生成的代码进行质量检查。 @
>

而今我们在朝这一目标的努力又更进一步了：一键生成精准的单元测试。在这篇文章中，我们将介绍从 1.4 版本（适用于团队的 Team AI）到
1.5.3 版本的一些特性：

- 精准的自动化测试生成。增强了静态代码分析能力，使得生成的构造函数更加准确；优化针对于 Spring 项目区分如何测试
  Controller、Service 的 prompt；提供不同类型的测试模板能力。
- 本地模型强化。提供了适合于 AutoDev 的 AutoDev Coder 数据集与模型；支持本地的数据记录功能，方便于进行数据蒸馏；支持部分的系统
  prompt 覆盖，即你可以更好的使用自己的模型。
- 多语言注释文档。新增 JavaScript、Rust、 Python 语言的支持，并且优化了 Kotlin 的文档生成逻辑。
- 自动流程优化。添加了 PrePush Review，即在 commit 之前，你可以使用 AI 来 review；大大简化提交信息生成的上下文，区分文件变更、依赖变更等场景，使生成的
  token 更少。

欢迎来加入我们：https://github.com/unit-mesh/auto-dev/，构建自己的 AI 辅助全流程编码助手。

在开发的过程中，我们选取了 ArchGuard 作为 AutoDev 全流程 AI 辅助的试点，ArchGuard 是一个使用 Kotlin
编写的开源架构治理平台。在过程中持续积累数据和经验，以更好地支撑 Kotlin 语言的使用体验。

## 1. 精准测试生成

结合在 ArchGuard 项目中生成了 90 个测试类 200+ 测试的用例经验，我们持续优化了的测试生成逻辑（估计还有一些 bug）。

因此，在 AutoDev 中有概率**直接生成**直接可用的单元测试。

### 精准上下文

在当前的版本里，测试的上下文除了会包含相关的类信息，还有完整的输入和输出类信息。即通过静态代码分析，获取 Service
相关的信息，也会获取每个函数的输入和输出等等信息。当一个被测试类是一个 Spring 相关的类，会判断是否是 Controller 和
Service，再给定一些测试规则。

代码实现参考 `JavaTestContextProvider`、`KotlinTestContextProvider` 的实现。

### 单元测试模板：团队 AI

在 ArchGuard 中，由于不可知的历史原因，需要编写一些特殊的注解 —— 而模型并非每次都能生成想要的。考虑到，这样的情况也会出现在大部分的项目中。因此，针对于
Controller 和 Service 与其它测试，你可以自定义单元测试的模板。

每个项目的测试逻辑是不一样的，加上我们推荐采用 prompt 即代码的方式来管理，你更可以将它分享给你的团队。

相关文档：https://ide.unitmesh.cc/customize/custom-test-template.html 。

### API 测试数据精准生成

相似的，在使用 AutoDev 的 API 测试数据生成功能时，我们也结合静态代码分析优化了对应的上下文能力，可以直接生成可用的测试数据。

详细见：`JavaTestDataBuilder` 和 `KotlinTestDataBuilder` 相关实现。

## 2. 针对本地模型优化

现在，只需要通过打开 AutoDev 配置页的 `AutoDev Coder` ，你可以针对私有化的模型做更多的配置。

### 公开模型数据的蒸馏

为了更好的测试公开的大语言模型，以及进行内部模型与工具的适配。我们在新版本中添加了 `Recording Instruction In Local`
的功能，即您可以记录与 AI 交互的数据，并以此作为内部模型微调与评估的样本。

同时，还方便于进行对应的 AutoDev Debug。

### 插件 prompt 覆盖

通过配置页，同样可以配置诸如`Explain code`、`Refactor code`、`Fix issue`、`Generate test`四个基本的 AutoDev Chat 相关的
prompt。

在进一步优化和构建内部的上下文之后，也将使用模板的方式释放出更多上下文接口。

## 3. 多语言文档

在文档上，现在可以支持 Python、 Rust、 JavaScript 语言的注释文档生成。同时，由于 OpenAI 经常为 Kotlin
类生成无用的函数注释，我们也针对这个功能进行了优化，只选取类前的注释代码。

## 4. 自动流程优化

自动化是 AutoDev 追求的主要特性，我们也在今年针对于日常开发流程初了更多的设计。在这个版本里，主要提供两个新特性。

### PrePush 检视

即在代码提交前，你可以让 AI 来辅助你进行一些初步的 review，以避免出现一些不必要的错误。

### 更流畅的提交信息生成

在 ArchGuard 项目中使用 AutoDev 重构时，我们生成了 167 次的提交信息，占所有功能的 1/3
。也因此，我们花了更多的时间在生成更好的提交信息上 —— 如何更好地控制 token。

## 其它

未来我们还将关注于：

- 流程自动化的强化。即支持更好的向前和向后流程接入，加速开发人员的编码速度。
- 交互体验优化。我们已经在代码库中引入了更好的加载和出错显示，未来也将持续丰富，毕竟没有 UX，交互上都是靠抄。
- 测试覆盖率的提升。在过去的一段时间里，由于 UI 测试速度缓慢，并且在 IDE 架构复杂，AutoDev
  的测试覆盖率是相对较低。而在静态分析相关的场景，则需要进行充分的测试，所以我们在为 AutoDev 添加更多的单元测试，以使得它更加稳定。

如果你也有兴趣，欢迎来挖坑：https://github.com/unit-mesh/auto-dev/ 。