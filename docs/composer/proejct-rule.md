---
layout: default
title: Project Rule
parent: AutoDev Sketch/Composer
nav_order: 3
---

# Project Rule

Since @2.0.2 version, AutoDev supports project rule. The project rule is a set of rules that are used to validate the project
structure and configuration. The project rule is defined under `prompts/rules` directory. The project rule is a Markdown file that contains the following fields:

For example, the project rule file is defined as follows:

```bash
prompts/rules/
├── service.md
├── controller.md
├── repository.md
├── rpc.md
└── README.md  # will always load this file default !!
```

Then the LLM will auto handle the project rule by RuleInsCommand, like:

```devin
/rule:service
```
