# AutoDev

> AutoDev 是一个全自动化 AI 辅助编程工具，也是一个在大气层设计的 DevTi 的 Jetbrains IDE 实现。AutoDev 直接对接到您的需求系统
> （如 Jira、Trello、Github Issue 等）中，您只需要在 IDE 中点击一下，AutoDev 将根据您的需求，自动生成代码，您只需要做好代码质检即可。

## Usage

1. Install
    - method 1. Download plugin from release page: [release](https://github.com/phodal/auto-dev/releases)
    - method 2. Install plugin in your IDE
2. configure GitHub and OpenAI config

### Copilot mode

Click as you want:

![Copilot Mode](https://unitmesh.cc/auto-dev/copilot-mode.png)

### AutoCRUD mode

1. add `// devti://story/github/1` comments in your code
3. click `AutoDev` button in the right top corner of the IDE

Token Configure:

![Token Configure](https://unitmesh.cc/auto-dev/configure-token.png)

Run Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/init-instruction.png)

Output Screenshots:

![AutoDev](https://unitmesh.cc/auto-dev/blog-controller.png)

### 原理

AutoDev 处理过程：

1. 对接了需求系统，从需求系统中获取到需求文档
2. 根据需求文档，自动分析需求，并完善需求文档
3. 根据完善后的需求，寻找最适合的 Endpoint，即 Controller
4. 根据 Endpoint，自动生成 Controller 代码
5. 根据 Controller 代码，自动生成 Service 代码
6. 根据 Service 代码，自动生成 Repository 代码

## Todos

- [ ] Languages Support
    - [x] Java
    - [ ] Kotlin
    - [ ] TypeScript
- [x] DevTi Protocol
    - [x] format 1: `devti://story/github/1102`
- [ ] Intelli code change
    - [x] Endpoint modify suggestions
    - [x] Controller Suggestion
      - [ ] import all Spring Controller imports
      - [ ] auto update imports
    - [ ] Service Suggestion
    - [ ] Repository Suggestion
    - [ ] Model Suggestion
- [ ] Code AI (ToBeContinue)
    - [ ] Analysis code
    - [ ] Auto generate code
    - [ ] Auto generate test
    - [ ] Auto generate document
    - [ ] Fix bug...

### DevTi Todos:

LoRA Training

- UserStory Analysis
- Endpoint Suggestion
- Code Suggestion

### DevTi Server

- [ ] OpenAI Proxy ?

## Development

1. `git clone https://github.com/unit-mesh/AutoDev.git`
2. open in IntelliJ IDEA

## License

@Thoughtworks AIEEL Team. This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
