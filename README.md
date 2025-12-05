# Xiiu(X=>)per

> The full platform supported AI4SDLC agents.

- Intellij Plugin: https://plugins.jetbrains.com/plugin/29223-autodev-experiment
- VSCode Extension**: [Visual Studio Marketplace](https://marketplace.visualstudio.com/items?itemName=Phodal.autodev)
- Web Version: https://phodal.github.io/auto-dev/
- CLI Tool**: `npm install -g @autodev/cli`
- Desktop & Android see in [release pages](https://github.com/phodal/auto-dev/releases)

### Modules

| Module               | Platform            | Status              | Description                                           |
|----------------------|---------------------|---------------------|-------------------------------------------------------|
| **mpp-idea**         | IntelliJ IDEA       | ✅ Production        | Jewel UI, Agent toolwindow, code review, remote agent |
| **mpp-vscode**       | VSCode              | ✅ Production        | CodeLens, auto test/doc, MCP protocol, Tree-sitter    |
| **mpp-ui** (Desktop) | macOS/Windows/Linux | ✅ Production        | Compose Multiplatform desktop app                     |
| **mpp-ui** (CLI)     | Terminal (Node.js)  | ✅ Production        | Terminal UI (React/Ink), local/server mode            |
| **mpp-ui** (Android) | Android             | ✅ Production        | Native Android app                                    |
| **mpp-web** (Web)    | Web                 | ✅ Production        | Web app                                               |
| **mpp-server**       | Server              | ✅ Production        | JVM (Ktor)                                            |
| **mpp-ios**          | iOS                 | 🚧 Production Ready | Native iOS app (SwiftUI + Compose)                    |

### 🌟 Key Features

- **Unified Codebase**: Core logic shared across all platforms - write once, run everywhere
- **Native Performance**: Compiled natively for each platform with zero overhead
- **Full AI Agent**: Built-in Coding Agent, tool system, multi-LLM support (OpenAI, Anthropic, Google, DeepSeek, Ollama,
  etc.)
- **DevIns Language**: Executable AI Agent scripting language
- **MCP Protocol**: Model Context Protocol support for extensible tool ecosystem
- **Code Understanding**: TreeSitter-based multi-language parsing (Java, Kotlin, Python, JS, TS, Go, Rust, C#)
- **Internationalization**: Chinese/English UI support

## License

This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
