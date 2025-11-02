package cc.unitmesh.agent.core

import cc.unitmesh.agent.model.AgentActivity
import cc.unitmesh.agent.model.AgentContext
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.AgentResult

/**
 * Agent 执行器接口
 * 
 * 负责执行 Agent 的主循环：
 * 1. 调用 LLM
 * 2. 处理工具调用
 * 3. 检查终止条件
 * 4. 发送活动事件
 */
interface AgentExecutor {
    /**
     * 执行 Agent
     * 
     * @param definition Agent 定义
     * @param context 执行上下文
     * @param onActivity 活动回调
     * @return 执行结果
     */
    suspend fun execute(
        definition: AgentDefinition,
        context: AgentContext,
        onActivity: (AgentActivity) -> Unit = {}
    ): AgentResult

    /**
     * 取消执行
     * 
     * @param agentId Agent ID
     */
    suspend fun cancel(agentId: String)
}

