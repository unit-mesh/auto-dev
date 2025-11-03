package cc.unitmesh.agent.core

import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.ToolInvocation
import cc.unitmesh.agent.tool.ToolResult

/**
 * Agent 基类 - 所有 Agent 的顶层抽象
 * 
 * Agent 本身就是一个 Tool，这样可以实现：
 * 1. Agent 可以作为工具被其他 Agent 调用
 * 2. SubAgent 可以被 MainAgent 当作工具使用
 * 3. 统一的执行接口和生命周期管理
 * 4. 统一的参数验证和错误处理
 * 
 * 参考 Gemini CLI 的设计理念：
 * - DeclarativeTool: 声明式工具定义
 * - ToolInvocation: 验证后的可执行调用
 * - SubagentToolWrapper: SubAgent 作为 Tool 的包装
 * 
 * 层次结构：
 * - Agent<TInput, TOutput> (基类, 实现 ExecutableTool)
 *   - SubAgent (子任务 Agent，如 ErrorRecovery, LogSummary)
 *   - MainAgent (主任务 Agent，如 CodingAgent)
 */
abstract class Agent<TInput : Any, TOutput : ToolResult>(
    val definition: AgentDefinition
) : ExecutableTool<TInput, TOutput> {

    /**
     * Tool 接口实现
     */
    override val name: String
        get() = definition.name

    override val description: String
        get() = definition.description

    /**
     * 获取 Agent 显示名称
     */
    val displayName: String
        get() = definition.displayName

    /**
     * 获取参数类型名称（用于 KMP 兼容）
     */
    override fun getParameterClass(): String = "AgentInput"

    /**
     * 创建 Tool 调用（ExecutableTool 接口）
     * 
     * @param params 验证后的参数
     * @return Tool 调用实例
     */
    override fun createInvocation(params: TInput): ToolInvocation<TInput, TOutput> {
        return AgentInvocation(params, this)
    }

    /**
     * 验证输入参数
     * 
     * @param input 原始输入数据
     * @return 验证后的输入对象
     * @throws IllegalArgumentException 如果输入无效
     */
    abstract fun validateInput(input: Map<String, Any>): TInput

    /**
     * 执行 Agent 的核心逻辑
     * 
     * @param input 验证后的输入
     * @param onProgress 进度回调
     * @return 结构化输出
     */
    abstract suspend fun execute(
        input: TInput,
        onProgress: (String) -> Unit = {}
    ): TOutput

    /**
     * 格式化输出为字符串（用于展示）
     * 
     * @param output 结构化输出
     * @return 格式化的字符串
     */
    abstract fun formatOutput(output: TOutput): String

    /**
     * 执行 Agent（统一入口）
     * 
     * @param rawInput 原始输入
     * @param onProgress 进度回调
     * @return 格式化后的输出字符串
     */
    suspend fun run(
        rawInput: Map<String, Any>,
        onProgress: (String) -> Unit = {}
    ): String {
        val validatedInput = validateInput(rawInput)
        val output = execute(validatedInput, onProgress)
        return formatOutput(output)
    }
}

class AgentInvocation<TInput : Any, TOutput : ToolResult>(
    override val params: TInput,
    override val tool: Agent<TInput, TOutput>
) : ToolInvocation<TInput, TOutput> {

    override fun getDescription(): String {
        return "${tool.displayName}: Executing with validated parameters"
    }

    override fun getToolLocations(): List<cc.unitmesh.agent.tool.ToolLocation> {
        return emptyList()
    }

    override suspend fun execute(
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): TOutput {
        return tool.execute(params) { progress ->
            // 可以在这里添加进度追踪逻辑
        }
    }
}
