package cc.unitmesh.devins.compiler.processor

import cc.unitmesh.devins.ast.*
import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.compiler.variable.VariableType
import cc.unitmesh.devins.compiler.variable.VariableScope
import kotlin.time.Clock

/**
 * 变量处理器
 * 处理 DevIns 中的变量节点（如 $variable）
 * 参考 @exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/compiler/processor/VariableProcessor.kt
 */
class VariableProcessor : BaseDevInsNodeProcessor() {
    
    override suspend fun process(node: DevInsNode, context: CompilerContext): ProcessResult {
        logProcessing(node, context)
        
        when (node) {
            is DevInsVariableNode -> {
                return processVariable(node, context)
            }
            
            else -> {
                context.logger.warn("[$name] Unexpected node type: ${node.nodeType}")
                return ProcessResult.failure("Cannot process node type: ${node.nodeType}")
            }
        }
    }
    
    private suspend fun processVariable(node: DevInsVariableNode, context: CompilerContext): ProcessResult {
        val variableName = node.name
        
        context.logger.info("[$name] Processing variable: $variableName")
        
        // 更新统计信息
        context.result.statistics.variableCount++
        
        // 检查变量是否已定义
        val variableInfo = context.variableTable.getVariable(variableName)
        
        return if (variableInfo != null) {
            // 变量已定义，使用其值
            processDefinedVariable(variableName, variableInfo, context)
        } else {
            // 变量未定义，尝试解析或创建模板占位符
            processUndefinedVariable(variableName, context)
        }
    }
    
    private suspend fun processDefinedVariable(
        variableName: String,
        variableInfo: cc.unitmesh.devins.compiler.variable.VariableInfo,
        context: CompilerContext
    ): ProcessResult {
        val value = variableInfo.value
        
        context.logger.info("[$name] Using defined variable $variableName with value: $value")
        
        val output = when (variableInfo.type) {
            VariableType.STRING -> value?.toString() ?: ""
            VariableType.BOOLEAN -> value?.toString() ?: "false"
            VariableType.NUMBER -> value?.toString() ?: "0"
            VariableType.ARRAY -> {
                if (value is List<*>) {
                    value.joinToString(", ") { it?.toString() ?: "" }
                } else {
                    value?.toString() ?: "[]"
                }
            }
            VariableType.OBJECT -> {
                if (value is Map<*, *>) {
                    value.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                } else {
                    value?.toString() ?: "{}"
                }
            }
            VariableType.FUNCTION -> "{{FUNCTION:$variableName}}"
            VariableType.UNKNOWN -> value?.toString() ?: ""
        }
        
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = buildMap {
                put("variableName", variableName)
                put("variableType", variableInfo.type)
                value?.let { put("variableValue", it) }
                put("isDefined", true)
            }
        )
    }
    
    private suspend fun processUndefinedVariable(
        variableName: String,
        context: CompilerContext
    ): ProcessResult {
        context.logger.info("[$name] Processing undefined variable: $variableName")
        
        // 尝试从内置变量中获取
        val builtinValue = getBuiltinVariable(variableName)
        
        return if (builtinValue != null) {
            // 添加到变量表
            context.variableTable.addVariable(
                name = variableName,
                varType = inferVariableType(builtinValue),
                value = builtinValue,
                scope = VariableScope.BUILTIN
            )
            
            val output = builtinValue.toString()
            context.appendOutput(output)
            
            ProcessResult.success(
                output = output,
                metadata = mapOf(
                    "variableName" to variableName,
                    "variableValue" to builtinValue,
                    "isBuiltin" to true
                )
            )
        } else {
            // 创建模板占位符
            val output = "\${$variableName}"
            context.appendOutput(output)
            
            // 添加未定义变量到变量表
            context.variableTable.addVariable(
                name = variableName,
                varType = VariableType.UNKNOWN,
                value = null,
                scope = VariableScope.USER_DEFINED
            )
            
            ProcessResult.success(
                output = output,
                metadata = mapOf(
                    "variableName" to variableName,
                    "isUndefined" to true,
                    "isPlaceholder" to true
                )
            )
        }
    }
    
    /**
     * 获取内置变量的值
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun getBuiltinVariable(name: String): Any? {
        return when (name.lowercase()) {
            "timestamp" -> Clock.System.now().toEpochMilliseconds()
            "date" -> formatEpochMillisToDate(Clock.System.now().toEpochMilliseconds())
            "time" -> Clock.System.now().toEpochMilliseconds().toString()
            "random" -> kotlin.random.Random.nextInt(1000, 9999)
            "uuid" -> generateSimpleUuid()
            
            // 上下文相关变量
            "currentfile" -> "{{CURRENT_FILE}}"
            "workspace" -> "{{WORKSPACE}}"
            "projectname" -> "{{PROJECT_NAME}}"
            "language" -> "{{LANGUAGE}}"
            "framework" -> "{{FRAMEWORK}}"
            
            // 用户相关变量
            "username" -> "{{USERNAME}}"
            "useremail" -> "{{USER_EMAIL}}"
            
            else -> null
        }
    }
    
    /**
     * 推断变量类型
     */
    private fun inferVariableType(value: Any?): VariableType {
        return when (value) {
            is String -> VariableType.STRING
            is Boolean -> VariableType.BOOLEAN
            is Number -> VariableType.NUMBER
            is List<*> -> VariableType.ARRAY
            is Map<*, *> -> VariableType.OBJECT
            else -> VariableType.UNKNOWN
        }
    }
    
    /**
     * 生成简单的 UUID
     */
    private fun generateSimpleUuid(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
    
    /**
     * 将时间戳（毫秒）格式化为日期字符串
     * 使用简单的 ISO 8601 格式: YYYY-MM-DD
     */
    private fun formatEpochMillisToDate(epochMillis: Long): String {
        // 转换为秒
        val epochSeconds = epochMillis / 1000
        
        // 计算天数（从 1970-01-01 开始）
        val daysSinceEpoch = epochSeconds / 86400
        
        // 计算年份（粗略算法，足够用于简单日期显示）
        var year = 1970
        var remainingDays = daysSinceEpoch
        
        while (true) {
            val daysInYear = if (isLeapYear(year)) 366 else 365
            if (remainingDays < daysInYear) break
            remainingDays -= daysInYear
            year++
        }
        
        // 计算月份和日期
        val daysInMonths = if (isLeapYear(year)) {
            listOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        } else {
            listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        }
        
        var month = 1
        var day = remainingDays.toInt() + 1
        
        for (daysInMonth in daysInMonths) {
            if (day <= daysInMonth) break
            day -= daysInMonth
            month++
        }
        
        // 格式化为 YYYY-MM-DD（跨平台实现）
        return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }
    
    /**
     * 判断是否为闰年
     */
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
    
    override fun canProcess(node: DevInsNode): Boolean {
        return node is DevInsVariableNode
    }
}
