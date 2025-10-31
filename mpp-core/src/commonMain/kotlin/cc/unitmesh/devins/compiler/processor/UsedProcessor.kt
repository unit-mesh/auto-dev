package cc.unitmesh.devins.compiler.processor

import cc.unitmesh.devins.ast.*
import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.token.DevInsTokenType

/**
 * Used 节点处理器
 * 统一处理 DevInsUsedNode（@agent, /command, $variable），并分发到对应的子处理器
 * 
 * 参考 @exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/compiler/processor/UsedProcessor.kt
 * 作为命令、代理和变量处理的统一入口点
 */
class UsedProcessor(
    private val commandProcessor: CommandProcessor,
    private val variableProcessor: VariableProcessor,
    private val agentProcessor: AgentProcessor
) : BaseDevInsNodeProcessor() {
    
    override suspend fun process(node: DevInsNode, context: CompilerContext): ProcessResult {
        logProcessing(node, context)
        
        when (node) {
            is DevInsUsedNode -> {
                return processUsed(node, context)
            }
            
            else -> {
                context.logger.warn("[$name] Unexpected node type: ${node.nodeType}")
                return ProcessResult.failure("Cannot process node type: ${node.nodeType}")
            }
        }
    }
    
    /**
     * 处理 Used 节点，根据类型分发到对应的子处理器
     */
    private suspend fun processUsed(node: DevInsUsedNode, context: CompilerContext): ProcessResult {
        val usedText = node.getText()
        
        context.logger.info("[$name] Processing used node: type=${node.type}, text='$usedText'")
        
        return when (node.type) {
            DevInsUsedNode.UsedType.COMMAND -> {
                processCommand(node, context)
            }
            
            DevInsUsedNode.UsedType.AGENT -> {
                processAgent(node, context)
            }
            
            DevInsUsedNode.UsedType.VARIABLE -> {
                processVariable(node, context)
            }
        }
    }
    
    /**
     * 处理命令节点
     */
    private suspend fun processCommand(node: DevInsUsedNode, context: CompilerContext): ProcessResult {
        // 提取命令名称和参数
        val identifier = node.identifier
        val commandName = when (identifier) {
            is DevInsIdentifierNode -> identifier.name
            is DevInsTokenNode -> identifier.token.text
            else -> identifier.getText()
        }
        
        context.logger.info("[$name] Routing command to CommandProcessor: $commandName")
        
        // 创建 DevInsCommandNode 并委托给 CommandProcessor
        // 从 Used 节点的 children 中提取参数
        // 只提取 COMMAND_PROP 类型的 token（即冒号后面的内容）
        val arguments = node.children.filter { child ->
            if (child is DevInsTokenNode) {
                // 只保留 COMMAND_PROP 类型的 token（命令参数）
                child.token.type == DevInsTokenType.COMMAND_PROP
            } else {
                // 保留非 token 节点（如标识符节点等）
                child !is DevInsTokenNode
            }
        }
        
        val commandNode = DevInsCommandNode(
            name = commandName,
            arguments = arguments,
            children = node.children
        )
        
        // 委托给 CommandProcessor
        return try {
            commandProcessor.process(commandNode, context)
        } catch (e: Exception) {
            context.logger.error("[$name] Failed to process command: $commandName", e)
            ProcessResult.failure("Command processing failed: ${e.message}")
        }
    }
    
    /**
     * 处理代理节点
     */
    private suspend fun processAgent(node: DevInsUsedNode, context: CompilerContext): ProcessResult {
        // 提取代理名称
        val identifier = node.identifier
        val agentName = when (identifier) {
            is DevInsIdentifierNode -> identifier.name
            is DevInsTokenNode -> identifier.token.text
            else -> identifier.getText()
        }
        
        context.logger.info("[$name] Routing agent to AgentProcessor: $agentName")
        
        // 创建 DevInsAgentNode 并委托给 AgentProcessor
        val agentNode = DevInsAgentNode(
            name = agentName,
            children = node.children
        )
        
        // 委托给 AgentProcessor
        return try {
            agentProcessor.process(agentNode, context)
        } catch (e: Exception) {
            context.logger.error("[$name] Failed to process agent: $agentName", e)
            ProcessResult.failure("Agent processing failed: ${e.message}")
        }
    }
    
    /**
     * 处理变量节点
     */
    private suspend fun processVariable(node: DevInsUsedNode, context: CompilerContext): ProcessResult {
        // 提取变量名称
        val identifier = node.identifier
        val variableName = when (identifier) {
            is DevInsIdentifierNode -> identifier.name
            is DevInsTokenNode -> identifier.token.text
            else -> identifier.getText()
        }
        
        context.logger.info("[$name] Routing variable to VariableProcessor: $variableName")
        
        // 创建 DevInsVariableNode 并委托给 VariableProcessor
        val variableNode = DevInsVariableNode(
            name = variableName,
            children = node.children
        )
        
        // 委托给 VariableProcessor
        return try {
            variableProcessor.process(variableNode, context)
        } catch (e: Exception) {
            context.logger.error("[$name] Failed to process variable: $variableName", e)
            ProcessResult.failure("Variable processing failed: ${e.message}")
        }
    }
    
    override fun canProcess(node: DevInsNode): Boolean {
        return node is DevInsUsedNode
    }
}

