package cc.unitmesh.agent.linter

/**
 * iOS platform-specific linter registration
 */
actual fun registerPlatformLinters(registry: LinterRegistry) {
    // iOS doesn't typically run linters directly
    // Linters are usually run on development machines
}

