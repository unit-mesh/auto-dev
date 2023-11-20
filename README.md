<p align="center">
  <img src="plugin/src/main/resources/META-INF/pluginIcon.svg" width="160px" height="160px"  alt="logo" />
</p>
<h1 align="center">AutoDev</h1>
<p align="center">
  <a href="https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml">
    <img src="https://github.com/unit-mesh/auto-dev/actions/workflows/build.yml/badge.svg" alt="Build" />
  </a>
  <a href="https://plugins.jetbrains.com/plugin/21520-autodev">
    <img src="https://img.shields.io/jetbrains/plugin/v/21520-autodev.svg" alt="Version" />
  </a>
  <a href="https://plugins.jetbrains.com/plugin/21520-autodev">
    <img src="https://img.shields.io/jetbrains/plugin/d/21520-autodev.svg" alt="Downloads" />
  </a>
  <a href="https://github.com/unit-mesh/chocolate-factory">
    <img src="https://img.shields.io/badge/powered_by-chocolate_factory-blue?logo=kotlin&logoColor=fff" alt="Powered By" />
  </a>  
</p>

> 🧙‍AutoDev: The AI-powered coding wizard with multilingual support 🌐, auto code generation 🏗️, and a helpful
> bug-slaying assistant 🐞! Customizable prompts 🎨 and a magic Auto Testing feature 🧪 included! 🚀

[Read the docs →](https://ide.unitmesh.cc/)

AutoDev Overview:

<p align="center">
  <img src="docs/autodev-overview.svg" width="100%" height="100%"  alt="Overview" />
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
    - Related code. Based on recent file changes, AutoDev will call calculate similar chunk to generate best code.
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
    - Custom Quick Action. You can add your own quick action.
- Miscellaneous
    - CI/CD support. AutoDev will auto generate CI/CD config file.
    - Dockerfile support. AutoDev will auto generate Dockerfile.

### Demo

Video demo (Youtube) — English

[![Watch the video](https://img.youtube.com/vi/gVBTBdFV5hA/sddefault.jpg)](https://youtu.be/gVBTBdFV5hA)

Video demo (Bilibili) - 中文

[![Watch the video](https://img.youtube.com/vi/gVBTBdFV5hA/sddefault.jpg)](https://www.bilibili.com/video/BV1yV4y1i74c/)

## Useful Links

- [Copilot-Explorer](https://github.com/thakkarparth007/copilot-explorer) Hacky repo to see what the Copilot extension
  sends to the server.
- [GitHub Copilot](https://github.com/saschaschramm/github-copilot) a small part of Copilot Performance logs.
- [花了大半个月，我终于逆向分析了Github Copilot](https://github.com/mengjian-github/copilot-analysis)

## Who is using AutoDev?



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
