package cc.unitmesh.agent.logging

import cc.unitmesh.agent.Platform
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level

/**
 * Platform-specific logging initialization
 */
expect fun initializePlatformLogging(config: LoggingConfig)

/**
 * Logging initializer for mpp-core
 * Initializes kotlin-logging configuration with platform-specific settings
 */
object LoggingInitializer {
    
    private var isInitialized = false
    private val logger = KotlinLogging.logger {}
    
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

    private fun initializeJvm(config: LoggingConfig) {
        initializePlatformLogging(config)
        logger.debug { "JVM logging initialized with Logback backend" }
    }

    private fun initializeJs(config: LoggingConfig) {
        logger.debug { "JavaScript logging initialized with console backend" }
    }

    private fun initializeAndroid(config: LoggingConfig) {
        logger.debug { "Android logging initialized" }
    }
    
    fun isInitialized(): Boolean = isInitialized
    
    fun reset() {
        isInitialized = false
    }
    
}

/**
 * Convenience function to get a logger with automatic initialization
 */
inline fun <reified T> T.getLogger(): io.github.oshai.kotlinlogging.KLogger {
    if (!LoggingInitializer.isInitialized()) {
        LoggingInitializer.initialize()
    }
    return KotlinLogging.logger {}
}

fun getLogger(name: String): io.github.oshai.kotlinlogging.KLogger {
    if (!LoggingInitializer.isInitialized()) {
        LoggingInitializer.initialize()
    }
    return KotlinLogging.logger(name)
}
