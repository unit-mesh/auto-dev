package cc.unitmesh.devins.ui.compose.agent

import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.tool.shell.PtyShellExecutor
import cc.unitmesh.llm.KoogLLMService

/**
 * JVM-specific factory for creating CodingAgent
 * Uses default file system implementation
 */
actual fun createPlatformCodingAgent(
    projectPath: String,
    llmService: KoogLLMService,
    maxIterations: Int,
    renderer: CodingAgentRenderer,
    mcpToolConfigService: McpToolConfigService
): CodingAgent {
    return CodingAgent(
        projectPath = projectPath,
        llmService = llmService,
        maxIterations = maxIterations,
        renderer = renderer,
        mcpToolConfigService = mcpToolConfigService,
        shellExecutor = PtyShellExecutor()
    )
}

