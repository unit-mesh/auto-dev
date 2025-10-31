- Always run build and tests before finish.
- If my origin request/solution is not working well, please don't change it.
- Update AGENTS.md when a task working in long context (chat and history)
- Never use `./gradlew clean` , just clean for your module only, for example `./gradlew :mpp-core:clean`

### Summary

- No summary if the problem it's simple
- Always summarize a bug fix using the structure Problem → Root Cause → Solution, ensuring clarity on what broke, why it broke, and how it was resolved.
- Don't write long documentation, just use mermaid to summary

### Kotlin Multiplatform (KMP) Best Practices for MPP-CORE and MPP-UI

- **Avoid blocking APIs in commonMain**: Never use `runBlocking` in common code as it's not supported on JS/Wasm
  targets. Use `CoroutineScope` with background launch + cached results for async operations that need synchronous APIs.
- **Platform-specific implementations**: Use `expect`/`actual` declarations for platform-dependent code. For example,
  file system operations should be actual implementations per platform (JVM, JS, Wasm).
- Test for CLI: `cd /Volumes/source/ai/autocrud && ./gradlew :mpp-core:jsProductionLibraryCompileSyn
c && cd mpp-ui && npm install && node test-deepseek.js`