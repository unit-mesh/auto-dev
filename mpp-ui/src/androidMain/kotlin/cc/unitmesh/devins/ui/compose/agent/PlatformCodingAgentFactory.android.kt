package cc.unitmesh.devins.ui.compose.agent

import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.tool.filesystem.AndroidToolFileSystem
import cc.unitmesh.devins.ui.platform.AndroidActivityProvider
import cc.unitmesh.llm.KoogLLMService

/**
 * Android-specific factory for creating CodingAgent with SAF support
 */
actual fun createPlatformCodingAgent(
    projectPath: String,
    llmService: KoogLLMService,
    maxIterations: Int,
    renderer: CodingAgentRenderer,
    mcpToolConfigService: McpToolConfigService
): CodingAgent {
    val activity = AndroidActivityProvider.getActivity()
    val context = activity?.applicationContext

    return if (context != null) {
        val androidFileSystem = AndroidToolFileSystem(context, projectPath)
        CodingAgent(
            projectPath = projectPath,
            llmService = llmService,
            maxIterations = maxIterations,
            renderer = renderer,
            fileSystem = androidFileSystem,
            mcpToolConfigService = mcpToolConfigService
        )
    } else {
        println("⚠️ Warning: No Android context available, using default file system")
        CodingAgent(
            projectPath = projectPath,
            llmService = llmService,
            maxIterations = maxIterations,
            renderer = renderer,
            mcpToolConfigService = mcpToolConfigService
        )
    }
}
