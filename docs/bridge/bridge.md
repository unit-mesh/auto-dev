---
layout: default
title: AutoDev Bridge - Legacy Migration
nav_order: 5
has_children: true
permalink: /bridge
---

# AutoDev Bridge - Legacy Code Migration

Required tool:

- [SCC](https://github.com/boyter/scc)

Required plugin:

- [OpenRewrite](https://plugins.jetbrains.com/plugin/23814-openrewrite) (Intellij IDEA Ultimate)
- [Endpoints](https://plugins.jetbrains.com/plugin/16890-endpoints) (Intellij IDEA Ultimate)

### Custom Bridge

follow [Prompt Override](/customize/prompt-override), the AI Composer can be customized. in the `prompt/code` folder,
you can create a file named `bridge.vm` to override the composer prompt.

### Custom Reasoner model

Refs to [New Config (2.0.0-beta.4+)](/quick-start#new-config-200-beta4)

