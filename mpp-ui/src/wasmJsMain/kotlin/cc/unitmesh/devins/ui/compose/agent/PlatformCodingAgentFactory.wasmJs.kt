package cc.unitmesh.devins.ui.compose.agent

import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.tool.filesystem.WasmJsToolFileSystem
import cc.unitmesh.llm.KoogLLMService

/**
 * WASM-JS-specific factory for creating CodingAgent with OPFS support
 * Uses Origin Private File System for persistent storage
 */
actual fun createPlatformCodingAgent(
    projectPath: String,
    llmService: KoogLLMService,
    maxIterations: Int,
    renderer: CodingAgentRenderer,
    mcpToolConfigService: McpToolConfigService
): CodingAgent {
    val wasmFileSystem = WasmJsToolFileSystem(projectPath)
    
    return CodingAgent(
        projectPath = projectPath,
        llmService = llmService,
        maxIterations = maxIterations,
        renderer = renderer,
        fileSystem = wasmFileSystem,
        mcpToolConfigService = mcpToolConfigService
    )
}
