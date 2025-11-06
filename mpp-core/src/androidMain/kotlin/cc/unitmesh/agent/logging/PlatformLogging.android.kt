package cc.unitmesh.agent.logging

/**
 * Android implementation of platform-specific logging initialization
 * Android uses Logcat for logging
 */
actual fun initializePlatformLogging(config: LoggingConfig) {
    // Android uses Logcat, which is automatically configured
    // No additional setup needed
}

/**
 * Android implementation of platform-specific log directory
 */
actual fun getPlatformLogDirectory(): String {
    // Android typically uses app-specific storage
    // This will be overridden by AndroidPlatform.getLogDir()
    return "/data/data/cc.unitmesh.autodev/files/logs"
}

