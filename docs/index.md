---
layout: default
title: Home
description: 🧙‍AutoDev - The AI-powered coding wizard with multilingual support 🌐, auto code generation 🏗️, and a helpful bug-slaying assistant 🐞! Customizable prompts 🎨 and a magic Auto Dev/Testing feature 🧪 included! 🚀
nav_order: 1
permalink: /
---

<p align="center">
  <img src="https://plugins.jetbrains.com/files/21520/412905/icon/pluginIcon.svg" width="160px" height="160px" />
</p>

<p align="center">
  <a href="https://codecov.io/gh/unit-mesh/auto-dev" > 
    <img src="https://codecov.io/gh/unit-mesh/auto-dev/graph/badge.svg?token=5RzcndjFPx"/> 
  </a>
  <a href="https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml">
    <img src="https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml/badge.svg" alt="Build">
  </a>
  <a href="https://plugins.jetbrains.com/plugin/21520-autodev">
    <img src="https://img.shields.io/jetbrains/plugin/v/21520-autodev.svg" alt="Version">
  </a>
  <a href="https://plugins.jetbrains.com/plugin/21520-autodev">
    <img src="https://img.shields.io/jetbrains/plugin/d/21520-autodev.svg" alt="Downloads">
  </a>
  <a href="https://github.com/unit-mesh/chocolate-factory">
    <img src="https://img.shields.io/badge/powered_by-chocolate_factory-blue?logo=kotlin&logoColor=fff" alt="Powered By" />
  </a>  
</p>

> 🧙‍AutoDev: The AI-powered coding wizard with multilingual support 🌐, auto code generation 🏗️, and a helpful
> bug-slaying assistant 🐞! Customizable prompts 🎨 and a magic Auto Dev/Testing/Document feature 🧪 included! 🚀

AutoDev Overview:

<p align="center">
  <img src="autodev-overview.svg" width="100%" height="100%"  alt="Overview" />
</p>

Features:

- Languages support: Java, Kotlin, JavaScript/TypeScript, Rust, Python, Golang, C/C++/OC, or others...
- Auto development mode.
    - AutoCRUD(Spring framework）. With DevTi Protocol (like `devti://story/github/1102`) will auto
      generate Model-Controller-Service-Repository code.
    - AutoSQL. Context-aware SQL generation.
    - AutoPage. Context-aware Page generation.
    - AutoTesting. create unit test intention, auto run unit test and try to fix test.
    - AutoDocument. Auto generate document.
    - AutoArkUI. Auto generate HarmonyOS ArkUI code.
- Copilot mode
    - Pattern specific.Based on your code context like (Controller, Service `import`), AutoDev will suggest you the best
      code.
    - Related code. Based on recent file changes, AutoDev will call calculate similar chunk to generate the best code.
    - AutoDev will help you find bug, explain code, trace exception, generate commits, and more.
- Chat mode
    - Chat with AI.
    - Chat with selection code.
    - Chat with code context-aware (To be implemented).
- Customize.
    - Custom specification of prompt.
    - Custom intention action. You can add your own intention action.
    - Custom LLM Server. You can customize your LLM Server in `Settings` -> `Tools` -> `AutoDev`
    - Custom Living documentation.
    - Team prompts. Customize your team prompts in codebase, and distribute to your team.
    - Prompt override. You can override AutoDev's prompt in your codebase.
- Infrastructure / DevOps
    - CI/CD support. AutoDev will auto generate CI/CD config file.
    - Dockerfile support. AutoDev will auto generate Dockerfile.
- Built-in LLM Fine-tune
    - [UnitEval](https://github.com/unit-mesh/unit-eval) evaluate llm result
    - [UnitGen](https://github.com/unit-mesh/unit-gen) generate code-llm fine-tune data.

AutoDev fine-tune models:

| name          | model download (HuggingFace)                                    | finetune Notebook                    | model download (OpenBayes)                                                          |
|---------------|-----------------------------------------------------------------|--------------------------------------|-------------------------------------------------------------------------------------|
| DeepSeek 6.7B | [AutoDev Coder](https://huggingface.co/unit-mesh/autodev-coder) | [finetune.ipynb](finetunes/deepseek) | [AutoDev Coder](https://openbayes.com/console/phodal/models/rCmer1KQSgp/9/overview) |

### Language Support

We follow [Chapi](https://github.com/phodal/chapi) for language support tier.

| Features                  | Java | Python | Go | Kotlin | JS/TS | C/C++ | C# | Scala | Rust |
|---------------------------|------|--------|----|--------|-------|-------|----|-------|------|
| Chat Language Context     | ✅    | ✅      |    | ✅      | ✅     | ✅     |    |       | ✅    | | 
| Structure AST             | ✅    |        |    | ✅      | ✅     | ✅     | ✅  |       |      | | 
| AutoCRUD                  | ✅    |        |    | ✅      |       |       |    |       |      | | 
| Doc Generation            | ✅    | ✅      | ✅  | ✅      | ✅     |       |    |       | ✅    | | 
| Precision Test Generation | ✅    | ✅      | ✅  | ✅      | ✅     |       |    |       | ✅    | | 
| Precision Code Generation | ✅    |        |    | ✅      |       |       |    |       |      | | 

