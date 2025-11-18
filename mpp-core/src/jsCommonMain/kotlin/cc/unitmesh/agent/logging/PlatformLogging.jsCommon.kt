package cc.unitmesh.agent.logging

/**
 * JavaScript and WebAssembly implementation of platform-specific logging initialization
 * Both JS and Wasm use console logging, no file storage needed
 */
actual fun initializePlatformLogging(config: LoggingConfig) {
    // JavaScript/Wasm platforms use console logging by default
    // No additional configuration needed
}

/**
 * JavaScript and WebAssembly implementation of platform-specific log directory
 * Neither JS nor Wasm support file logging, return a placeholder
 */
actual fun getPlatformLogDirectory(): String {
    return "console-only" // JS/Wasm platforms don't support file logging
}
