package cc.unitmesh.agent.logging

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Platform-specific log directory path
 */
expect fun getPlatformLogDirectory(): String

/**
 * AutoDev 统一日志器
 * 封装 kotlin-logging，提供简洁的日志接口
 */
object AutoDevLogger {
    
    private var isInitialized = false
    private val loggers = mutableMapOf<String, KLogger>()
    
    /**
     * 初始化日志系统
     * 只需要调用一次，通常在应用启动时
     */
    fun initialize() {
        if (!isInitialized) {
            LoggingInitializer.initialize()
            isInitialized = true
            
            val logger = getLogger("AutoDevLogger")
            logger.info { "🚀 AutoDev logging system initialized" }
            logger.info { "📁 Log files location: ${getLogDirectory()}" }
        }
    }
    
    /**
     * 获取指定名称的日志器
     */
    fun getLogger(name: String): KLogger {
        return loggers.getOrPut(name) {
            KotlinLogging.logger(name)
        }
    }
    
    /**
     * 获取指定类的日志器
     */
    inline fun <reified T> getLoggerForClass(): KLogger {
        val className = T::class.simpleName ?: "Unknown"
        return getLogger(className)
    }

    /**
     * 获取调用者类的日志器
     */
    fun getCallerLogger(): KLogger {
        // 使用调用栈获取调用者类名
        val stackTrace = Thread.currentThread().stackTrace
        val callerClass = if (stackTrace.size > 2) {
            stackTrace[2].className.substringAfterLast('.')
        } else {
            "AutoDev"
        }
        return getLogger(callerClass)
    }
    
    /**
     * 获取日志目录路径（仅 JVM 平台）
     */
    fun getLogDirectory(): String {
        return getPlatformLogDirectory()
    }
    
    /**
     * 检查日志系统是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
    
    // 便捷的静态方法
    
    /**
     * 记录 INFO 级别日志
     */
    fun info(tag: String = "AutoDev", message: () -> String) {
        getLogger(tag).info(message)
    }
    
    /**
     * 记录 DEBUG 级别日志
     */
    fun debug(tag: String = "AutoDev", message: () -> String) {
        getLogger(tag).debug(message)
    }
    
    /**
     * 记录 WARN 级别日志
     */
    fun warn(tag: String = "AutoDev", message: () -> String) {
        getLogger(tag).warn(message)
    }
    
    /**
     * 记录 ERROR 级别日志
     */
    fun error(tag: String = "AutoDev", throwable: Throwable? = null, message: () -> String) {
        val logger = getLogger(tag)
        if (throwable != null) {
            logger.error(throwable, message)
        } else {
            logger.error(message)
        }
    }
    
    /**
     * 记录 TRACE 级别日志
     */
    fun trace(tag: String = "AutoDev", message: () -> String) {
        getLogger(tag).trace(message)
    }
}

/**
 * 扩展函数：为任何类提供日志功能
 */
inline fun <reified T> T.logger(): KLogger {
    return AutoDevLogger.getLoggerForClass<T>()
}

/**
 * 全局便捷函数
 */
fun autodevLog(): AutoDevLogger = AutoDevLogger
