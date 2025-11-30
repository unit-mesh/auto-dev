## Core Rules

- Current project is a Kotlin multiplatform project, always consider the multiplatform aspect, JS, WASM, Desktop JVM, Android, iOS
- Current project is a Kotlin multiplatform project, always consider the multiplatform aspect, JS, WASM, Desktop JVM, Android, iOS
- Current project is a Kotlin multiplatform project, always consider the multiplatform aspect, JS, WASM, Desktop JVM, Android, iOS

Rest:

- Always run the build and tests before completing a task, making sure they pass.
- If an existing request/solution does not work, preserve its intent.
- Do not run `./gradlew clean`. Clean only the specific module, e.g., `./gradlew :mpp-core:clean`.
- Put temporary test scripts under `docs/test-scripts`.

## Debug

- Log save in `~/.autodev/logs/autodev-app.log`

## Summary

- Omit a summary if the problem is simple. For bug fixes, summarize as: Problem → Root Cause → Solution. Keep summary
  short if need. Use Mermaid for long chat only.

## Kotlin Multiplatform \(KMP\) Best Practices for `mpp-core` and `mpp-ui`

- Use `expect`/`actual` for platform-specific code \(e.g., file I/O on JVM/JS/Wasm\).
- Check export first, if some functions not working well with CLI (TypeScript)
- 在 Kotlin/JS 的 @JsExport 中：
    - Avoid `Flow`, use `Promise`
    - ✅ 使用具体类作为返回类型和参数类型
    - ❌ 避免使用接口类型（JS 端无法正确处理接口的类型转换）
- For WASM platform, we should not use emoji and utf8 in code.
- Use ./gradlew :mpp-ui:generateI18n4kFiles for i18n

## Design System \(Color & Theme\)

- **CLI/TUI (TypeScript)**: Use `mpp-ui/src/jsMain/typescript/design-system/` → Import `semanticInk` / `semanticChalk`
- **Compose (Desktop/Android)**: Use `AutoDevColors` from `cc.unitmesh.devins.ui.compose.theme` → Or `MaterialTheme.colorScheme`
- **DO NOT hardcode colors** \(e.g., `Color(0xFF...)` or `#hex`\). Always use design tokens for consistency across platforms.
- **Docs**: See `docs/design-system-color.md` (TypeScript) and `docs/design-system-compose.md` (Kotlin Compose)

## AutoDev CLI Quick Test

1. Build MPP Core: `cd /Volumes/source/ai/autocrud && ./gradlew :mpp-core:assembleJsPackage`
2. Build and run MPP CLI: `cd mpp-ui && npm run build && npm run start`

## IntelliJ IDEA Plugin (mpp-idea)

`mpp-idea` is a standalone Gradle project with `includeBuild` dependency on parent project.

**Build Commands:**
```bash
# Compile (from project root)
cd mpp-idea && ../gradlew compileKotlin

# Run tests (JewelRendererTest uses JUnit 5, no IntelliJ Platform required)
cd mpp-idea && ../gradlew test --tests "cc.unitmesh.devins.idea.renderer.JewelRendererTest"

# Build plugin
cd mpp-idea && ../gradlew buildPlugin
```

**Notes:**
- Do NOT use `./gradlew :mpp-idea:compileKotlin` from root - use `cd mpp-idea && ../gradlew` instead
- `IdeaAgentViewModelTest` requires IntelliJ Platform Test Framework
- `JewelRendererTest` can run standalone with JUnit 5

## Release

1. modify version in `gradle.properties`
2. publish cli version: `cd mpp-ui && npm publish:remote`
3. publish Desktop: `git tag compose-vVersion` (same in `gradle.properties`), `git push origin compose-vVersion`
4. draft release in GitHub, run gh cli: `gh release create compose-vVersion --draft`

