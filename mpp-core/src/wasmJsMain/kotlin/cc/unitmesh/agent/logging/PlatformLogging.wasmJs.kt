package cc.unitmesh.agent.logging

/**
 * WebAssembly implementation of platform-specific logging initialization
 * WASM uses console logging, no file storage available
 */
actual fun initializePlatformLogging(config: LoggingConfig) {
    // WebAssembly platform uses console logging by default
    // No additional configuration needed
}

/**
 * WebAssembly implementation of platform-specific log directory
 * WASM doesn't support file logging, return a placeholder
 */
actual fun getPlatformLogDirectory(): String {
    return "console-only" // WASM platform doesn't support file logging
}
