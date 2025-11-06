package cc.unitmesh.agent.logging

import cc.unitmesh.agent.Platform
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level

/**
 * Logging initializer for mpp-core
 * Initializes kotlin-logging configuration with platform-specific settings
 */
object LoggingInitializer {
    
    private var isInitialized = false
    private val logger = KotlinLogging.logger {}
    
    /**
     * Initialize logging with the given configuration
     */
    fun initialize(config: LoggingConfig = LoggingConfig.default()) {
        if (isInitialized) {
            logger.debug { "Logging already initialized, skipping..." }
            return
        }
        
        try {
            initializePlatformSpecific(config)
            
            isInitialized = true
            
            logger.info { "Logging initialized for platform: ${Platform.name}" }
            logger.info { "Log level: ${config.logLevel}" }
            logger.info { "Log directory: ${config.baseLogDir}" }
            logger.debug { "File logging enabled: ${config.enableFileLogging}" }
            logger.debug { "Console logging enabled: ${config.enableConsoleLogging}" }
            
        } catch (e: Exception) {
            println("Failed to initialize logging: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Initialize with log level from string
     */
    fun initialize(logLevelString: String) {
        val level = when (logLevelString.uppercase()) {
            "TRACE" -> Level.TRACE
            "DEBUG" -> Level.DEBUG
            "INFO" -> Level.INFO
            "WARN" -> Level.WARN
            "ERROR" -> Level.ERROR
            else -> Level.INFO
        }
        
        val config = LoggingConfig.default().copy(logLevel = level)
        initialize(config)
    }
    
    /**
     * Platform-specific initialization
     */
    private fun initializePlatformSpecific(config: LoggingConfig) {
        when {
            Platform.isJvm -> initializeJvm(config)
            Platform.isJs -> initializeJs(config)
            Platform.isAndroid -> initializeAndroid(config)
            else -> {
                logger.warn { "Unknown platform: ${Platform.name}, using default configuration" }
            }
        }
    }
    
    /**
     * JVM-specific initialization
     */
    private fun initializeJvm(config: LoggingConfig) {
        // JVM uses SLF4J backend, configuration is handled by slf4j-simple
        logger.debug { "JVM logging initialized with SLF4J backend" }
    }
    
    /**
     * JavaScript-specific initialization
     */
    private fun initializeJs(config: LoggingConfig) {
        // JS logging goes to console by default
        logger.debug { "JavaScript logging initialized with console backend" }
    }
    
    /**
     * Android-specific initialization
     */
    private fun initializeAndroid(config: LoggingConfig) {
        // Android logging configuration
        logger.debug { "Android logging initialized" }
    }
    
    /**
     * Check if logging is initialized
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Reset initialization state (for testing)
     */
    fun reset() {
        isInitialized = false
    }
    
}

/**
 * Convenience function to get a logger with automatic initialization
 */
inline fun <reified T> T.getLogger(): io.github.oshai.kotlinlogging.KLogger {
    // Ensure logging is initialized with default config
    if (!LoggingInitializer.isInitialized()) {
        LoggingInitializer.initialize()
    }
    return KotlinLogging.logger {}
}

/**
 * Convenience function to get a logger with name
 */
fun getLogger(name: String): io.github.oshai.kotlinlogging.KLogger {
    // Ensure logging is initialized with default config
    if (!LoggingInitializer.isInitialized()) {
        LoggingInitializer.initialize()
    }
    return KotlinLogging.logger(name)
}
