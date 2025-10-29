package cc.unitmesh.devins.compiler.processor

import cc.unitmesh.devins.ast.*
import cc.unitmesh.devins.compiler.context.CompilerContext

/**
 * Velocity 表达式处理器
 * 处理 DevIns 中的 Velocity 模板表达式（如 #if, #foreach 等）
 * 参考 @exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/compiler/processor/VelocityExprProcessor.kt
 */
class VelocityExpressionProcessor : BaseDevInsNodeProcessor() {
    
    override suspend fun process(node: DevInsNode, context: CompilerContext): ProcessResult {
        logProcessing(node, context)
        
        when (node) {
            is DevInsConditionalExpressionNode -> {
                return processConditionalExpression(node, context)
            }

            is DevInsExpressionNode -> {
                return processExpression(node, context)
            }

            else -> {
                context.logger.warn("[$name] Unexpected node type: ${node.nodeType}")
                return ProcessResult.failure("Cannot process node type: ${node.nodeType}")
            }
        }
    }
    
    private suspend fun processExpression(
        node: DevInsExpressionNode,
        context: CompilerContext
    ): ProcessResult {
        val expressionText = getNodeText(node)
        context.logger.info("[$name] Processing expression: $expressionText")

        // 直接输出表达式，让后续的模板引擎处理
        context.appendOutput(expressionText)

        return ProcessResult.success(
            output = expressionText,
            metadata = mapOf(
                "expressionType" to "general",
                "expressionText" to expressionText
            )
        )
    }
    
    private suspend fun processConditionalExpression(
        node: DevInsConditionalExpressionNode, 
        context: CompilerContext
    ): ProcessResult {
        val condition = node.condition
        val thenBranch = node.thenBranch
        val elseBranch = node.elseBranch
        
        context.logger.info("[$name] Processing conditional expression")
        
        val output = StringBuilder()
        
        // 处理条件
        if (condition != null) {
            val conditionText = getNodeText(condition)
            output.append("#if($conditionText)")
        }
        
        // 处理 then 分支
        if (thenBranch != null) {
            val thenText = getNodeText(thenBranch)
            output.append(thenText)
        }
        
        // 处理 else 分支
        if (elseBranch != null) {
            output.append("#else")
            val elseText = getNodeText(elseBranch)
            output.append(elseText)
        }
        
        // 结束条件
        if (condition != null) {
            output.append("#end")
        }
        
        val finalOutput = output.toString()
        context.appendOutput(finalOutput)
        
        return ProcessResult.success(
            output = finalOutput,
            metadata = mapOf(
                "expressionType" to "conditional",
                "hasCondition" to (condition != null),
                "hasThenBranch" to (thenBranch != null),
                "hasElseBranch" to (elseBranch != null)
            )
        )
    }
    
    /**
     * 处理 #if 表达式
     */
    private suspend fun processIfExpression(
        condition: String,
        thenContent: String,
        elseContent: String?,
        context: CompilerContext
    ): ProcessResult {
        val output = StringBuilder()
        
        output.append("#if($condition)")
        output.append(thenContent)
        
        if (elseContent != null) {
            output.append("#else")
            output.append(elseContent)
        }
        
        output.append("#end")
        
        val finalOutput = output.toString()
        context.appendOutput(finalOutput)
        
        return ProcessResult.success(
            output = finalOutput,
            metadata = mapOf(
                "type" to "if",
                "condition" to condition,
                "hasElse" to (elseContent != null)
            )
        )
    }
    
    /**
     * 处理 #foreach 表达式
     */
    private suspend fun processForeachExpression(
        variable: String,
        collection: String,
        content: String,
        context: CompilerContext
    ): ProcessResult {
        val output = "#foreach($variable in $collection)$content#end"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "type" to "foreach",
                "variable" to variable,
                "collection" to collection
            )
        )
    }
    
    /**
     * 处理 #set 表达式
     */
    private suspend fun processSetExpression(
        variable: String,
        value: String,
        context: CompilerContext
    ): ProcessResult {
        val output = "#set($variable = $value)"
        context.appendOutput(output)
        
        // 同时更新变量表
        context.variableTable.addVariable(
            name = variable.removePrefix("$"),
            varType = cc.unitmesh.devins.compiler.variable.VariableType.UNKNOWN,
            value = value
        )
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "type" to "set",
                "variable" to variable,
                "value" to value
            )
        )
    }
    
    /**
     * 处理变量引用表达式
     */
    private suspend fun processVariableReference(
        variableName: String,
        context: CompilerContext
    ): ProcessResult {
        val output = "\${$variableName}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "type" to "variable_reference",
                "variable" to variableName
            )
        )
    }
    
    /**
     * 处理方法调用表达式
     */
    private suspend fun processMethodCall(
        objectName: String,
        method: String,
        arguments: List<String>,
        context: CompilerContext
    ): ProcessResult {
        val argsText = arguments.joinToString(", ")
        val output = "\${$objectName.$method($argsText)}"
        context.appendOutput(output)

        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "type" to "method_call",
                "object" to objectName,
                "method" to method,
                "arguments" to arguments
            )
        )
    }
    
    override fun canProcess(node: DevInsNode): Boolean {
        return node is DevInsConditionalExpressionNode || node is DevInsExpressionNode
    }
}
