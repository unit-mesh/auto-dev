package cc.unitmesh.devins.compiler.processor

import cc.unitmesh.devins.ast.*
import cc.unitmesh.devins.compiler.context.CompilerContext

/**
 * 文本段处理器
 * 处理普通文本内容，直接输出到结果中
 * 参考 @exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/compiler/processor/TextSegmentProcessor.kt
 */
class TextSegmentProcessor : BaseDevInsNodeProcessor() {
    
    override suspend fun process(node: DevInsNode, context: CompilerContext): ProcessResult {
        logProcessing(node, context)
        
        val text = getNodeText(node)
        
        when (node) {
            is DevInsTextSegmentNode -> {
                context.appendOutput(text)
            }
            
            is DevInsTokenNode -> {
                // 处理特殊的 token 类型
                when (node.token.type.name) {
                    "NEWLINE" -> context.appendOutput("\n")
                    "TEXT_SEGMENT" -> context.appendOutput(text)
                    "MARKDOWN_HEADER" -> context.appendOutput("#[[${text}]]#")
                    "WHITE_SPACE" -> context.appendOutput(text)
                    else -> {
                        context.appendOutput(text)
                        context.logger.warn("[$name] Unknown token type: ${node.token.type}")
                    }
                }
            }
            
            else -> {
                // 对于其他类型的节点，直接输出文本
                context.appendOutput(text)
                context.logger.warn("[$name] Unexpected node type in TextSegmentProcessor: ${node.nodeType}")
            }
        }
        
        return ProcessResult.success(text)
    }
    
    override fun canProcess(node: DevInsNode): Boolean {
        return when (node) {
            is DevInsTextSegmentNode -> true
            is DevInsTokenNode -> {
                // 检查是否是文本相关的 token
                node.token.type.name in setOf(
                    "TEXT_SEGMENT",
                    "NEWLINE", 
                    "MARKDOWN_HEADER",
                    "WHITE_SPACE"
                )
            }
            else -> false
        }
    }
}
