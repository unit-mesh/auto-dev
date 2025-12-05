# Xiiu (X=>)

> Kotlin Multiplatform (KMP) to deliver a truly cross-platform AI Coding Agent ecosystem.

**Current Versions**:

- Intellij Plugin: https://plugins.jetbrains.com/plugin/29223-autodev-experiment
- MPP Modules (Core/UI/Server): `v0.3.4`
- VSCode Extension: `v0.5.x`

### 📦 Core Modules

| Module            | Platform Support            | Description                                                                  |
|-------------------|-----------------------------|------------------------------------------------------------------------------|
| **mpp-core**      | JVM, JS, WASM, Android, iOS | AI Agent engine, DevIns compiler, tool system, LLM integration, MCP protocol |
| **mpp-codegraph** | JVM, JS                     | TreeSitter-based code parsing & graph building (8+ languages)                |

### 🖥️ Client Applications

| Module               | Platform            | Status         | Description                                           |
|----------------------|---------------------|----------------|-------------------------------------------------------|
| **mpp-idea**         | IntelliJ IDEA       | ✅ Production   | Jewel UI, Agent toolwindow, code review, remote agent |
| **mpp-vscode**       | VSCode              | ✅ Production   | CodeLens, auto test/doc, MCP protocol, Tree-sitter    |
| **mpp-ui** (Desktop) | macOS/Windows/Linux | ✅ Production   | Compose Multiplatform desktop app                     |
| **mpp-ui** (CLI)     | Terminal (Node.js)  | ✅ Production   | Terminal UI (React/Ink), local/server mode            |
| **mpp-ui** (Android) | Android             | 🚧 In Progress | Native Android app                                    |
| **mpp-web** (Web)    | Web                 | 🚧 In Progress | Web app                                               |
| **mpp-ios**          | iOS                 | 🚧 In Progress | Native iOS app (SwiftUI + Compose)                    |

### ⚙️ Server & Tools

| Module             | Platform          | Features                                                 |
|--------------------|-------------------|----------------------------------------------------------|
| **mpp-server**     | JVM (Ktor)        | HTTP API, SSE streaming, remote project management       |
| **mpp-viewer**     | Multiplatform     | Universal viewer API (code, Markdown, images, PDF, etc.) |
| **mpp-viewer-web** | JVM, Android, iOS | WebView implementation, Monaco Editor integration        |

### 🌟 Key Features

- **Unified Codebase**: Core logic shared across all platforms - write once, run everywhere
- **Native Performance**: Compiled natively for each platform with zero overhead
- **Full AI Agent**: Built-in Coding Agent, tool system, multi-LLM support (OpenAI, Anthropic, Google, DeepSeek, Ollama,
  etc.)
- **DevIns Language**: Executable AI Agent scripting language
- **MCP Protocol**: Model Context Protocol support for extensible tool ecosystem
- **Code Understanding**: TreeSitter-based multi-language parsing (Java, Kotlin, Python, JS, TS, Go, Rust, C#)
- **Internationalization**: Chinese/English UI support

### 🔗 Links

- **Web Demo**: https://unit-mesh.github.io/auto-dev/
- **VSCode Extension**: [Visual Studio Marketplace](https://marketplace.visualstudio.com/items?itemName=Phodal.autodev)
- **CLI Tool**: `npm install -g @autodev/cli`

## License

This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
