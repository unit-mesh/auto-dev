package cc.unitmesh.agent.linter

/**
 * WASM-JS platform-specific linter registration
 */
actual fun registerPlatformLinters(registry: LinterRegistry) {
    // WASM doesn't support shell execution yet
    // Linters would need to be implemented differently
}

