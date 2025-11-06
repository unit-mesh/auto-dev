package cc.unitmesh.agent.logging

/**
 * JVM implementation of platform-specific logging initialization
 */
actual fun initializePlatformLogging(config: LoggingConfig) {
    JvmLoggingInitializer.initializeLogback(config)
}

/**
 * JVM implementation of platform-specific log directory
 */
actual fun getPlatformLogDirectory(): String {
    return JvmLoggingInitializer.getLogDirectory()
}
