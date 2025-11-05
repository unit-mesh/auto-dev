package cc.unitmesh.devins.compiler.context

import cc.unitmesh.devins.compiler.result.DevInsCompiledResult
import cc.unitmesh.devins.compiler.variable.VariableTable
import cc.unitmesh.devins.compiler.variable.VariableScope
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.agent.logging.getLogger
import io.github.oshai.kotlinlogging.KLogger
import kotlin.time.Clock

/**
 * 编译器上下文
 * 保存编译过程中的状态信息
 */
class CompilerContext {
    
    /**
     * 输出缓冲区
     */
    val output: StringBuilder = StringBuilder()
    
    /**
     * 编译结果
     */
    val result: DevInsCompiledResult = DevInsCompiledResult()
    
    /**
     * 变量表
     */
    val variableTable: VariableTable = VariableTable()
    
    /**
     * 日志记录器
     */
    val logger: CompilerLogger = CompilerLogger()
    
    /**
     * 是否跳过下一个代码块
     */
    var skipNextCode: Boolean = false
    
    /**
     * 编译选项
     */
    var options: CompilerOptions = CompilerOptions()
    
    /**
     * 项目文件系统
     * 用于访问项目文件，支持 SpecKit 等功能
     */
    var fileSystem: ProjectFileSystem = EmptyFileSystem()
    
    /**
     * 添加输出内容
     */
    fun appendOutput(text: String) {
        output.append(text)
    }
    
    /**
     * 设置错误状态
     */
    fun setError(hasError: Boolean, errorMessage: String? = null) {
        result.hasError = hasError
        if (errorMessage != null) {
            result.errorMessage = errorMessage
        }
    }
    
    /**
     * 检查是否有错误
     */
    fun hasError(): Boolean = result.hasError
    
    /**
     * 重置上下文状态
     */
    fun reset() {
        output.clear()
        result.reset()
        // 保留用户定义的变量，只清除内置变量
        resetVariableTable()
        skipNextCode = false
    }

    /**
     * 重置变量表，保留用户定义的变量
     */
    private fun resetVariableTable() {
        // 保存用户定义的变量
        val userVariables = variableTable.getVariablesByScope(VariableScope.USER_DEFINED)

        // 清空变量表
        variableTable.clear()

        // 重新添加用户定义的变量
        userVariables.forEach { (name, info) ->
            variableTable.addVariable(name, info.type, info.value, info.scope)
        }
    }
}

/**
 * 编译器选项
 */
data class CompilerOptions(
    /**
     * 是否启用调试模式
     */
    val debug: Boolean = false,

    /**
     * 是否启用严格模式
     */
    val strict: Boolean = false,

    /**
     * 最大递归深度
     */
    val maxRecursionDepth: Int = 100,

    /**
     * 是否启用模板编译
     */
    val enableTemplateCompilation: Boolean = true,

    /**
     * 是否保留原始输出
     */
    val keepRawOutput: Boolean = false
)

/**
 * 编译器日志记录器
 * 使用 kotlin-logging 替代 println
 */
class CompilerLogger {

    private val logger: KLogger = getLogger("DevInsCompiler")
    private val logs = mutableListOf<LogEntry>()
    var enableDebug: Boolean = false  // 默认关闭 debug 日志
    var minLevel: LogLevel = LogLevel.ERROR  // 最小日志级别，默认只显示错误

    fun debug(message: String) {
        if (enableDebug && minLevel <= LogLevel.DEBUG) {
            logs.add(LogEntry(LogLevel.DEBUG, message))
            logger.debug { message }
        }
    }

    fun info(message: String) {
        if (minLevel <= LogLevel.INFO) {
            logs.add(LogEntry(LogLevel.INFO, message))
            logger.info { message }
        }
    }

    fun warn(message: String) {
        if (minLevel <= LogLevel.WARN) {
            logs.add(LogEntry(LogLevel.WARN, message))
            logger.warn { message }
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (minLevel <= LogLevel.ERROR) {
            logs.add(LogEntry(LogLevel.ERROR, message, throwable))
            if (throwable != null) {
                logger.error(throwable) { message }
            } else {
                logger.error { message }
            }
        }
    }

    fun getLogs(): List<LogEntry> = logs.toList()

    fun clear() {
        logs.clear()
    }
}

/**
 * 日志条目
 */
data class LogEntry(
    val level: LogLevel,
    val message: String,
    val throwable: Throwable? = null,
    val timestamp: Long = getCurrentTimeMillis()
)

/**
 * 日志级别
 */
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

/**
 * 获取当前时间戳（毫秒）
 */
@OptIn(kotlin.time.ExperimentalTime::class)
internal fun getCurrentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
