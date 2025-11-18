package cc.unitmesh.agent.logging

/**
 * JavaScript implementation of platform-specific logging initialization
 * JS uses console logging, no file storage needed
 */
actual fun initializePlatformLogging(config: LoggingConfig) {
    // JavaScript platform uses console logging by default
    // No additional configuration needed
}

/**
 * JavaScript implementation of platform-specific log directory
 * JS doesn't support file logging, return a placeholder
 */
actual fun getPlatformLogDirectory(): String {
    return "console-only" // JS platform doesn't support file logging
}
