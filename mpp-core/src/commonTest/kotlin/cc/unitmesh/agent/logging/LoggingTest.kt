package cc.unitmesh.agent.logging

import io.github.oshai.kotlinlogging.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoggingTest {
    
    @Test
    fun testLoggingConfigCreation() {
        val config = LoggingConfig.default()
        assertEquals(Level.INFO, config.logLevel)
        assertTrue(config.enableFileLogging)
        assertTrue(config.enableConsoleLogging)
    }
    
    @Test
    fun testDebugConfig() {
        val config = LoggingConfig.debug()
        assertEquals(Level.DEBUG, config.logLevel)
        assertTrue(config.enableFileLogging)
        assertTrue(config.enableConsoleLogging)
    }
    
    @Test
    fun testProductionConfig() {
        val config = LoggingConfig.production()
        assertEquals(Level.WARN, config.logLevel)
        assertFalse(config.enableConsoleLogging)
        assertTrue(config.enableFileLogging)
    }

    @Test
    fun testLoggerCreation() {
        val logger = getLogger("TestLogger")
        // Just verify we can create a logger without errors
        assertTrue(true) // If we get here, logger creation succeeded
    }
    
    @Test
    fun testLoggingInitializer() {
        // Reset state
        LoggingInitializer.reset()
        assertFalse(LoggingInitializer.isInitialized())
        
        // Initialize with default config
        LoggingInitializer.initialize()
        assertTrue(LoggingInitializer.isInitialized())
        
        // Initialize again should not cause issues
        LoggingInitializer.initialize()
        assertTrue(LoggingInitializer.isInitialized())
    }
    
    @Test
    fun testLogFilePath() {
        val config = LoggingConfig.default()
        val logPath = config.getLogFilePath("jvm")
        
        // Should contain platform name and date
        assertTrue(logPath.contains("jvm"))
        assertTrue(logPath.contains(".log"))
        assertTrue(logPath.contains("/.autodev/logs/"))
    }
}
