package cc.unitmesh.agent.core

import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.ToolResult

/**
 * MainAgent - 主任务 Agent 基类
 * 
 * MainAgent 是用于处理主要任务的 Agent，例如：
 * - CodingAgent: 自动化编码任务
 * - ArchitectAgent: 架构设计任务
 * - RefactoringAgent: 代码重构任务
 * 
 * MainAgent 的特点：
 * 1. 可以协调多个 SubAgents（作为 Tools 使用）
 * 2. 拥有主循环迭代逻辑
 * 3. 管理任务的整体生命周期
 * 4. 处理复杂的决策流程
 * 5. 可以将任何 Tool（包括 SubAgent）注册到自己的工具集
 */
abstract class MainAgent<TInput : Any, TOutput : ToolResult>(
    definition: AgentDefinition
) : Agent<TInput, TOutput>(definition) {

    /**
     * 注册的工具（包括 SubAgents）
     * SubAgent 本身就是 ExecutableTool，可以直接注册
     */
    protected val tools: MutableList<ExecutableTool<*, *>> = mutableListOf()

    /**
     * 注册一个工具（可以是普通 Tool 或 SubAgent）
     *
     * @param tool 要注册的工具
     */
    fun registerTool(tool: ExecutableTool<*, *>) {
        tools.add(tool)
        sortToolsByPriority()
    }

    /**
     * 注销一个工具
     *
     * @param tool 要注销的工具
     */
    fun unregisterTool(tool: ExecutableTool<*, *>) {
        tools.remove(tool)
    }

    /**
     * 获取所有已注册的工具
     */
    fun getAllTools(): List<ExecutableTool<*, *>> = tools.toList()

    /**
     * 获取所有 SubAgent 工具
     */
    fun getSubAgents(): List<SubAgent<*, *>> {
        return tools.filterIsInstance<SubAgent<*, *>>()
    }

    /**
     * 按优先级排序工具（SubAgent 有优先级）
     */
    private fun sortToolsByPriority() {
        tools.sortBy { tool ->
            when (tool) {
                is SubAgent<*, *> -> tool.priority
                else -> Int.MAX_VALUE // 普通工具排在后面
            }
        }
    }

    /**
     * 最大迭代次数
     */
    open val maxIterations: Int = 100

    /**
     * 当前迭代次数
     */
    protected var currentIteration: Int = 0

    /**
     * 是否应该继续迭代
     * 
     * @return 是否继续
     */
    protected open fun shouldContinue(): Boolean {
        return currentIteration < maxIterations
    }

    /**
     * 重置迭代计数
     */
    protected fun resetIteration() {
        currentIteration = 0
    }

    /**
     * 增加迭代计数
     */
    protected fun incrementIteration() {
        currentIteration++
    }
}
