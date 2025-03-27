---
layout: default
title: Project Rule
parent: AutoDev Sketch/Composer
nav_order: 3
---

# Project Rule

Notes: We prefer to use [prompts override](https://ide.unitmesh.cc/customize/prompt-override.html) to customize the
prompt, but if you want to use the project rule, you can use the following method to customize the prompt.

Since @2.0.2 version, AutoDev supports project rule. The project rule is a set of rules that are used to validate the
project structure and configuration. The project rule is defined under `prompts/rules` directory. 

## Project Rule Example

The project rule is a Markdown file that contains the following fields:

For example, the project rule file is defined as follows:

```bash
prompts/rules/
├── service.md
├── controller.md
├── repository.md
├── rpc.md
└── README.md  # will always load this file default !!
```

Then the AutoDev will auto handle the project rule by RuleInsCommand, you can use the following command to check the project
rule:

```devin
/rule:service
```
