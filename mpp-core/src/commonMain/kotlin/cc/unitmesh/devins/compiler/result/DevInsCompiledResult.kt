package cc.unitmesh.devins.compiler.result

import cc.unitmesh.devins.compiler.variable.VariableTable

/**
 * DevIns 编译结果
 * 参考 @exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/compiler/DevInsCompiledResult.kt
 */
data class DevInsCompiledResult(
    /**
     * 原始 DevIns 内容
     */
    var input: String = "",
    
    /**
     * 编译后的输出字符串
     */
    var output: String = "",
    
    /**
     * 是否为本地命令
     */
    var isLocalCommand: Boolean = false,
    
    /**
     * 是否有错误
     */
    var hasError: Boolean = false,
    
    /**
     * 错误消息
     */
    var errorMessage: String? = null,
    
    /**
     * 执行代理配置
     */
    var executeAgent: CustomAgentConfig? = null,
    
    /**
     * 下一个要执行的作业
     */
    var nextJob: DevInsCompiledResult? = null,
    
    /**
     * 前置元数据配置
     */
    var config: FrontMatterConfig? = null,
    
    /**
     * 变量表
     */
    var variableTable: VariableTable = VariableTable(),
    
    /**
     * 编译统计信息
     */
    var statistics: CompilationStatistics = CompilationStatistics()
) {
    
    /**
     * 重置编译结果
     */
    fun reset() {
        input = ""
        output = ""
        isLocalCommand = false
        hasError = false
        errorMessage = null
        executeAgent = null
        nextJob = null
        config = null
        variableTable.clear()
        statistics.reset()
    }
    
    /**
     * 检查编译是否成功
     */
    fun isSuccess(): Boolean = !hasError
    
    /**
     * 获取错误信息
     */
    fun getError(): String {
        return errorMessage ?: "Unknown error"
    }
}

/**
 * 自定义代理配置
 */
data class CustomAgentConfig(
    val name: String,
    val type: String,
    val parameters: Map<String, Any> = emptyMap()
)

/**
 * 前置元数据配置
 */
data class FrontMatterConfig(
    val name: String? = null,
    val description: String? = null,
    val variables: Map<String, Any> = emptyMap(),
    val lifecycle: Map<String, String> = emptyMap(),
    val functions: List<String> = emptyList(),
    val agents: List<String> = emptyList()
)

/**
 * 编译统计信息
 */
data class CompilationStatistics(
    var startTime: Long = 0,
    var endTime: Long = 0,
    var nodeCount: Int = 0,
    var variableCount: Int = 0,
    var commandCount: Int = 0,
    var agentCount: Int = 0,
    var codeBlockCount: Int = 0
) {
    
    /**
     * 获取编译耗时（毫秒）
     */
    fun getDuration(): Long = endTime - startTime
    
    /**
     * 重置统计信息
     */
    fun reset() {
        startTime = 0
        endTime = 0
        nodeCount = 0
        variableCount = 0
        commandCount = 0
        agentCount = 0
        codeBlockCount = 0
    }
    
    /**
     * 开始计时
     */
    fun start() {
        startTime = System.currentTimeMillis()
    }
    
    /**
     * 结束计时
     */
    fun end() {
        endTime = System.currentTimeMillis()
    }
}
