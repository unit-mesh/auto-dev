# AutoDev

<p align="center">
  <img src="plugin/src/main/resources/META-INF/pluginIcon.svg" width="64px" height="64px" />
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
</p>

> 🧙‍AutoDev: The AI-powered coding wizard with multilingual support 🌐, auto code generation 🏗️, and a helpful
> bug-slaying assistant 🐞! Customizable prompts 🎨 and a magic Auto Testing feature 🧪 included! 🚀

Features:

- Languages support: Java, Kotlin, Python, JavaScript or others...
- Auto development mode.
    - With DevTi Protocol (like `devti://story/github/1102`) will auto generate
      Model-Controller-Service-Repository code.
    - AutoCRUD mode (Java/Kotlin Language only）. Auto generate CRUD code.
    - Auto Testing. create unit test intention, auto run unit test and try to fix test.
- Copilot mode
    - Pattern specific.Based on your code context like (Controller, Service `import`), AutoDev will suggest you the best
      code.
    - Related code. Based on recently file changes, AutoDev will call calculate similar chunk to generate best code.
    - AutoDev will help you find bug, explain code, trace exception, generate commits, and more.
- Chat mode
    - Chat with AI.
    - Chat with selection code.
- Customize.
    - Custom specification of prompt.
    - Custom intention action. You can add your own intention action.
    - Custom LLM Server. You can customize your LLM Server in `Settings` -> `Tools` -> `AutoDev`

## Document

- Usage: [docs/usage.md](docs/usage.md)
- Development: [docs/development.md](docs/development.md)
- Prompt Strategy: [docs/prompt-strategy.md](docs/prompt-strategy.md)

### Customize

- Custom Action: [docs/custom-action.md](docs/custom-action.md)
- Custom Documentation: [docs/custom-living-documentation.md](docs/custom-living-documentation.md)
- Custom LLM Server: [docs/custom-llm-server.md](docs/custom-llm-server.md)

### Development

- Custom Language: [docs/custom-language.md](docs/custom-language.md)

### Demo

Video demo (Youtube) - English

[![Watch the video](https://img.youtube.com/vi/gVBTBdFV5hA/sddefault.jpg)](https://youtu.be/gVBTBdFV5hA)

Video demo (Bilibili) - 中文

[![Watch the video](https://img.youtube.com/vi/gVBTBdFV5hA/sddefault.jpg)](https://www.bilibili.com/video/BV1yV4y1i74c/)

## Useful Links

- [Copilot-Explorer](https://github.com/thakkarparth007/copilot-explorer)  Hacky repo to see what the Copilot extension
  sends to the server.
- [Github Copilot](https://github.com/saschaschramm/github-copilot) a small part about Copilot Performance logs.
- [花了大半个月，我终于逆向分析了Github Copilot](https://github.com/mengjian-github/copilot-analysis)

## License

- ChatUI based
  on: [https://github.com/Cspeisman/chatgpt-intellij-plugin](https://github.com/Cspeisman/chatgpt-intellij-plugin)
- Multiple target inspired
  by: [https://github.com/intellij-rust/intellij-rust](https://github.com/intellij-rust/intellij-rust)
- SimilarFile inspired by: JetBrains and GitHub Copilot

**Known License issues**: JetBrain plugin development is no walk in the park! Oops, we cheekily borrowed some code from
the JetBrains Community version and the super cool JetBrains AI Assistant plugin in our codebase.
But fret not, we are working our magic to clean it up diligently! 🧙‍♂️✨.

Those code will be removed in the future, you
can check it in `src/main/kotlin/com/intellij/temporary`, if you want to use this plugin in your company,
please remove those code to avoid any legal issues.

This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
