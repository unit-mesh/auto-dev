package cc.unitmesh.devins.compiler

import cc.unitmesh.devins.ast.DevInsFileNode
import cc.unitmesh.devins.ast.DevInsNode
import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.compiler.processor.*
import cc.unitmesh.devins.compiler.result.DevInsCompiledResult
import cc.unitmesh.devins.compiler.template.TemplateCompiler
import cc.unitmesh.devins.parser.DevInsParser
import cc.unitmesh.devins.parser.ParseResult
import cc.unitmesh.devins.workspace.WorkspaceManager

/**
 * DevIns 编译器
 * 将 DevIns AST 编译成可执行的模板
 * 
 * 参考 @exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/compiler/DevInsCompiler.kt
 * 但基于 mpp-core 的 AST 节点而不是 PSI 元素
 */
class DevInsCompiler(
    private val context: CompilerContext = CompilerContext()
) {
    
    // 初始化子处理器
    private val commandProcessor = CommandProcessor()
    private val variableProcessor = VariableProcessor()
    private val agentProcessor = AgentProcessor()
    
    // 初始化处理器列表
    // UsedProcessor 作为统一入口点，与 IDEA 版本保持一致
    // Parser 现在生成 DevInsUsedNode 而不是具体的命令/变量/代理节点
    private val processors: List<DevInsNodeProcessor> = listOf(
        TextSegmentProcessor(),
        CodeBlockProcessor(),
        FrontMatterProcessor(),
        UsedProcessor(commandProcessor, variableProcessor, agentProcessor),
        VelocityExpressionProcessor()
    )
    
    /**
     * 编译 DevIns 文件节点
     */
    suspend fun compile(fileNode: DevInsFileNode): DevInsCompiledResult {
        context.reset()
        context.result.input = fileNode.getText()
        
        try {
            // 遍历所有子节点并处理
            fileNode.children.forEach { node ->
                if (context.hasError()) return@forEach
                
                val processor = findProcessor(node)
                if (processor != null) {
                    val processResult = processor.process(node, context)
                    if (!processResult.success) {
                        context.logger.warn("Failed to process node: ${processResult.errorMessage}")
                    }
                    if (!processResult.shouldContinue) {
                        return@forEach
                    }
                } else {
                    // 如果没有找到合适的处理器，直接输出文本
                    context.appendOutput(node.getText())
                    context.logger.warn("No processor found for node type: ${node.nodeType}")
                }
            }
            
            // 设置最终输出
            val rawOutput = context.output.toString()
            context.result.output = rawOutput

            // 如果需要，进行模板编译
            if (context.options.enableTemplateCompilation) {
                val templateCompiler = TemplateCompiler(context.variableTable, context.fileSystem)
                context.result.output = templateCompiler.compile(rawOutput)
            }

        } catch (e: Exception) {
            context.logger.error("Compilation failed", e)
            context.result.hasError = true
            context.result.errorMessage = e.message ?: "Unknown compilation error"
        }

        return context.result
    }
    
    /**
     * 查找能够处理指定节点的处理器
     */
    private fun findProcessor(node: DevInsNode): DevInsNodeProcessor? {
        return processors.find { it.canProcess(node) }
    }
    
    /**
     * 编译 DevIns 源代码字符串
     */
    suspend fun compileFromSource(source: String): DevInsCompiledResult {
        try {
            // 解析源代码为 AST
            val parser = DevInsParser(source)
            val parseResult = parser.parse()

            return when (parseResult) {
                is ParseResult.Success -> {
                    // 解析成功，编译 AST
                    compile(parseResult.value)
                }
                is ParseResult.Failure -> {
                    // 解析失败，返回错误结果
                    val result = DevInsCompiledResult()
                    result.hasError = true
                    result.errorMessage = "Parse failed: ${parseResult.error.message}"
                    result.input = source
                    result
                }
            }
        } catch (e: Exception) {
            // 处理异常
            val result = DevInsCompiledResult()
            result.hasError = true
            result.errorMessage = "Compilation failed: ${e.message}"
            result.input = source
            return result
        }
    }

    /**
     * 创建带有自定义选项的编译器
     */
    companion object {
        /**
         * 创建默认编译器
         */
        fun create(): DevInsCompiler {
            val context = CompilerContext()
            // 从 WorkspaceManager 获取当前工作空间的文件系统
            val workspace = WorkspaceManager.getCurrentOrEmpty()
            context.fileSystem = workspace.fileSystem
            return DevInsCompiler(context)
        }

        /**
         * 创建带有调试模式的编译器
         */
        fun createWithDebug(): DevInsCompiler {
            val context = CompilerContext()
            context.options.copy(debug = true)
            return DevInsCompiler(context)
        }

        /**
         * 创建带有严格模式的编译器
         */
        fun createWithStrictMode(): DevInsCompiler {
            val context = CompilerContext()
            context.options.copy(strict = true)
            return DevInsCompiler(context)
        }
    }
}
