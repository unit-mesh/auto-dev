package cc.unitmesh.agent.model

import kotlinx.serialization.Serializable

/**
 * Agent 定义 - 声明式配置
 * 
 * 参考 Gemini CLI 的 AgentDefinition 设计
 */
@Serializable
data class AgentDefinition(
    val name: String,
    val displayName: String,
    val description: String,
    val promptConfig: PromptConfig,
    val modelConfig: ModelConfig,
    val runConfig: RunConfig,
    val toolConfig: ToolConfig? = null,
    val inputSchema: Map<String, InputParameter> = emptyMap(),
    val outputSchema: OutputSchema? = null
)

/**
 * 提示词配置
 */
@Serializable
data class PromptConfig(
    val systemPrompt: String,
    val queryTemplate: String? = null,
    val initialMessages: List<String> = emptyList()
)

/**
 * 模型配置
 */
@Serializable
data class ModelConfig(
    val modelId: String,
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096,
    val topP: Double = 0.95
)

/**
 * 运行配置
 */
@Serializable
data class RunConfig(
    val maxTurns: Int = 50,
    val maxTimeMinutes: Int = 10,
    val terminateOnError: Boolean = false
)

/**
 * 工具配置
 */
@Serializable
data class ToolConfig(
    val allowedTools: List<String>,
    val toolTimeout: Int = 30000
)

/**
 * 输入参数定义
 */
@Serializable
data class InputParameter(
    val type: String,
    val required: Boolean,
    val description: String,
    val defaultValue: String? = null
)

/**
 * 输出结构定义
 */
@Serializable
data class OutputSchema(
    val fields: Map<String, String>,
    val required: List<String> = emptyList()
)

