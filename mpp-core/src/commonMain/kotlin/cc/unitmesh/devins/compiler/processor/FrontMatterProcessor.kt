package cc.unitmesh.devins.compiler.processor

import cc.unitmesh.devins.ast.*
import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.compiler.result.FrontMatterConfig

/**
 * 前置元数据处理器
 * 处理 DevIns 文件开头的前置元数据配置
 * 参考 @exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/compiler/processor/FrontMatterProcessor.kt
 */
class FrontMatterProcessor : BaseDevInsNodeProcessor() {
    
    override suspend fun process(node: DevInsNode, context: CompilerContext): ProcessResult {
        logProcessing(node, context)
        
        when (node) {
            is DevInsFrontMatterNode -> {
                return processFrontMatter(node, context)
            }
            
            is DevInsFrontMatterEntryNode -> {
                return processFrontMatterEntry(node, context)
            }
            
            else -> {
                context.logger.warn("[$name] Unexpected node type: ${node.nodeType}")
                return ProcessResult.failure("Cannot process node type: ${node.nodeType}")
            }
        }
    }
    
    private suspend fun processFrontMatter(node: DevInsFrontMatterNode, context: CompilerContext): ProcessResult {
        context.logger.info("[$name] Processing front matter with ${node.entries.size} entries")
        
        val config = FrontMatterConfig()
        val variables = mutableMapOf<String, Any>()
        val lifecycle = mutableMapOf<String, String>()
        val functions = mutableListOf<String>()
        val agents = mutableListOf<String>()
        
        var name: String? = null
        var description: String? = null
        
        // 处理所有前置元数据条目
        for (entry in node.entries) {
            val result = processFrontMatterEntry(entry, context)
            if (!result.success) {
                context.logger.warn("[$name] Failed to process front matter entry: ${result.errorMessage}")
                continue
            }
            
            // 从处理结果中提取配置信息
            val metadata = result.metadata
            val key = metadata["key"] as? String ?: continue
            val value = metadata["value"]
            
            when (key.lowercase()) {
                "name" -> name = value as? String
                "description" -> description = value as? String
                "variables" -> {
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val valueMap = value as Map<String, Any?>
                        valueMap.forEach { (k, v) ->
                            if (v != null) {
                                variables[k] = v
                            }
                        }
                    }
                }
                "when", "on_streaming", "before_streaming", "on_streaming_end", "after_streaming" -> {
                    lifecycle[key] = value?.toString() ?: ""
                }
                "functions" -> {
                    when (value) {
                        is List<*> -> functions.addAll(value.mapNotNull { it?.toString() })
                        is String -> functions.add(value)
                    }
                }
                "agents" -> {
                    when (value) {
                        is List<*> -> agents.addAll(value.mapNotNull { it?.toString() })
                        is String -> agents.add(value)
                    }
                }
                else -> {
                    // 其他配置项作为变量处理
                    if (value != null) {
                        variables[key] = value
                    }
                }
            }
        }
        
        // 创建配置对象
        val finalConfig = config.copy(
            name = name,
            description = description,
            variables = variables,
            lifecycle = lifecycle,
            functions = functions,
            agents = agents
        )
        
        // 设置到编译结果中
        context.result.config = finalConfig
        
        // 将变量添加到变量表中
        variables.forEach { (key, value) ->
            context.variableTable.addVariable(
                name = key,
                varType = inferVariableType(value),
                value = value
            )
        }
        
        return ProcessResult.success(
            metadata = mapOf(
                "config" to finalConfig,
                "variableCount" to variables.size
            )
        )
    }
    
    private suspend fun processFrontMatterEntry(
        entry: DevInsFrontMatterEntryNode, 
        context: CompilerContext
    ): ProcessResult {
        val key = entry.key?.getText()?.trim() ?: return ProcessResult.failure("Missing key in front matter entry")
        val value = entry.value?.let { parseValue(it) }
        
        context.logger.info("[$name] Processing front matter entry: $key = $value")
        
        return ProcessResult.success(
            metadata = buildMap {
                put("key", key)
                value?.let { put("value", it) }
            }
        )
    }
    
    /**
     * 解析前置元数据值
     */
    private fun parseValue(valueNode: DevInsNode): Any? {
        val text = getNodeText(valueNode).trim()
        
        return when {
            // 布尔值
            text.equals("true", ignoreCase = true) -> true
            text.equals("false", ignoreCase = true) -> false
            
            // 数字
            text.toIntOrNull() != null -> text.toInt()
            text.toDoubleOrNull() != null -> text.toDouble()
            
            // 字符串（移除引号）
            text.startsWith("\"") && text.endsWith("\"") -> text.substring(1, text.length - 1)
            text.startsWith("'") && text.endsWith("'") -> text.substring(1, text.length - 1)
            
            // 数组（简单解析）
            text.startsWith("[") && text.endsWith("]") -> {
                text.substring(1, text.length - 1)
                    .split(",")
                    .map { it.trim().removeSurrounding("\"", "'") }
            }
            
            // 默认作为字符串
            else -> text
        }
    }
    
    /**
     * 推断变量类型
     */
    private fun inferVariableType(value: Any?): cc.unitmesh.devins.compiler.variable.VariableType {
        return when (value) {
            is String -> cc.unitmesh.devins.compiler.variable.VariableType.STRING
            is Boolean -> cc.unitmesh.devins.compiler.variable.VariableType.BOOLEAN
            is Number -> cc.unitmesh.devins.compiler.variable.VariableType.NUMBER
            is List<*> -> cc.unitmesh.devins.compiler.variable.VariableType.ARRAY
            is Map<*, *> -> cc.unitmesh.devins.compiler.variable.VariableType.OBJECT
            else -> cc.unitmesh.devins.compiler.variable.VariableType.UNKNOWN
        }
    }
    
    override fun canProcess(node: DevInsNode): Boolean {
        return node is DevInsFrontMatterNode || node is DevInsFrontMatterEntryNode
    }
}
