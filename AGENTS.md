## Core Rules

- Always run build and tests before finishing a task.
- If the original request/solution is not working well, do not change its intent. Propose a minimal fix or report issues.
- Never run `./gradlew clean`. Clean only the specific module, e.g., `./gradlew :mpp-core:clean`.

## Summary

- Omit a summary if the problem is simple.
- For bug fixes, summarize as: Problem → Root Cause → Solution.
- Keep it short. Use Mermaid for visual summaries.

## Kotlin Multiplatform \(KMP\) Best Practices for `mpp-core` and `mpp-ui`

- Avoid blocking APIs in `commonMain`. Never use `runBlocking` in common code \(JS/Wasm unsupported\).
- Use `CoroutineScope` with background `launch` + cached results to support sync-like APIs.
- Use `expect`/`actual` for platform-specific code \(e.g., file I/O on JVM/JS/Wasm\).
- Check export first, if some functions not working well with CLI (TypeScript)
- JS exports:
    - Use `String`, not `Char`.
    - Convert enums to strings.
    - Avoid `Flow`; use `Promise` and callbacks.

## AutoDev CLI Quick Test

- Test scripts for CLI put at mpp-ui/test-scripts
- Test CLI
  1. Build MPP Core:
      - `cd /Volumes/source/ai/autocrud && ./gradlew :mpp-core:assembleJsPackage`
  2. Build and run MPP CLI:
      - `cd mpp-ui && npm run build:ts && node dist/index.js`
 