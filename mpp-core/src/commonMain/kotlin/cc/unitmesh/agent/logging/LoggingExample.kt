package cc.unitmesh.agent.logging

import cc.unitmesh.agent.Platform
import io.github.oshai.kotlinlogging.Level

/**
 * Example demonstrating kotlin-logging usage in mpp-core
 */
object LoggingExample {
    
    private val logger = getLogger("LoggingExample")
    
    /**
     * Demonstrate different log levels
     */
    fun demonstrateLogging() {
        // Initialize logging with DEBUG level for demonstration
        LoggingInitializer.initialize(LoggingConfig.debug())
        
        logger.trace { "This is a TRACE message" }
        logger.debug { "This is a DEBUG message" }
        logger.info { "This is an INFO message" }
        logger.warn { "This is a WARN message" }
        logger.error { "This is an ERROR message" }
        
        // Log with exception
        try {
            throw RuntimeException("Example exception")
        } catch (e: Exception) {
            logger.error(e) { "Error occurred: ${e.message}" }
        }
        
        // Log platform information
        logger.info { "Running on platform: ${Platform.name}" }
        logger.info { "Log directory: ${Platform.getLogDir()}" }
        logger.info { "Current log file: ${LoggingInitializer.getCurrentLogFilePath()}" }
    }
    
    /**
     * Demonstrate configuration changes
     */
    fun demonstrateConfigurationChanges() {
        logger.info { "=== Configuration Demo ===" }
        
        // Change to WARN level
        GlobalLoggingConfig.updateLogLevel(Level.WARN)
        logger.debug { "This DEBUG message should not appear" }
        logger.warn { "This WARN message should appear" }
        
        // Change back to INFO level
        GlobalLoggingConfig.updateLogLevel(Level.INFO)
        logger.info { "Back to INFO level" }
        
        // Show current configuration
        val config = GlobalLoggingConfig.config
        logger.info { "Current log level: ${config.logLevel}" }
        logger.info { "File logging enabled: ${config.enableFileLogging}" }
        logger.info { "Console logging enabled: ${config.enableConsoleLogging}" }
    }
}
