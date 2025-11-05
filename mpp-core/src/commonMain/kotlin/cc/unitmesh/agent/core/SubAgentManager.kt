package cc.unitmesh.agent.core

import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.subagent.AnalysisAgent
import cc.unitmesh.agent.subagent.ContentHandlerContext

/**
 * SubAgent 管理器
 * 
 * 负责管理 SubAgent 实例的生命周期，包括：
 * 1. SubAgent 实例的创建和销毁
 * 2. 长内容的自动检测和委托
 * 3. SubAgent 间的通信协调
 * 4. 状态持久化和恢复
 * 
 * 这个管理器实现了多Agent体系中的核心协调逻辑
 */
class SubAgentManager {
    
    // 注册的 SubAgent 实例
    private val subAgents = mutableMapOf<String, SubAgent<*, *>>()
    
    // 内容处理阈值
    private val contentThreshold = 5000
    
    /**
     * 注册 SubAgent
     */
    fun <TInput : Any, TOutput : ToolResult> registerSubAgent(
        subAgent: SubAgent<TInput, TOutput>
    ) {
        subAgents[subAgent.name] = subAgent
        println("🤖 Registered SubAgent: ${subAgent.name}")
    }
    
    /**
     * 注销 SubAgent
     */
    fun unregisterSubAgent(name: String) {
        subAgents.remove(name)
        println("🗑️ Unregistered SubAgent: $name")
    }
    
    /**
     * 获取 SubAgent
     */
    @Suppress("UNCHECKED_CAST")
    fun <TInput : Any, TOutput : ToolResult> getSubAgent(
        name: String
    ): SubAgent<TInput, TOutput>? {
        return subAgents[name] as? SubAgent<TInput, TOutput>
    }
    
    /**
     * 获取所有 SubAgent
     */
    fun getAllSubAgents(): Map<String, SubAgent<*, *>> {
        return subAgents.toMap()
    }
    
    /**
     * 检查内容是否需要特殊处理
     * 如果内容过长，自动委托给 ContentHandlerAgent
     */
    suspend fun checkAndHandleLongContent(
        content: String,
        contentType: String = "text",
        source: String = "unknown",
        metadata: Map<String, String> = emptyMap()
    ): ToolResult.AgentResult? {
        
        if (content.length <= contentThreshold) {
            return null // 不需要特殊处理
        }
        
        println("📊 Detected long content (${content.length} chars), delegating to AnalysisAgent")

        val analysisAgent = getSubAgent<ContentHandlerContext, ToolResult.AgentResult>("analysis-agent")
        if (analysisAgent == null) {
            println("⚠️ AnalysisAgent not registered, skipping long content handling")
            return null
        }
        
        val context = ContentHandlerContext(
            content = content,
            contentType = contentType,
            source = source,
            metadata = metadata
        )
        
        return try {
            analysisAgent.execute(context) { progress ->
                println("📊 AnalysisAgent: $progress")
            }
        } catch (e: Exception) {
            println("❌ AnalysisAgent failed: ${e.message}")
            ToolResult.AgentResult(
                success = false,
                content = "Content analysis failed: ${e.message}",
                metadata = mapOf("error" to e.message.orEmpty())
            )
        }
    }
    
    /**
     * 向指定的 SubAgent 提问
     */
    suspend fun askSubAgent(
        subAgentName: String,
        question: String,
        context: Map<String, Any> = emptyMap()
    ): ToolResult.AgentResult {
        
        val subAgent = subAgents[subAgentName]
        if (subAgent == null) {
            return ToolResult.AgentResult(
                success = false,
                content = "SubAgent '$subAgentName' not found",
                metadata = mapOf("availableAgents" to subAgents.keys.joinToString(","))
            )
        }
        
        return try {
            subAgent.handleQuestion(question, context)
        } catch (e: Exception) {
            ToolResult.AgentResult(
                success = false,
                content = "Failed to ask SubAgent '$subAgentName': ${e.message}",
                metadata = mapOf("error" to e.message.orEmpty())
            )
        }
    }
    
    /**
     * 获取所有 SubAgent 的状态摘要
     */
    fun getSystemStatus(): Map<String, Any> {
        return mapOf(
            "registeredAgents" to subAgents.size,
            "agentNames" to subAgents.keys.toList(),
            "agentStates" to subAgents.mapValues { (_, agent) -> agent.getStateSummary() },
            "contentThreshold" to contentThreshold
        )
    }
    
    /**
     * 清理所有 SubAgent 的历史数据
     */
    fun cleanup() {
        subAgents.values.forEach { agent ->
            if (agent is AnalysisAgent) {
                agent.cleanupHistory()
            }
        }
        println("🧹 Cleaned up SubAgent histories")
    }
}
