package cc.unitmesh.agent.core

import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.tool.ToolResult

/**
 * SubAgent - 用于处理子任务的 Agent
 * 
 * SubAgent 继承自 Agent，专门用于处理特定的子任务，例如：
 * - ErrorRecoveryAgent: 分析和修复错误
 * - LogSummaryAgent: 总结长日志输出
 * - CodeReviewAgent: 代码审查
 * - TestGenerationAgent: 测试生成
 * 
 * SubAgent 的特点：
 * 1. 聚焦于单一职责
 * 2. 可被 MainAgent 当作 Tool 调用
 * 3. 拥有独立的 LLM 上下文
 * 4. 输入输出结构化
 * 5. 本身就是一个 Tool，可以被任何 Agent 使用
 * 
 * 参考 Gemini CLI 的 SubagentToolWrapper 设计
 */
abstract class SubAgent<TInput : Any, TOutput : ToolResult>(
    definition: AgentDefinition
) : Agent<TInput, TOutput>(definition) {
    
    /**
     * SubAgent 的优先级
     * 用于在多个 SubAgent 同时触发时决定执行顺序
     * 数值越小，优先级越高
     */
    open val priority: Int = 100

    /**
     * 检查是否应该触发此 SubAgent
     * 
     * @param context 当前上下文
     * @return 是否应该执行
     */
    open fun shouldTrigger(context: Map<String, Any>): Boolean = true
}

