# AutoCRUD

> AutoCRUD 是一个全自动化 AI 辅助编程工具，也是一个在大气层中（飞机上）设计的 DevTi 的 Intellij IDE 实现。

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
2. create `.env` file in root directory and add the following content:

```bash
OPENAI_KEY=YOUR_KEY
GITHUB_TOKEN=YOUR_KEY
```

## License

@Thoughtworks AIEEL Team. This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
