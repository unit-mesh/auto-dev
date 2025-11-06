package cc.unitmesh.agent.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream

/**
 * JVM-specific logging initializer using Logback
 */
object JvmLoggingInitializer {
    
    private val logger = KotlinLogging.logger {}
    
    /**
     * Initialize Logback with custom configuration
     */
    fun initializeLogback(config: LoggingConfig) {
        try {
            // 确保日志目录存在
            ensureLogDirectoryExists(config.baseLogDir)
            
            // 设置系统属性，供 logback.xml 使用
            System.setProperty("user.home", System.getProperty("user.home"))
            
            // 获取 Logback 上下文
            val context = LoggerFactory.getILoggerFactory() as LoggerContext
            
            // 尝试加载自定义配置
            val configLoaded = loadLogbackConfig(context)
            
            if (configLoaded) {
                logger.info { "Logback configuration loaded successfully" }
                logger.info { "Log files will be stored in: ${getLogDirectory()}" }
            } else {
                logger.warn { "Failed to load custom Logback configuration, using default" }
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize Logback: ${e.message}" }
            throw e
        }
    }
    
    /**
     * 加载 Logback 配置文件
     */
    private fun loadLogbackConfig(context: LoggerContext): Boolean {
        return try {
            val configurator = JoranConfigurator()
            configurator.context = context
            
            // 清除现有配置
            context.reset()
            
            // 尝试从 classpath 加载 logback.xml
            val configStream: InputStream? = this::class.java.classLoader
                .getResourceAsStream("logback.xml")
            
            if (configStream != null) {
                configStream.use { stream ->
                    configurator.doConfigure(stream)
                }
                true
            } else {
                logger.warn { "logback.xml not found in classpath" }
                false
            }
            
        } catch (e: JoranException) {
            logger.error(e) { "Error configuring Logback: ${e.message}" }
            false
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error loading Logback config: ${e.message}" }
            false
        }
    }
    
    /**
     * 确保日志目录存在
     */
    private fun ensureLogDirectoryExists(baseLogDir: String) {
        val logDir = File(getLogDirectory())
        if (!logDir.exists()) {
            val created = logDir.mkdirs()
            if (created) {
                logger.info { "Created log directory: ${logDir.absolutePath}" }
            } else {
                logger.warn { "Failed to create log directory: ${logDir.absolutePath}" }
            }
        }
    }
    
    /**
     * 获取日志目录路径
     */
    fun getLogDirectory(): String {
        return "${System.getProperty("user.home")}/.autodev/logs"
    }
    
    /**
     * 获取当前日志文件路径
     */
    fun getCurrentLogFile(): String {
        return "${getLogDirectory()}/autodev-app.log"
    }

    /**
     * 获取错误日志文件路径
     */
    fun getErrorLogFile(): String {
        return "${getLogDirectory()}/autodev-app.log"
    }
    
    /**
     * 检查日志文件是否存在
     */
    fun logFilesExist(): Boolean {
        val logFile = File(getCurrentLogFile())
        return logFile.exists()
    }
    
    /**
     * 获取日志文件大小（字节）
     */
    fun getLogFileSize(): Long {
        val logFile = File(getCurrentLogFile())
        return if (logFile.exists()) logFile.length() else 0L
    }
}
