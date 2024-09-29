<p align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" width="160px" height="160px"  alt="logo" />
</p>
<h1 align="center">AutoDev for Intellij</h1>
<p align="center">
  <a href="https://codecov.io/gh/unit-mesh/auto-dev" > 
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

VSCode Version: [https://github.com/unit-mesh/auto-dev-vscode](https://github.com/unit-mesh/auto-dev-vscode)

[Quick Start →](https://ide.unitmesh.cc/quick-start)

[DevIns — AI Agent language for AutoDevelopment →](https://ide.unitmesh.cc/devins) which you can use follow syntax as
bridges for Human-AI-IDE:

    /write:src/main/java/cc/unitmesh/Controller.java#L1-L12

    ```java
    public class Controller {
        public void method() {
            System.out.println("Hello, World!");
        }
    }
    ```

With built-in syntax highlighting and auto-completion, you can use DevIns as next-generation AI Agent language.

## AutoDev Architecture

Here is the AutoDev architecture:

![](docs/autodev-arch.svg)

## AutoDev Feature Overview

<p align="center">
  <img src="docs/autodev-overview.svg" width="100%" height="100%"  alt="Overview" />
</p>

Features:

- Languages support: Java, Kotlin, JavaScript/TypeScript, Rust, Python, Golang, C/C++/OC, or others...
- Auto development mode
    - AutoCRUD (Spring framework）. With DevTi Protocol (like `devti://story/github/1102`) will auto
      generate Model-Controller-Service-Repository code.
    - AutoSQL (required Database plugin). Context-aware SQL generation.
    - AutoPage (React). Context-aware Web Page generation.
    - AutoArkUI (HarmonyOS). Auto generate HarmonyOS ArkUI code.
    - AutoTesting. create unit test intention, auto run unit test and try to fix test.
    - AutoDocument. Auto generate document.
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

AutoDev fine-tune models:

download from [HuggingFace](https://huggingface.co/unit-mesh)


| name          | model download (HuggingFace)                                    | model download (OpenBayes)                                                          |
|---------------|-----------------------------------------------------------------|-------------------------------------------------------------------------------------|
| DeepSeek 6.7B | [AutoDev Coder](https://huggingface.co/unit-mesh/autodev-coder) | AutoDev Coder](https://openbayes.com/console/phodal/models/rCmer1KQSgp/9/overview) |


## Language Features

### Language Support

We follow [Chapi](https://github.com/phodal/chapi) AST analysis engine for language support tier.

| Features                  | Java | Python | Go | Kotlin | JS/TS | C/C++ | C# | Scala | Rust | ArkTS |
|---------------------------|------|--------|----|--------|-------|-------|----|-------|------|-------|
| Chat Language Context     | ✅    | ✅      | ✅  | ✅      | ✅     | ✅     |    |       | ✅    | ✅     | 
| Structure AST             | ✅    |        | ✅  | ✅      | ✅     | ✅     |    |       |      |       | 
| Doc Generation            | ✅    | ✅      | ✅  | ✅      | ✅     |       |    |       | ✅    | ✅     | 
| Precision Test Generation | ✅    | ✅      | ✅  | ✅      | ✅     |       |    |       | ✅    |       | 
| Precision Code Generation | ✅    |        |    | ✅      |       |       |    |       |      |       | 
| AutoCRUD                  | ✅    |        |    | ✅      |       |       |    |       |      |       | 

### Extensions

see in [exts](exts)

## Demo

DevIns Language demo (Bilibili) - 中文

[![Watch the video](https://img.youtube.com/vi/gVBTBdFV5hA/sddefault.jpg)](https://www.bilibili.com/video/BV12H4y1W7sE/)

Video demo (YouTube) — English

[![Watch the video](https://img.youtube.com/vi/gVBTBdFV5hA/sddefault.jpg)](https://youtu.be/gVBTBdFV5hA)

Video demo (Bilibili) - 中文

[![Watch the video](https://img.youtube.com/vi/gVBTBdFV5hA/sddefault.jpg)](https://www.bilibili.com/video/BV1yV4y1i74c/)

## Useful Links

- [Copilot-Explorer](https://github.com/thakkarparth007/copilot-explorer) Hacky repo to see what the Copilot extension
  sends to the server.
- [GitHub Copilot](https://github.com/saschaschramm/github-copilot) a small part of Copilot Performance logs.
- [花了大半个月，我终于逆向分析了Github Copilot](https://github.com/mengjian-github/copilot-analysis)

## Who is using AutoDev?

Welcome to add your company here.

- Thoughtworks, a leading technology consultancy.

## License

Regarding the matter discussed in the LICENSE issue at the project's outset, we want to address the complexity of JetBrain plugin development. In the process, we referenced certain code and API designs from the JetBrains Community version and the JetBrains AI Assistant plugin. JetBrains understandably reserves the right to view this as potential infringement on their intellectual property.

Therefore, as of April 2024, AutoDev is no longer available on the JetBrains Plugin Marketplace. However, for older versions' AutoDev, you can access downloads from our Releases page. 

Additionally, we extend a warm invitation to participate in the development of the VSCode version. Your contributions are greatly appreciated.

- ChatUI based
  on: [https://github.com/Cspeisman/chatgpt-intellij-plugin](https://github.com/Cspeisman/chatgpt-intellij-plugin)
- Multiple target inspired
  by: [https://github.com/intellij-rust/intellij-rust](https://github.com/intellij-rust/intellij-rust)
- SimilarFile inspired by: JetBrains and GitHub Copilot
- DevIn Language refs
  on [JetBrains' Markdown Util](https://github.com/JetBrains/intellij-community/tree/master/platform/markdown-utils),
  which is licensed under the Apache 2.0 license.

**Known License issues**: JetBrain plugin development is no walk in the park! Oops, we cheekily borrowed some code from
the JetBrains Community version and the super cool JetBrains AI Assistant plugin in our codebase.
But fret not, we are working our magic to clean it up diligently! 🧙‍♂️✨.

Those codes will be removed in the future, you
can check it in `src/main/kotlin/com/intellij/temporary`, if you want to use this plugin in your company,
please remove those codes to avoid any legal issues.

This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
