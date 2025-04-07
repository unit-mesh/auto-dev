---
layout: default
title: Home
description: 🧙‍AutoDev - The AI-powered coding wizard with multilingual support 🌐, auto code generation 🏗️, and a helpful bug-slaying assistant 🐞! Customizable prompts 🎨 and a magic Auto Dev/Testing feature 🧪 included! 🚀
nav_order: 1
permalink: /
---

<p align="center">
  <img src="https://raw.githubusercontent.com/unit-mesh/auto-dev/refs/heads/master/src/main/resources/META-INF/pluginIcon.svg" width="160px" height="160px" />
</p>
<h1 align="center">AutoDev for Intellij Platform</h1>
<p align="center">
  <a href="https://codecov.io/gh/unit-mesh/auto-dev"> 
    <img src="https://codecov.io/gh/unit-mesh/auto-dev/graph/badge.svg?token=5RzcndjFPx"/> 
  </a>
  <a href="https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml">
    <img src="https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml/badge.svg" alt="Build" />
  </a>
  <a href="https://github.com/unit-mesh/chocolate-factory">
    <img src="https://img.shields.io/badge/powered_by-chocolate_factory-blue?logo=kotlin&logoColor=fff" alt="Powered By" />
  </a>  
</p>

> 🧙‍AutoDev: The AI-powered coding wizard with multilingual support 🌐, auto code generation 🏗️, and a helpful
> bug-slaying assistant 🐞! Customizable prompts 🎨 and a magic Auto Dev/Testing/Document/Agent feature 🧪 included! 🚀

