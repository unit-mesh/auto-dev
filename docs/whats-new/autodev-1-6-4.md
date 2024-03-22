---
layout: default
title: AutoDev 1.6.4：HarmonyOS 应用开发体验提升
nav_order: 7
parent: What's New
---

生成式 AI 在软件研发和知识管理上，有着非常大的潜力，也因此这项技术被越来越多的企业所采用。而在一些新兴的技术上，诸如于鸿蒙操作系统，它带来了一些新
的理念、开发工具 DevEco Studio、新的语言 ArkTS、新的 UI 框架 ArkUI 等等。从模式上来说，它与生成式 AI 结合企业内部的基础设施过程非常相似。

因此，我们开始在 AutoDev 中探索如何结合这些新知识的可能性，同时降低开发人员的学习负担。

源码：https://github.com/unit-mesh/auto-dev

## 鸿蒙操作系统 + 生成式 AI 的三个试验式功能

在初步听鸿蒙团队介绍完 HarmonyOS 的一些自研工具之后，便有了三个在 AutoDev 试验的思路：

- 添加 ArkTS 支持。ArkTS 是鸿蒙生态中基于 TypeScript 扩展的应用开发语言。
- 自动 ArkUI 页面生成。ArkUI 是一套构建分布式应用界面的声明式 UI 开发框架。它与我们先前引入的 AutoPage
  并没有太多的区别，可以结合思维链进行代码和 UI 生成。
- UI 布局迁移。即将其它语言、框架编写的代码，交由生成式 AI 转化成适用于鸿蒙的代码。

作为阅读过 Gradle、Intellij Community、DevEcoStudio 源码，以及《前端架构：从入门到微前端》作者，我大抵算是对于 TypeScript、
ArkUI、
声明式 UI 有一定的经验，所以我自信的开始了 AutoDev 的新功能开发 —— 然后就踩了一堆坑。

## 1. ArkTS 语言的 AI 支持

在我下载安装完 DevEco Studio 之后，发现 AutoDev 居然不支持 TypeScript？？？经过我在 WebStorm 反复测试后，发现是 IDE 的关系。结合
PSIViewer 插件后，
才发现差异之后，DevEco Studio 的 JavaScript/TypeScript
语言是自己实现的，诸如于：`com.huawei.ace.language.psi.impl.JavaScriptIdentifierNameImpl`。

原因不外乎：

- Intellij 平台中的 JavaScript 插件是收费的，没有开源版本。
- 鸿蒙直接针对于 TypeScript 语法进行扩展，会比实现一个新的更简单。

所以 DevEco Studio 自研了一个 JavaScript/TypeScript 模块，支持 JavaScript 语法高亮、代码提示、代码格式化等功能。与此同时，DevEco
Studio
添加了 ArkTS 语言，即 TypeScript 扩展语法。

这就意味着，使用 DevEco Studio + AutoDev 时，会出现三种新的文件类型：

- Huawei JavaScript
- Huawei TypeScript
- Huawei ArkTS

头疼。。

为此，在 AutoDev 中采取的方法是，其于标准 PSI 做初步的抽象，以实现对于文档生成的支持。而如果要做好则需要：

1. 基于反射来重复利用 JavaScript PSI
2. 融入 DevEco Studio 的 JavaScript 支持

当然，考虑到调试上的难度，以前代码中各种现的 xxStudio 字眼（新的自研 IDE 平台？？），我暂时放弃了上述的做法：大体上鸿蒙 IDE
会有自己的 AI 能力。

## 2. AutoArkUI：RAG 增强的 ArkUI 代码生成

> ArkUI 是一套构建分布式应用界面的声明式 UI 开发框架。

与 ArkTS 相比，要结合 ArkUI 显得稍微复杂一些。 所以，我在当前版本里考虑的是：结合经典 UI 的元素生成页面，即：

- 布局。诸如于：线性布局（Row、Column）、层叠布局（Stack）、弹性布局（Flex）等。
- 组件。诸如于：按钮（Button）、单选框（Radio）、切换按钮（Toggle）等。

而由于 ChatGPT 是不包含 HarmonyOS 的最新知识的，所以需要采用类似于 AutoPage 的两步生成特性。

1. 分析用户的需求，选择合适的布局与组件。
2. 根据用户的需求与详细的布局、组件信息，生成对应的 ArkUI 代码。

上述的两步便是 AutoDev 中 AutoArkUi 生成 UI 的特性，详细可以参考 AutoDev 的代码，以及对应的 prompt。如下是对应的步骤 1 的
prompt：

- User: // maybe send Android Layout code, maybe some requirements
- Your Answer: [FlexLayout, Button, CheckBox, Checkbox, Button]

考虑到编程语言 DSL（领域特定语言）极易受用户语言的影响，所以采用的是英语的方式，避免无端生成中文 DSL 。

## 3. 迁移 Android/iOS/小程序 应用

生成式 AI 具备极好的代码翻译能力。诸如于 IBM 在 Cobol 转化为 Java 上的工程化设计，以及我们在 AutoDev 中设计的遗留系统改造能力，其所针对的
都是生成 AI 在这方面的能力。

所以，我们也在 AutoDev 中内置了这个功能，只是当前支持的只是布局上的迁移。但是，考虑到这种生成方式依旧有一系列的问题，有待我们进一步寻找更好的方式。
类似的问题在生成 ArkUI 也是存在的。

相似的，这个功能目前是与 AutoArkUI 融合在一起的，理论上通过静态代码分析是最简单的，有待未来进一步完善。

## 4. RAG 增强的聊天上下文：C++ NAPI 等

在试验了多次之后，会发现对于 HarmonyOS 这种新知识，ChatGPT 是不知道的。所以，需要基于 AutoDev 的上下文接口，创建基于
HarmonyOS 的上下文。
当然的版本（1.6.4）里， 添加的是：`This project is a HarmonyOS project.` （毫无意义的废话），再结合不同语言来写一些上下文：

- TypeScript/JavaScript/ArkTS. Which use TypeScript (ArkTS) as the main language, and use Flutter like TypeScript UI
  framework.
- CPP/"C/C++"/CCE. Which use C++ as the main language, and NAPI for building native Addons.

大体来说，就是告诉 AI：

- 编写 ArkUI/前端代码的时候，考虑一下这个项目是类似于 Flutter 的声明式 UI 。
- 编写原生代码的时候，考虑一下这个项目是基于 NAPI 来构建插件的。

当然了，这些是基于我的初步理解所构建的上下文，

## 未来

考虑到上述的功能，就是几小时内实现的，就不要有太高的期望了。

当前版本依旧有诸多问题：

- 转换 Android 布局易瞎编。除了需要知道更多的转换规则，还需要知识更多的属性，而这些部分是通过传统的代码分析工具解决的
- 组件和布局信息的 hardcode。懂的都懂
- 缺少示例代码。没有动态生成的示例代码，使得 RAG 的效果是有限的
- 诸如于 C++ 语言的支持
- 微信小程序等小程序平台的转换

然而我并非 Android、小程序应用迁移到鸿蒙应用的专家，所以还是有一系列的挑战。等我心情好的时候，再考虑写一些更好玩的新特性。
