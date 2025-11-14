package cc.unitmesh.agent.linter

/**
 * Android platform-specific linter registration
 */
actual fun registerPlatformLinters(registry: LinterRegistry) {
    // Android doesn't typically run linters directly
    // Linters are usually run on development machines
}

