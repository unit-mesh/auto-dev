package cc.unitmesh.agent.logging

actual fun initializePlatformLogging(config: LoggingConfig) {
    // iOS logging initialization
    // For now, we'll use simple console logging
    println("iOS logging initialized with level: ${config.logLevel}")
}

