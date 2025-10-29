package cc.unitmesh.devins.compiler.context

import cc.unitmesh.devins.compiler.result.DevInsCompiledResult
import cc.unitmesh.devins.compiler.variable.VariableTable
import cc.unitmesh.devins.compiler.variable.VariableScope
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
 * 简单的日志记录器
 */
class CompilerLogger {
    
    private val logs = mutableListOf<LogEntry>()
    
    fun info(message: String) {
        logs.add(LogEntry(LogLevel.INFO, message))
        println("[INFO] $message")
    }
    
    fun warn(message: String) {
        logs.add(LogEntry(LogLevel.WARN, message))
        println("[WARN] $message")
    }
    
    fun error(message: String, throwable: Throwable? = null) {
        logs.add(LogEntry(LogLevel.ERROR, message, throwable))
        println("[ERROR] $message")
        throwable?.printStackTrace()
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
    INFO, WARN, ERROR
}

/**
 * 获取当前时间戳（毫秒）
 */
@OptIn(kotlin.time.ExperimentalTime::class)
internal fun getCurrentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
