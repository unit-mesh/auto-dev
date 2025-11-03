## Core Rules

- Always run the build and tests before completing a task, making sure they pass.
- Put test scripts under `docs/test-scripts`.
- If an existing request/solution does not work, preserve its intent.
- Do not run `./gradlew clean`. Clean only the specific module, e.g., `./gradlew :mpp-core:clean`.

## Summary

- Omit a summary if the problem is simple.
- For bug fixes, summarize as: Problem → Root Cause → Solution.
- Keep summary short if need. Use Mermaid for long chat only.

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

1. Build MPP Core:
    - `cd /Volumes/source/ai/autocrud && ./gradlew :mpp-core:assembleJsPackage`
2. Build and run MPP CLI:
    - `cd mpp-ui && npm run build:ts && node dist/index.js`
 