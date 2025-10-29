package cc.unitmesh.devins.compiler.processor

import cc.unitmesh.devins.ast.*
import cc.unitmesh.devins.compiler.context.CompilerContext

/**
 * 代码块处理器
 * 处理代码块节点，支持不同语言的代码块
 * 参考 @exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/compiler/processor/CodeProcessor.kt
 */
class CodeBlockProcessor : BaseDevInsNodeProcessor() {
    
    override suspend fun process(node: DevInsNode, context: CompilerContext): ProcessResult {
        logProcessing(node, context)
        
        // 检查是否需要跳过此代码块
        if (context.skipNextCode) {
            context.skipNextCode = false
            context.logger.info("[$name] Skipping code block as requested")
            return ProcessResult.success()
        }
        
        when (node) {
            is DevInsCodeBlockNode -> {
                return processCodeBlock(node, context)
            }
            
            else -> {
                context.logger.warn("[$name] Unexpected node type: ${node.nodeType}")
                return ProcessResult.failure("Cannot process node type: ${node.nodeType}")
            }
        }
    }
    
    private suspend fun processCodeBlock(node: DevInsCodeBlockNode, context: CompilerContext): ProcessResult {
        val codeText = getNodeText(node)
        
        // 更新统计信息
        context.result.statistics.codeBlockCount++
        
        // 检查是否有语言标识
        val language = extractLanguage(node)
        if (language != null) {
            context.logger.info("[$name] Processing code block with language: $language")
        }
        
        // 直接输出代码块内容
        context.appendOutput(codeText)
        
        return ProcessResult.success(
            output = codeText,
            metadata = mapOf(
                "language" to (language ?: "unknown"),
                "codeLength" to codeText.length
            )
        )
    }
    
    /**
     * 从代码块节点中提取语言信息
     */
    private fun extractLanguage(node: DevInsCodeBlockNode): String? {
        // 尝试从子节点中找到语言标识
        for (child in node.children) {
            if (child is DevInsTokenNode) {
                val tokenText = child.token.text.trim()
                // 检查是否是语言标识（通常在代码块开始标记后）
                if (tokenText.matches(Regex("[a-zA-Z][a-zA-Z0-9]*"))) {
                    return tokenText
                }
            }
        }
        
        // 如果没有找到明确的语言标识，尝试从代码内容推断
        val codeContent = getCodeContent(node)
        return inferLanguageFromContent(codeContent)
    }
    
    /**
     * 获取代码块的实际代码内容（排除标记符号）
     */
    private fun getCodeContent(node: DevInsCodeBlockNode): String {
        val fullText = getNodeText(node)
        
        // 移除代码块标记（```）
        return fullText
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            .let { content ->
                // 如果第一行是语言标识，移除它
                val lines = content.split('\n')
                if (lines.isNotEmpty() && lines[0].matches(Regex("[a-zA-Z][a-zA-Z0-9]*"))) {
                    lines.drop(1).joinToString("\n")
                } else {
                    content
                }
            }
    }
    
    /**
     * 从代码内容推断语言类型
     */
    private fun inferLanguageFromContent(content: String): String? {
        return when {
            content.contains("fun ") || content.contains("class ") && content.contains("kotlin") -> "kotlin"
            content.contains("function ") || content.contains("const ") || content.contains("let ") -> "javascript"
            content.contains("def ") || content.contains("import ") && content.contains("python") -> "python"
            content.contains("public class") || content.contains("import java") -> "java"
            content.contains("#include") || content.contains("int main") -> "c"
            content.contains("fn ") || content.contains("use ") && content.contains("rust") -> "rust"
            else -> null
        }
    }
    
    override fun canProcess(node: DevInsNode): Boolean {
        return node is DevInsCodeBlockNode
    }
}
