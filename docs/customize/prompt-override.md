---
layout: default
title: Prompt Override
parent: Customize Features
nav_order: 15
---

Prompt Override ([#54](https://github.com/unit-mesh/auto-dev/issues/54)) will override the AutoDev prompt with your own
prompt.

## How to use

create a folder named `prompt/genius` in your project root directory, then create the prompt file which defined in
Supported Action.

For example, create a file named `prompts/genius/sql/sql-gen-clarify.vm`, will override the clarify prompt of AutoSQL/GenSQL

## Supported Action

```bash
├── harmonyos
│   ├── arkui-clarify.vm
│   └── arkui-design.vm
├── cicd
│   └── generate-github-action.vm
├── code
│   ├── code-complete.vm
│   ├── code-gen.vm
│   └── test-gen.vm
├── page
│   ├── page-gen-clarify.vm
│   └── page-gen-design.vm
├── practises
│   ├── code-review.vm
│   └── gen-commit-msg.vm
├── quick
│   └── quick-action.vm
├── sql
│   ├── sql-gen-clarify.vm
│   └── sql-gen-design.vm
└── sre
    └── generate-dockerfile.vm
```