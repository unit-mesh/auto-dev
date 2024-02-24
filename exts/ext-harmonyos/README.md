# HarmonyOS Extension

openharmony/lib/ohos-info-center-plugin-3.1.0.501, API Reference，可惜使用的是远程 API 。
openharmony/lib/javascript-3.1.0.501.jar, 自研 JavaScript PSI 模块
openharmony/lib/ohos-cpp-lsp-client-3.1.0.501.jar, 自研 C++ PSI 模块

JS 类：`class class com.huawei.ace.language.psi.impl.JavaScriptIdentifierNameImpl`
CPP 类：`class class com.huawei.ideacpp.psi.impl.CPPIdentifierImpl`

## 设计思念

三个新要素：新的语言、遗留系统迁移、新的 UI 框架。

而且结合源码中 HiStudio 等信息的存在，我猜这会是一个自研的 IDE，所以暂时不会在 AutoDev 添加一些额外新的功能。

而在标准的 JetBrains 是以 ObjectiveC 作为抽象，所以又存在一系列的差异，又会是一个新的头疼问题，诸如于：

CCELanguage, CLanguage, CPPLanguage，所以理由上拿到  CannonicalName  会是一个更好的选择。

## ArkTS 支持

>
ArkTS是鸿蒙生态的应用开发语言。它在保持TypeScript（简称TS）基本语法风格的基础上，对TS的动态类型特性施加更严格的约束，引入静态类型。同时，提供了声明式UI、状态管理等相应的能力，让开发者可以以更简洁、更自然的方式开发高性能应用。

JetBrains 的 JavaScript 语法支持并没有免费的版本，所以 DevEco Studio 自研了一个 JavaScript/TypeScript 模块，支持
JavaScript
语法高亮、代码提示、代码格式化等功能。与此同时，DevEco Studio 添加了 ArkTS 语言，即 TypeScript 扩展语法。

这就意味着，使用 DevEco Studio + AutoDev 时，会出现三种新的文件类型：

- Huawei JavaScript
- Huawei TypeScript
- Huawei ArkTS

而由于 DevEco Studio 似乎并没有发布这些包，即与 JetBrains 体系集成。所以，通过普通的方式是难以进行调试和测试的。

## Auto ArkUI 生成

> ArkUI是一套构建分布式应用界面的声明式 UI 开发框架。

简单来说，它与 AutoDev 之前的 AutoPage 并没有太大的区别。

## Android/iOS 迁移

由于 JetBrains 的 AppCode 已经不维护了，所以事实上，只能使用结合 DevEcoStudio 或者是 Android Studio 进行 Android 迁移。

