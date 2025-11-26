package cc.unitmesh.agent.mcp

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import io.modelcontextprotocol.kotlin.sdk.shared.Transport

/**
 * WebAssembly JS implementation of MCP process launcher
 * Currently not supported on WasmJS platform
 */
actual class DefaultMcpProcessLauncher : McpProcessLauncher {
    actual override suspend fun launchStdioProcess(config: McpProcessConfig): Transport {
        throw ToolException(
            "MCP stdio process launching is not currently supported on WebAssembly platform",
            ToolErrorType.NOT_SUPPORTED
        )
    }
    
    actual override fun isSupported(): Boolean = false
}
