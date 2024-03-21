---
layout: default
title: AutoDev 1.7.0：AutoDev AI Agent
nav_order: 8
parent: What's New
---

在开源 AI IDE 插件 AutoDev 的  [#51](https://github.com/unit-mesh/auto-dev/issues/51) issue 中，我们设计了 AutoDev 的 AI
Agent 能力，半年后我们终于交付了这个功能。

在 AutoDev 1.7.0 中，你将可以接入内部的 AI Agent，并将其无缝与现有的 AI 辅助能力结合在一起。

本文将使用结合 AI Agent 作为 demo，来展示如何使用 AutoDev 无疑 Agent
能力。详细见文档：https://ide.unitmesh.cc/agent/agent.html ，或者阅读代码。

## 自定义 AI Agent

背景：如我们所知，通用的大语言模型是缺乏内部的相关资料的，我们需要结合 RAG 来做扩展。而在这些场景下，由于我们的资料可能会一直在更新，在
IDE 上做类似的功能是不合适的。为此，我们应该在服务端实现类似的能力，并将接口暴露给 IDE 。

因此，我们基于先前的自定义 LLM 经验，设计了 AutoDev 的自定义 AI Agent 能力，方便于

## 1. 直接返回结果示例：内部  API  集成

典型场景：

- 模型不了解的知识。在学习鸿蒙应用的开发时，也可以在内部部署对应的 API，来加速学习。
- 内部知识。当内部包含大量的领域知识、规范、 API 信息等场景

在这些场景下，可以直接使用 `Direct` 作为返回类型，集成对应的 AI Agent。对应的配置示例：

```json
{
  "name": "内部 API 集成",
  "url": "http://127.0.0.1:8765/api/agent/api-market",
  "responseAction": "Direct"
}
```

即，直接返回并处理对应的内容。

## 2. 返回检索结果示例：组件库集成

典型场景：

- 结合知识检索。即需要的是某一类的知识，而只需要基于这些知识进一步往下沟通。诸如于，我需要让 AI
  选择合适的组件，那么就需要拥有所有的组件信息，以进一步向下编码。

在这些场景下，可以直接使用 `TextChunk` 作为返回类型，集成对应的 AI Agent。对应的配置示例：

```json
{
  "name": "组件库查询",
  "url": "http://127.0.0.1:8765/api/agent/component-list",
  "responseAction": "TextChunk"
}
```

即，AI 返回的是检索的结果，我可以基于结果来进行下一步聊天。

## 3. WebView 结果示例：低代码页面生成

典型场景：

- 前端页面生成。
- 低代码页面生成。

这两种场景颇为相似，由于 AI 生成的前端代码问题多，往往需要结合内部的组件库或者 RAG 来进行。虽然如此，RAG
在这两种场景下结果也不好。与之相比，直接由 AI 生成一个原型图，交由产品经理和 UX 、 Dev 聊天显得更有价值。

在这些场景下，可以直接使用 `WebView` 作为返回类型，集成对应的 AI Agent。对应的配置示例：

```json
{
  "name": "页面生成",
  "url": "http://127.0.0.1:8765/api/agent/ux",
  "auth": {
    "type": "Bearer",
    "token": "eyJhbGci"
  },
  "responseAction": "WebView"
}
```

即，返回的结果是一个 WebView，后续在聊天中会展示对应的页面。

## 小结

在 IDE 里，我们可以将 AI Agent 视为一系列的能力插件，用于加速我们的开发过程。
