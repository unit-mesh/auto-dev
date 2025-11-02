package cc.unitmesh.agent.core

import cc.unitmesh.agent.model.AgentDefinition

/**
 * SubAgent 抽象基类
 * 
 * SubAgent 是独立的执行单元，具有：
 * 1. 独立的 LLM 会话上下文
 * 2. 独立的工具权限控制
 * 3. 独立的超时和重试策略
 * 4. 结构化的输入输出
 * 
 * 参考 Gemini CLI 的 AgentExecutor 设计
 */
abstract class SubAgent<TInput, TOutput>(
    val definition: AgentDefinition
) {
    /**
     * 验证输入
     * 
     * @param input 原始输入数据
     * @return 验证后的输入对象
     * @throws IllegalArgumentException 如果输入无效
     */
    abstract fun validateInput(input: Map<String, Any>): TInput

    /**
     * 执行 SubAgent
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
     * 执行 SubAgent（统一入口）
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

    /**
     * 获取 Agent 名称
     */
    val name: String
        get() = definition.name

    /**
     * 获取 Agent 显示名称
     */
    val displayName: String
        get() = definition.displayName
}

