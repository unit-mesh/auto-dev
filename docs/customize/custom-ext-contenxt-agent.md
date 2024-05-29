---
layout: default
title: Custom extension Context Agent
parent: Customize Features
nav_order: 16
permalink: /custom/extension-context-agent
---

AutoDev@1.8.6
{: .label .label-yellow }

# Custom extension Context Agent

For [#195](https://github.com/unit-mesh/auto-dev/issues/195), we introduced the concept of the extension context agent. 
The extension context agent is a new feature that allows you to customize the context of the extension.

## @autodev.ext-context.test

For example:

```json
{
    "name": "@autodev.ext-context.test",
    "description": "AutoTest",
    "url": "http://127.0.0.1:8765/api/agent/auto-test",
    "responseAction": "Direct"
}
```
