package cc.unitmesh.devins.compiler.processor

import cc.unitmesh.devins.ast.*
import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.compiler.result.CustomAgentConfig

/**
 * Agent 处理器
 * 处理 DevIns 中的 Agent 节点（如 @agent）
 * 参考 @exts/devins-lang/src/main/kotlin/cc/unitmesh/devti/language/compiler/processor/UsedProcessor.kt 中的 Agent 处理部分
 */
class AgentProcessor : BaseDevInsNodeProcessor() {
    
    override suspend fun process(node: DevInsNode, context: CompilerContext): ProcessResult {
        logProcessing(node, context)
        
        when (node) {
            is DevInsAgentNode -> {
                return processAgent(node, context)
            }
            
            else -> {
                context.logger.warn("[$name] Unexpected node type: ${node.nodeType}")
                return ProcessResult.failure("Cannot process node type: ${node.nodeType}")
            }
        }
    }
    
    private suspend fun processAgent(node: DevInsAgentNode, context: CompilerContext): ProcessResult {
        val agentName = node.name
        
        context.logger.info("[$name] Processing agent: $agentName")
        
        // 更新统计信息
        context.result.statistics.agentCount++
        
        // 根据 Agent 名称进行不同的处理
        return when (agentName.lowercase()) {
            "helper", "assistant" -> processHelperAgent(agentName, context)
            "coder", "developer" -> processCoderAgent(agentName, context)
            "reviewer", "critic" -> processReviewerAgent(agentName, context)
            "tester", "qa" -> processTesterAgent(agentName, context)
            "architect", "designer" -> processArchitectAgent(agentName, context)
            "analyst", "researcher" -> processAnalystAgent(agentName, context)
            "writer", "documenter" -> processWriterAgent(agentName, context)
            else -> processCustomAgent(agentName, context)
        }
    }
    
    private suspend fun processHelperAgent(agentName: String, context: CompilerContext): ProcessResult {
        context.logger.info("[$name] Processing helper agent: $agentName")
        
        val agentConfig = CustomAgentConfig(
            name = agentName,
            type = "helper",
            parameters = mapOf(
                "role" to "general assistance",
                "capabilities" to listOf("question answering", "guidance", "support")
            )
        )
        
        context.result.executeAgent = agentConfig
        
        val output = "{{AGENT:$agentName}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "agentName" to agentName,
                "agentType" to "helper",
                "config" to agentConfig
            )
        )
    }
    
    private suspend fun processCoderAgent(agentName: String, context: CompilerContext): ProcessResult {
        context.logger.info("[$name] Processing coder agent: $agentName")
        
        val agentConfig = CustomAgentConfig(
            name = agentName,
            type = "coder",
            parameters = mapOf(
                "role" to "code generation and modification",
                "capabilities" to listOf("coding", "refactoring", "debugging", "optimization")
            )
        )
        
        context.result.executeAgent = agentConfig
        
        val output = "{{CODER_AGENT:$agentName}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "agentName" to agentName,
                "agentType" to "coder",
                "config" to agentConfig
            )
        )
    }
    
    private suspend fun processReviewerAgent(agentName: String, context: CompilerContext): ProcessResult {
        context.logger.info("[$name] Processing reviewer agent: $agentName")
        
        val agentConfig = CustomAgentConfig(
            name = agentName,
            type = "reviewer",
            parameters = mapOf(
                "role" to "code review and quality assurance",
                "capabilities" to listOf("code review", "quality check", "best practices", "security audit")
            )
        )
        
        context.result.executeAgent = agentConfig
        
        val output = "{{REVIEWER_AGENT:$agentName}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "agentName" to agentName,
                "agentType" to "reviewer",
                "config" to agentConfig
            )
        )
    }
    
    private suspend fun processTesterAgent(agentName: String, context: CompilerContext): ProcessResult {
        context.logger.info("[$name] Processing tester agent: $agentName")
        
        val agentConfig = CustomAgentConfig(
            name = agentName,
            type = "tester",
            parameters = mapOf(
                "role" to "testing and quality assurance",
                "capabilities" to listOf("unit testing", "integration testing", "test case generation", "bug finding")
            )
        )
        
        context.result.executeAgent = agentConfig
        
        val output = "{{TESTER_AGENT:$agentName}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "agentName" to agentName,
                "agentType" to "tester",
                "config" to agentConfig
            )
        )
    }
    
    private suspend fun processArchitectAgent(agentName: String, context: CompilerContext): ProcessResult {
        context.logger.info("[$name] Processing architect agent: $agentName")
        
        val agentConfig = CustomAgentConfig(
            name = agentName,
            type = "architect",
            parameters = mapOf(
                "role" to "system architecture and design",
                "capabilities" to listOf("system design", "architecture planning", "technology selection", "scalability")
            )
        )
        
        context.result.executeAgent = agentConfig
        
        val output = "{{ARCHITECT_AGENT:$agentName}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "agentName" to agentName,
                "agentType" to "architect",
                "config" to agentConfig
            )
        )
    }
    
    private suspend fun processAnalystAgent(agentName: String, context: CompilerContext): ProcessResult {
        context.logger.info("[$name] Processing analyst agent: $agentName")
        
        val agentConfig = CustomAgentConfig(
            name = agentName,
            type = "analyst",
            parameters = mapOf(
                "role" to "analysis and research",
                "capabilities" to listOf("requirement analysis", "data analysis", "research", "documentation")
            )
        )
        
        context.result.executeAgent = agentConfig
        
        val output = "{{ANALYST_AGENT:$agentName}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "agentName" to agentName,
                "agentType" to "analyst",
                "config" to agentConfig
            )
        )
    }
    
    private suspend fun processWriterAgent(agentName: String, context: CompilerContext): ProcessResult {
        context.logger.info("[$name] Processing writer agent: $agentName")
        
        val agentConfig = CustomAgentConfig(
            name = agentName,
            type = "writer",
            parameters = mapOf(
                "role" to "documentation and writing",
                "capabilities" to listOf("documentation", "technical writing", "user guides", "API docs")
            )
        )
        
        context.result.executeAgent = agentConfig
        
        val output = "{{WRITER_AGENT:$agentName}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "agentName" to agentName,
                "agentType" to "writer",
                "config" to agentConfig
            )
        )
    }
    
    private suspend fun processCustomAgent(agentName: String, context: CompilerContext): ProcessResult {
        context.logger.info("[$name] Processing custom agent: $agentName")
        
        val agentConfig = CustomAgentConfig(
            name = agentName,
            type = "custom",
            parameters = mapOf(
                "role" to "custom agent",
                "capabilities" to listOf("custom functionality")
            )
        )
        
        context.result.executeAgent = agentConfig
        
        val output = "{{CUSTOM_AGENT:$agentName}}"
        context.appendOutput(output)
        
        return ProcessResult.success(
            output = output,
            metadata = mapOf(
                "agentName" to agentName,
                "agentType" to "custom",
                "config" to agentConfig,
                "isCustom" to true
            )
        )
    }
    
    override fun canProcess(node: DevInsNode): Boolean {
        return node is DevInsAgentNode
    }
}
