package cc.unitmesh.agent.logging

import cc.unitmesh.agent.Platform
import io.github.oshai.kotlinlogging.Level
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Logging configuration for mpp-core
 * Manages log levels and output directory settings
 */
data class LoggingConfig(
    /**
     * Current log level
     */
    val logLevel: Level = Level.INFO,
    
    /**
     * Base log directory path
     * Default: ~/.autodev/logs
     */
    val baseLogDir: String = getDefaultLogDir(),
    
    /**
     * Whether to enable file logging
     */
    val enableFileLogging: Boolean = true,
    
    /**
     * Whether to enable console logging
     */
    val enableConsoleLogging: Boolean = true,
    
    /**
     * Maximum log file size in MB
     */
    val maxLogFileSizeMB: Int = 10,
    
    /**
     * Maximum number of log files to keep
     */
    val maxLogFiles: Int = 5
) {
    
    /**
     * Get the log file path for current platform and date
     * Format: ~/.autodev/logs/${platform}-${dayNow}.log
     */
    fun getLogFilePath(platformName: String): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val dayNow = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
        return "$baseLogDir/${platformName}-${dayNow}.log"
    }
    
    companion object {
        /**
         * Default log directory: ~/.autodev/logs
         */
        private fun getDefaultLogDir(): String {
            return Platform.getLogDir()
        }
        
        /**
         * Create default configuration
         */
        fun default(): LoggingConfig = LoggingConfig()
        
        /**
         * Create debug configuration with DEBUG level
         */
        fun debug(): LoggingConfig = LoggingConfig(
            logLevel = Level.DEBUG,
            enableConsoleLogging = true,
            enableFileLogging = true
        )
        
        /**
         * Create production configuration with WARN level
         */
        fun production(): LoggingConfig = LoggingConfig(
            logLevel = Level.WARN,
            enableConsoleLogging = false,
            enableFileLogging = true
        )
    }
}

