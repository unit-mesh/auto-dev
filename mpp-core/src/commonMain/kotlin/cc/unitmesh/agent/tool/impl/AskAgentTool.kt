package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.core.SubAgentManager
import cc.unitmesh.agent.tool.BaseExecutableTool
import cc.unitmesh.agent.tool.ToolExecutionContext
import cc.unitmesh.agent.tool.ToolInvocation
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import kotlinx.serialization.Serializable

/**
 * AskAgent 工具参数
 */
@Serializable
data class AskSubAgentParams(
    val agentName: String,
    val question: String,
    val context: Map<String, String> = emptyMap()
)

/**
 * AskAgent 工具 Schema
 */
object AskSubAgentSchema : DeclarativeToolSchema(
    description = "Ask a question to a specific Agent that has analyzed content",
    properties = mapOf(
        "agentName" to string(
            description = "Name of the Agent to ask (e.g., 'analysis-agent', 'error-agent')",
            required = true
        ),
        "question" to string(
            description = "The question to ask the Agent",
            required = true
        ),
        "context" to string(
            description = "Additional context for the question as JSON string"
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return "/$toolName agentName=\"analysis-agent\" question=\"What were the main insights from the last analysis?\""
    }
}

/**
 * AskAgent 工具
 *
 * 允许 CodingAgent 向特定的 Agent 提问，实现 Agent 间的对话机制
 * 这是多Agent体系的核心通信工具
 */
class AskAgentTool(
    private val subAgentManager: SubAgentManager
) : BaseExecutableTool<AskSubAgentParams, ToolResult.AgentResult>() {

    override val name: String = "ask-agent"
    override val description: String = "Ask a question to a specific Agent"

    override fun getParameterClass(): String = AskSubAgentParams::class.simpleName ?: "AskSubAgentParams"

    override fun createToolInvocation(params: AskSubAgentParams): ToolInvocation<AskSubAgentParams, ToolResult.AgentResult> {
        val outerTool = this
        return object : ToolInvocation<AskSubAgentParams, ToolResult.AgentResult> {
            override val params: AskSubAgentParams = params
            override val tool = outerTool

            override fun getDescription(): String =
                "Ask '${params.question}' to Agent '${params.agentName}'"

            override fun getToolLocations(): List<cc.unitmesh.agent.tool.ToolLocation> = emptyList()

            override suspend fun execute(context: ToolExecutionContext): ToolResult.AgentResult {
                // 转换 context 参数
                val questionContext = params.context.mapValues { it.value as Any }
                
                return subAgentManager.askSubAgent(
                    subAgentName = params.agentName,
                    question = params.question,
                    context = questionContext
                )
            }
        }
    }


}