- IntelliJ IDEA version: Android Studio, PyCharm, WebStorm, GoLand, RubyMine, AppCode and more.
- VSCode Version: [https://github.com/unit-mesh/auto-dev-vscode](https://github.com/unit-mesh/auto-dev-vscode)

🆕🆕🆕: New AI agent language: [https://github.com/phodal/shire](https://github.com/phodal/shire)

## AutoDev 2.0 Sketch - the Cursor Composer in Intellij IDEA

Video demo (YouTube) — English

[![Watch the video](https://img.youtube.com/vi/b1pO641-A54/sddefault.jpg)](https://youtu.be/b1pO641-A54)

**AutoDev Sketch** is an IDE canvas feature provided by [Shire](https://github.com/phodal/shire), designed to simplify
interactions and enhance the
developer experience within the IDE.

| Sketch Name                 | Description                                                                                                  | Screenshots                                                      |
|-----------------------------|--------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| **Code Sketch**             | Real-time code editor with syntax highlighting                                                               | ![](https://shire.run/images/shire-sketch-diff.png)              |
| **Diff Sketch**             | Diff content comparison tool with patch handling                                                             | ![](https://shire.run/images/shire-sketch-diff-view.png)         |
| **Terminal Sketch**         | Editable pop-up terminal interface and integration with other sketch, like frontend dev localhost in WebView | ![](https://shire.run/images/shire-sketch-terminal.png)          |
| **WebView Sketch**          | HTML/React/Ionic mockup generator with WebView display                                                       | ![](https://shire.run/images/shire-sketch-webview.png)           |
| **OpenAPI Sketch`*`**       | OpenAPI editor with bidirectional code-OpenAPI synchronization                                               | ![](https://unitmesh.cc/auto-dev/sketch-openapi.png)             |
| **Dependency Sketch`*`**    | Security-builtin vulnerable dependency checker                                                               | ![](https://unitmesh.cc/auto-dev/sketch-security-dependency.png) |
| **Go Playground Sketch`*`** | Go Playground with bidirectional code preview/edit                                                           | ![](https://unitmesh.cc/auto-dev/sketch-go-playground.png)       |
| **Mermaid Sketch`*`**       | Real-time flowchart preview/edit with bidirectional binding                                                  | ![](https://shire.run/images/shire-sketch-mermaid.png)           |
| **PlantUML Sketch`*`**      | UML diagram editor with bidirectional code-diagram synchronization                                           | ![](https://shire.run/images/shire-sketch-plantuml.png)          |
| **PlanSketch**              | Display AutoDev Planner planning for resovling code issues                                                   | ![](https://unitmesh.cc/auto-dev/autodev-plan-sketch.png)        |

`*` means requires additional plugin installation.

### Unite Your Dev Ecosystem, Create Your AI Copilot

![Inline Chat](https://shire.run/images/shire-ecology-system.png)

### Demos

Video demo (Bilibili) - 中文/Chinese

[![Watch the video](https://img.youtube.com/vi/1m3HJzw0YhA/sddefault.jpg)](https://www.bilibili.com/video/BV115c6eUEps/)

## AutoDev Architecture

Here is the AutoDev architecture:

![](autodev-arch.svg)

## AutoDev Feature Overview

<p align="center">
  <img src="autodev-overview.svg" width="100%" height="100%"  alt="Overview" />
</p>

Features:

- Sketch coding Agent
    - Agentic drive coding workflow with Sketch viewer.
- Auto development mode
    - ~~AutoCRUD (Spring framework）. With DevTi Protocol (like `devti://story/github/1102`) will auto
      generate Model-Controller-Service-Repository code.~~
    - AutoSQL (required Database plugin). Context-aware SQL generation.
    - AutoPage (React). Context-aware Web Page generation.
    - ~~AutoArkUI (HarmonyOS). Auto generate HarmonyOS ArkUI code.~~
    - Auto Testing. create unit test intention, auto run unit test and try to fix test.
    - Auto Document. Auto generate document.
- Copilot mode
    - AutoDev will help you find bug, explain code, trace exception, generate commits, and more.
    - Pattern specific. Based on your code context like (Controller, Service `import`), AutoDev will suggest the best
      code to you.
    - Related code. Based on recent file changes, AutoDev will call calculate similar chunk to generate the best code.
- Chat with AI. Chat with selection code and context-aware code.
- Customize.
    - Custom specification of prompt. For example, Controller, Service, Repository, Model, etc.
    - Custom intention action. You can add your own intention action.
    - Custom LLM Server. You can customize your LLM Server in `Settings` -> `Tools` -> `AutoDev`
    - Custom Living documentation. Customize your own living documentation, like annotation.
    - Team AI. Customize your team prompts in codebase, and distribute to your team.
    - Prompt override. You can override AutoDev's prompt in your codebase.
- SDLC
    - VCS. Generate/improve commit message, release note, and more.
    - Code Review. Generate code-review content.
    - Smart Refactoring. AI based Rename, refactoring with code smell, refactoring suggetion and more.
    - Dockerfile. Based on your project, generate Dockerfile.
    - CI/CD config. Based on build tool, generate CI/CD config file, like `.github/workflows/build.yml`.
    - Terminal. In Terminal ToolWindow, you can use custom input to generate shell/command
- Custom AI Agent
    - Executable AI Agent language: DevIns.
    - Custom AI Agent. You can integrate your own AI Agent into AutoDev.
- Model
    - Built-in LLM Fine-tune
    - [UnitEval](https://github.com/unit-mesh/unit-eval) evaluate llm result
    - [UnitGen](https://github.com/unit-mesh/unit-gen) generate code-llm fine-tune data.

## Fine-tuning model

AutoDev fine-tune models:

| name          | model download (HuggingFace)                                    | model download (OpenBayes)                                                          |
|---------------|-----------------------------------------------------------------|-------------------------------------------------------------------------------------|
| DeepSeek 6.7B | [AutoDev Coder](https://huggingface.co/unit-mesh/autodev-coder) | [AutoDev Coder](https://openbayes.com/console/phodal/models/rCmer1KQSgp/9/overview) |

### Language Support (for Fine-tuning)

We follow [Chapi](https://github.com/phodal/chapi) AST analysis engine for language support tier.

| Features                  | Java | Python | Go | Kotlin | JS/TS | C/C++ | C# | Scala | Rust |
|---------------------------|------|--------|----|--------|-------|-------|----|-------|------|
| Chat Language Context     | ✅    | ✅      | ✅  | ✅      | ✅     | ✅     |    |       | ✅    | 
| Structure AST             | ✅    |        | ✅  | ✅      | ✅     | ✅     |    |       |      | 
| Doc Generation            | ✅    | ✅      | ✅  | ✅      | ✅     |       |    |       | ✅    | 
| Precision Test Generation | ✅    | ✅      | ✅  | ✅      | ✅     |       |    |       | ✅    | 
| Precision Code Generation | ✅    |        |    | ✅      |       |       |    |       |      | 
