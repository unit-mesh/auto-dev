package cc.unitmesh.devins.compiler.processor

import cc.unitmesh.devins.ast.DevInsNode
import cc.unitmesh.devins.compiler.context.CompilerContext

/**
 * DevIns 节点处理器接口
 * 参考 @exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/compiler/processor/DevInElementProcessor.kt
 * 但处理的是 AST 节点而不是 PSI 元素
 */
interface DevInsNodeProcessor {
    
    /**
     * 处理 AST 节点
     * @param node 要处理的 AST 节点
     * @param context 编译上下文
     * @return 处理结果
     */
    suspend fun process(node: DevInsNode, context: CompilerContext): ProcessResult
    
    /**
     * 检查此处理器是否能处理给定的节点
     * @param node 要检查的 AST 节点
     * @return 如果此处理器能处理该节点则返回 true
     */
    fun canProcess(node: DevInsNode): Boolean
    
    /**
     * 处理器名称
     */
    val name: String
        get() = this::class.simpleName ?: "UnknownProcessor"
}

/**
 * 处理结果
 * 表示节点处理的结果状态
 */
data class ProcessResult(
    /**
     * 处理是否成功
     */
    val success: Boolean = true,
    
    /**
     * 错误消息（如果处理失败）
     */
    val errorMessage: String? = null,
    
    /**
     * 是否应该继续处理后续节点
     */
    val shouldContinue: Boolean = true,
    
    /**
     * 处理后的输出内容（可选）
     */
    val output: String? = null,
    
    /**
     * 额外的元数据
     */
    val metadata: Map<String, Any> = emptyMap()
) {
    
    companion object {
        /**
         * 创建成功的处理结果
         */
        fun success(output: String? = null, metadata: Map<String, Any> = emptyMap()): ProcessResult {
            return ProcessResult(
                success = true,
                output = output,
                metadata = metadata
            )
        }
        
        /**
         * 创建失败的处理结果
         */
        fun failure(
            errorMessage: String,
            shouldContinue: Boolean = true,
            metadata: Map<String, Any> = emptyMap()
        ): ProcessResult {
            return ProcessResult(
                success = false,
                errorMessage = errorMessage,
                shouldContinue = shouldContinue,
                metadata = metadata
            )
        }
        
        /**
         * 创建停止处理的结果
         */
        fun stop(output: String? = null, metadata: Map<String, Any> = emptyMap()): ProcessResult {
            return ProcessResult(
                success = true,
                shouldContinue = false,
                output = output,
                metadata = metadata
            )
        }
    }
}

/**
 * 抽象基础处理器
 * 提供一些通用的处理逻辑
 */
abstract class BaseDevInsNodeProcessor : DevInsNodeProcessor {
    
    /**
     * 处理节点的子节点
     */
    protected suspend fun processChildren(
        node: DevInsNode,
        context: CompilerContext,
        filter: (DevInsNode) -> Boolean = { true }
    ): ProcessResult {
        for (child in node.children.filter(filter)) {
            if (context.hasError()) {
                return ProcessResult.failure("Context has error, stopping child processing")
            }
            
            // 这里可以递归调用编译器来处理子节点
            // 或者直接输出子节点的文本
            context.appendOutput(child.getText())
        }
        
        return ProcessResult.success()
    }
    
    /**
     * 安全地获取节点文本
     */
    protected fun getNodeText(node: DevInsNode): String {
        return try {
            node.getText()
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 记录处理信息
     * 默认使用 debug 级别，避免污染用户输出
     */
    protected fun logProcessing(node: DevInsNode, context: CompilerContext, message: String? = null) {
        val msg = message ?: "Processing ${node.nodeType}"
        context.logger.debug("[$name] $msg")
    }
}
