package cc.unitmesh.agent.core

import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.tool.ToolResult

/**
 * SubAgent - 用于处理子任务的 Agent
 *
 * SubAgent 继承自 Agent，专门用于处理特定的子任务，例如：
 * - ErrorRecoveryAgent: 分析和修复错误
 * - LogSummaryAgent: 总结长日志输出
 * - ContentHandlerAgent: 处理长内容并支持对话
 * - CodeReviewAgent: 代码审查
 * - TestGenerationAgent: 测试生成
 *
 * SubAgent 的特点：
 * 1. 聚焦于单一职责
 * 2. 可被 MainAgent 当作 Tool 调用
 * 3. 拥有独立的 LLM 上下文
 * 4. 输入输出结构化
 * 5. 本身就是一个 Tool，可以被任何 Agent 使用
 * 6. 可以持有 Tool 执行结果的实例状态
 * 7. 支持与其他 Agent 的对话交互
 *
 * 参考 Gemini CLI 的 SubagentToolWrapper 设计和 A2A 协议的 Agent Card 概念
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

    /**
     * 处理来自其他 Agent 的问题
     * 这是新增的对话能力，允许其他 Agent 向此 SubAgent 提问
     *
     * @param question 问题内容
     * @param context 问题上下文
     * @return 回答结果
     */
    open suspend fun handleQuestion(
        question: String,
        context: Map<String, Any> = emptyMap()
    ): ToolResult.AgentResult {
        return ToolResult.AgentResult(
            success = false,
            content = "This SubAgent does not support question handling",
            metadata = mapOf("subagent" to name)
        )
    }

    /**
     * 获取当前 SubAgent 持有的状态摘要
     * 用于其他 Agent 了解此 SubAgent 的当前状态
     *
     * @return 状态摘要
     */
    open fun getStateSummary(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "description" to description,
            "priority" to priority
        )
    }
}

