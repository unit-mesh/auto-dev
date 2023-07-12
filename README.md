# AutoDev

<p align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" width="64px" height="64px" />
</p>

[![Build](https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml/badge.svg)](https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/21520-autodev.svg)](https://plugins.jetbrains.com/plugin/21520-autodev)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/21520-autodev.svg)](https://plugins.jetbrains.com/plugin/21520-autodev)

> AutoDev 是一款高度自动化的 AI 辅助编程工具。AutoDev 能够与您的需求管理系统（例如 Jira、Trello、Github Issue 等）直接对接。在
> IDE 中，您只需简单点击，AutoDev 会根据您的需求自动为您生成代码。您所需做的，仅仅是对生成的代码进行质量检查。

features:

- languages support: Java, Kotlin (maybe ?)
- Auto development mode. With DevTi Protocol (like `devti://story/github/1102`) will auto generate Controller-Service-Model code.
- Smart code completion. Based on your code context like (Controller, Service `import`), AutoDev will suggest you the best code.
- AI assistant. AutoDev will help you find bug, explain code, trace exception, and more.
- Custom prompt. You can custom your prompt in `Settings` -> `Tools` -> `DevTi`
- Custom LLM Server. You can custom your LLM Server in `Settings` -> `Tools` -> `DevTi`
- [ ] Smart architecture. With ArchGuard Co-mate DSL, AutoDev will help you design your architecture.

## Usage

1. Install
    - method 1. Install from JetBrains Plugin Repository: [AutoDev](https://plugins.jetbrains.com/plugin/21520-autodev)
    - method 2. Download plugin from release page: [release](https://github.com/unit-mesh/auto-dev/releases) and install
      plugin in your IDE
2. configure GitHub Token (optional) and OpenAI config in `Settings` -> `Tools` -> `DevTi`

![Token Configure](https://unitmesh.cc/auto-dev/autodev-config.png)

### CodeCompletion mode

Right click on the code editor, select `AutoDev` -> `CodeCompletion` -> `CodeComplete`

![Code completion](https://unitmesh.cc/auto-dev/completion-mode.png)

### Copilot mode

1. click as you want:

![Copilot Mode](https://unitmesh.cc/auto-dev/copilot-mode.png)

### AutoCRUD mode

1. add `// devti://story/github/1` comments in your code.
2. configure GitHub repository for Run Configuration.
3. click `AutoDev` button in the comments' left.

Run Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/init-instruction.png)

Output Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/blog-controller.png)

## Development

1. `git clone https://github.com/unit-mesh/AutoDev.git`
2. open in IntelliJ IDEA
3. `./gradlew runIde`

### Custom prompt

```json
{
  "auto_complete": {
    "instruction": "",
    "input": ""
  },
  "auto_comment": {
    "instruction": "",
    "input": ""
  },
  "code_review": {
    "instruction": "",
    "input": ""
  },
  "refactor": {
    "instruction": "",
    "input": ""
  },
  "write_test": {
    "instruction": "",
    "input": ""
  }
}
```

## License

ChatUI based
on: [https://github.com/Cspeisman/chatgpt-intellij-plugin](https://github.com/Cspeisman/chatgpt-intellij-plugin)

@Thoughtworks AIEE Team. This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
