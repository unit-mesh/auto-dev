package cc.unitmesh.devins.parser

import cc.unitmesh.devins.ast.DevInsNode
import cc.unitmesh.devins.token.TokenPosition

/**
 * 解析结果
 */
sealed class ParseResult<out T> {
    /**
     * 解析成功
     */
    data class Success<T>(val value: T) : ParseResult<T>()
    
    /**
     * 解析失败
     */
    data class Failure(val error: ParseError) : ParseResult<Nothing>()
    
    /**
     * 检查是否成功
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * 检查是否失败
     */
    val isFailure: Boolean get() = this is Failure
    
    /**
     * 获取成功结果，如果失败则返回 null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }
    
    /**
     * 获取成功结果，如果失败则抛出异常
     */
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw ParseException(error)
    }
    
    /**
     * 映射成功结果
     */
    inline fun <R> map(transform: (T) -> R): ParseResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
    
    /**
     * 平铺映射
     */
    inline fun <R> flatMap(transform: (T) -> ParseResult<R>): ParseResult<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }
    
    companion object {
        /**
         * 创建成功结果
         */
        fun <T> success(value: T): ParseResult<T> = Success(value)
        
        /**
         * 创建失败结果
         */
        fun failure(error: ParseError): ParseResult<Nothing> = Failure(error)
        
        /**
         * 创建失败结果
         */
        fun failure(message: String, position: TokenPosition): ParseResult<Nothing> {
            return Failure(ParseError(message, position))
        }
    }
}

/**
 * 解析错误
 */
data class ParseError(
    val message: String,
    val position: TokenPosition,
    val cause: Throwable? = null
) {
    override fun toString(): String {
        return "ParseError: $message at $position${cause?.let { " (caused by: $it)" } ?: ""}"
    }
}

/**
 * 解析异常
 */
class ParseException(val error: ParseError) : Exception(error.toString(), error.cause)

/**
 * 解析上下文
 * 包含解析过程中的状态信息
 */
data class ParseContext(
    /**
     * 当前解析位置
     */
    var position: Int = 0,
    
    /**
     * 是否在恢复模式（错误恢复）
     */
    var isRecovering: Boolean = false,
    
    /**
     * 解析错误列表
     */
    val errors: MutableList<ParseError> = mutableListOf(),
    
    /**
     * 解析选项
     */
    val options: ParseOptions = ParseOptions()
) {
    /**
     * 添加错误
     */
    fun addError(error: ParseError) {
        errors.add(error)
    }
    
    /**
     * 添加错误
     */
    fun addError(message: String, position: TokenPosition) {
        addError(ParseError(message, position))
    }
    
    /**
     * 检查是否有错误
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()
    
    /**
     * 获取第一个错误
     */
    fun getFirstError(): ParseError? = errors.firstOrNull()
    
    /**
     * 清除错误
     */
    fun clearErrors() {
        errors.clear()
    }
}

/**
 * 解析选项
 */
data class ParseOptions(
    /**
     * 是否启用错误恢复
     */
    val enableErrorRecovery: Boolean = true,
    
    /**
     * 最大错误数量
     */
    val maxErrors: Int = 10,
    
    /**
     * 是否保留注释
     */
    val preserveComments: Boolean = true,
    
    /**
     * 是否保留空白字符
     */
    val preserveWhitespace: Boolean = false,
    
    /**
     * 是否严格模式
     */
    val strictMode: Boolean = false
)

/**
 * 解析器状态
 */
data class ParserState(
    /**
     * 当前 Token 索引
     */
    var tokenIndex: Int = 0,
    
    /**
     * 解析深度（用于防止无限递归）
     */
    var depth: Int = 0,
    
    /**
     * 最大解析深度
     */
    val maxDepth: Int = 1000
) {
    /**
     * 检查是否达到最大深度
     */
    fun isMaxDepthReached(): Boolean = depth >= maxDepth
    
    /**
     * 增加深度
     */
    fun enterDepth() {
        depth++
    }
    
    /**
     * 减少深度
     */
    fun exitDepth() {
        if (depth > 0) {
            depth--
        }
    }
    
    /**
     * 保存状态
     */
    fun save(): ParserState = copy()
    
    /**
     * 恢复状态
     */
    fun restore(state: ParserState) {
        tokenIndex = state.tokenIndex
        depth = state.depth
    }
}
