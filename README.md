# AutoDev

<p align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" width="64px" height="64px" />
</p>

[![Build](https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml/badge.svg)](https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/21520-autodev.svg)](https://plugins.jetbrains.com/plugin/21520-autodev)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/21520-autodev.svg)](https://plugins.jetbrains.com/plugin/21520-autodev)

> AutoDev 是一款高度自动化的 AI 辅助编程工具。AutoDev 能够与您的需求管理系统（例如 Jira、Trello、Github Issue 等）直接对接。在
> IDE 中，您只需简单点击，AutoDev 会根据您的需求自动为您生成代码。您所需做的，仅仅是对生成的代码进行质量检查。

与 GitHub Copilot 差异：

| 特征    | GitHub Copilot    | AutoDev                   |
|-------|-------------------|---------------------------|
| LLM训练 | 经过大模型训练，结果更加准确    | 引入 ChatGPT 或自己训练，结果不是那么准确 |
| 返回结果  | 一次返回多个结果          | 一次返回一个完整的方法               |
| 返回类型  | 返回一个完整的类          | 返回一个完整的方法                 |
| 支持语言  | 支持多种语言            | 仅支持Java                   |
| 支持功能  | 生成代码、生成注释、生成测试用例等 | 生成代码、寻找bug、生成注释等          |
| 开源    | 不开源               | 开源                        |

结论：AutoDev 的唯一优势是开源，GitHub Copilot 完胜。

## Todos

- [X] Languages Support by PSI
    - [x] Java
- [ ] Languages Support by LSP
    - [ ] Kotlin
    - [ ] TypeScript
- [x] DevTi Protocol
    - [x] format 1: `devti://story/github/1102`
- [ ] Intelli code change
    - [x] Endpoint modify suggestions
    - [x] Controller Suggestion
        - [x] import all common imports
    - [x] Service Suggestion
    - [ ] Repository Suggestion
        - [ ] parse Gradle for dependencies
- [ ] Code AI
    - [x] Generate code
    - [ ] Generate test
    - [x] Add comments
    - [x] Code Suggestions
    - [x] Find bug...
    - [x] Explain code by selection
    - [x] Trace Exception
- [ ] Chat with IDE
    - [ ] 重构：`重构 xx 方法`
    - [ ] 重构：`重构 xx 类`
    - [ ] 替换：`替换 xx 方法`，`devti:/refactor/method`
- [x] Custom LLM Server
- [ ] Telemetry
- [ ] Context Engineering
    - [ ] Open Tabs
    - [ ] Related Files


## Usage

1. Install
    - method 1. Install from JetBrains Plugin Repository: [AutoDev](https://plugins.jetbrains.com/plugin/21520-autodev)
    - method 2. Download plugin from release page: [release](https://github.com/unit-mesh/auto-dev/releases) and install
      plugin in your IDE
2. configure GitHub Token (optional) and OpenAI config in `Settings` -> `Tools` -> `DevTi`

### Copilot mode

1. click as you want:

![Copilot Mode](https://unitmesh.cc/auto-dev/copilot-mode.png)

### AutoCRUD mode

1. add `// devti://story/github/1` comments in your code.
2. configure GitHub repository for Run Configuration.
3. click `AutoDev` button in the comments' left.

Token Configure:

![Token Configure](https://unitmesh.cc/auto-dev/configure-token.png)

Run Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/init-instruction.png)

Output Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/blog-controller.png)

### Custom OpenAI proxy example

![Custom Config](https://unitmesh.cc/auto-dev/autodev-config.png)

### 原理

AutoDev 处理过程：

1. 对接了需求系统，从需求系统中获取到需求文档
2. 根据需求文档，自动分析需求，并完善需求文档
3. 根据完善后的需求，寻找最适合的 Endpoint，即 Controller
4. 根据 Endpoint，自动生成 Controller 代码
5. 根据 Controller 代码，自动生成 Service 代码
6. 根据 Service 代码，自动生成 Repository 代码

## Development

1. `git clone https://github.com/unit-mesh/AutoDev.git`
2. open in IntelliJ IDEA
3. `./gradlew runIde`

### LSP Json RPC

```json
{
  "range": {
    "start": {
      "line": 0,
      "column": 0
    },
    "end": {
      "line": 0,
      "column": 0
    }
  },
  "text": "",
  "language": "xxx",
  "uuid": "xxx"
}
```

### API Spec

authorization: `Bearer ${token}`

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
  "find_bug": {
    "instruction": "",
    "input": ""
  }
}
```

then return:

```json
{
  "instruction": "implementation the method",
  "input": "xxxx",
  "output": "xxxx"
}
```

### How Copilot works?

- IDE with InlaysAction (extends EditorAction)
- send RPC to JSON RPC
- WASM with TreeSitter
- Return to IDE

## License

ChatUI based
on: [https://github.com/Cspeisman/chatgpt-intellij-plugin](https://github.com/Cspeisman/chatgpt-intellij-plugin)

@Thoughtworks AIEEL Team. This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
