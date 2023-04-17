# AutoCRUD

> AutoCRUD 是一个全自动化 AI 辅助编程工具，也是一个在大气层设计的 DevTi 的 Jetbrains IDE 实现。AutoCRUD 直接对接到您的需求系统
（如 Jira、Trello、Github Issue 等）中，您只需要在 IDE 中点击一下，AutoCRUD 将根据您的需求，自动生成代码，您只需要做好代码质检即可。


## Usage

### Steps

1. add `// devti://story/github/1` comments in your code
2. configure GitHub and OpenAI config
3. click `AutoCRUD` button in the right top corner of the IDE

Token Configure:

![Token Configure](https://unitmesh.cc/autocrud/configure-token.png)

Run Screenshots:

![AutoCRUD](https://unitmesh.cc/autocrud/init-instruction.png)

Output Screenshots:

![AutoCRUD](https://unitmesh.cc/autocrud/blog-controller.png)

### 原理

AutoCRUD 处理过程：

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
    - [ ] Service Suggestion
    - [ ] Repository Suggestion
    - [ ] Model Suggestion
- [ ] Code AutoComplete
    - [ ] Smart suggestion
    - [ ] Fix bug...

### DevTi Todos:

LoRA Training

- UserStory Analysis
- Endpoint Suggestion
- Code Suggestion

### DevTi Server

- [ ] OpenAI Proxy ?

## Development

1. `git clone https://github.com/unit-mesh/autocrud.git`
2. open in Intellij IDEA

## License

@Thoughtworks AIEEL Team. This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
