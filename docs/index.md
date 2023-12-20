---
layout: default
title: Home
description: 🧙‍AutoDev - The AI-powered coding wizard with multilingual support 🌐, auto code generation 🏗️, and a helpful bug-slaying assistant 🐞! Customizable prompts 🎨 and a magic Auto Testing feature 🧪 included! 🚀
nav_order: 1
permalink: /
---

<p align="center">
  <img src="https://plugins.jetbrains.com/files/21520/412905/icon/pluginIcon.svg" width="160px" height="160px" />
</p>

<p align="center">
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
> bug-slaying assistant 🐞! Customizable prompts 🎨 and a magic Auto Testing feature 🧪 included! 🚀

AutoDev Overview:

<p align="center">
  <img src="autodev-overview.svg" width="100%" height="100%"  alt="Overview" />
</p>

Features:

- Languages support: Java, Kotlin, Python, JavaScript/TypeScript, Goland, Rust or others...
- Auto development mode.
    - With DevTi Protocol (like `devti://story/github/1102`) will auto generate
      Model-Controller-Service-Repository code.
    - AutoCRUD mode (Java/Kotlin Language only）. Auto generate CRUD code.
    - Auto Testing. create unit test intention, auto run unit test and try to fix test.
- Copilot mode
    - Pattern specific.Based on your code context like (Controller, Service `import`), AutoDev will suggest you the best
      code.
    - Related code. Based on recent file changes, AutoDev will call calculate similar chunk to generate the best code.
    - AutoDev will help you find bug, explain code, trace exception, generate commits, and more.
- Chat mode
    - Chat with AI.
    - Chat with selection code.
- Customize.
    - Custom specification of prompt.
    - Custom intention action. You can add your own intention action.
    - Custom LLM Server. You can customize your LLM Server in `Settings` -> `Tools` -> `AutoDev`
    - Custom Living documentation.
    - Team prompts. Customize your team prompts in codebase, and distribute to your team.
- Miscellaneous
    - CI/CD support. AutoDev will auto generate CI/CD config file.
    - Dockerfile support. AutoDev will auto generate Dockerfile.
- Built-in LLM Fine-tune by [UnitEval](https://github.com/unit-mesh/unit-eval)

AutoDev fine-tune models:

download from [HuggingFace](https://huggingface.co/unit-mesh)

| name          | model download (HuggingFace)                                                                                  | finetune Notebook                    | jupyter notebook Log                                                     | model download (OpenBayes)                                                                                           |
|---------------|---------------------------------------------------------------------------------------------------------------|--------------------------------------|--------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| DeepSeek 6.7B | [unit-mesh/autodev-deepseek-6.7b-finetunes](https://huggingface.co/unit-mesh/autodev-deepseek-6.7b-finetunes) | [finetune.ipynb](finetunes/deepseek) | [OpenBayes](https://openbayes.com/console/phodal/containers/mzEofYrqrfc) | [deepseek-coder-6.7b-instruct-finetune-100steps](https://openbayes.com/console/phodal/models/XAyeQEC0h4Q/1/overview) |
| CodeGeeX2 6B  | TODO                                                                                                          | TODO                                 | TODO                                                                     |
