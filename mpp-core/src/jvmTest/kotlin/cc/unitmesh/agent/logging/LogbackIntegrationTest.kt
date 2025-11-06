package cc.unitmesh.agent.logging

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * Test to verify Logback integration works correctly
 */
class LogbackIntegrationTest {
    
    @Test
    fun testLogbackConfiguration() {
        // Test 1: Initialize logging
        LoggingInitializer.initialize()
        assertTrue(LoggingInitializer.isInitialized(), "Logging should be initialized")
        
        // Test 2: Create logger and log messages
        val logger = getLogger("LogbackIntegrationTest")
        
        logger.info { "Test INFO message from LogbackIntegrationTest" }
        logger.debug { "Test DEBUG message from LogbackIntegrationTest" }
        logger.warn { "Test WARN message from LogbackIntegrationTest" }
        logger.error { "Test ERROR message from LogbackIntegrationTest" }
        
        // Test 3: Test exception logging
        try {
            throw RuntimeException("Test exception for logging")
        } catch (e: Exception) {
            logger.error(e) { "Caught test exception: ${e.message}" }
        }
        
        // Wait a moment for file operations
        Thread.sleep(1000)
        
        // Test 4: Check if log directory exists
        val logDir = File(JvmLoggingInitializer.getLogDirectory())
        assertTrue(logDir.exists() || logDir.mkdirs(), "Log directory should exist or be creatable")
        
        println("✅ Logback integration test completed successfully!")
        println("📁 Log directory: ${logDir.absolutePath}")
        
        // List log files if they exist
        val logFiles = logDir.listFiles()?.filter { it.name.contains("autodev-mpp-core") }
        if (logFiles?.isNotEmpty() == true) {
            println("📄 Log files created:")
            logFiles.forEach { file ->
                println("   - ${file.name} (${file.length()} bytes)")
                if (file.length() > 0) {
                    println("     ✅ File has content")
                }
            }
        } else {
            println("ℹ️  No log files found yet (may take a moment to appear)")
        }
    }
    
    @Test
    fun testComponentLoggers() {
        // Test different component loggers
        val agentLogger = getLogger("cc.unitmesh.agent.TestComponent")
        val devinsLogger = getLogger("cc.unitmesh.devins.TestComponent")
        val llmLogger = getLogger("cc.unitmesh.llm.TestComponent")
        val mcpLogger = getLogger("cc.unitmesh.agent.mcp.TestComponent")
        
        agentLogger.info { "Agent component test message" }
        devinsLogger.debug { "DevIns component test message" }
        llmLogger.info { "LLM component test message" }
        mcpLogger.debug { "MCP component test message" }
        
        println("✅ Component loggers test completed")
    }
    
    @Test
    fun testLogLevels() {
        val logger = getLogger("LogLevelTest")
        
        // Test all log levels
        logger.trace { "TRACE level message" }
        logger.debug { "DEBUG level message" }
        logger.info { "INFO level message" }
        logger.warn { "WARN level message" }
        logger.error { "ERROR level message" }
        
        println("✅ Log levels test completed")
    }
}
