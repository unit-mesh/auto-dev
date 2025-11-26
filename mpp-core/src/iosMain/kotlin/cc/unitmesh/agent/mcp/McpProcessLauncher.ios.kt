package cc.unitmesh.agent.mcp

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import io.modelcontextprotocol.kotlin.sdk.shared.Transport

/**
 * iOS implementation of MCP process launcher
 * Currently not supported on iOS
 */
actual class DefaultMcpProcessLauncher : McpProcessLauncher {
    override suspend fun launchStdioProcess(config: McpProcessConfig): Transport {
        throw ToolException(
            "MCP stdio process launching is not currently supported on iOS",
            ToolErrorType.NOT_SUPPORTED
        )
    }
    
    override fun isSupported(): Boolean = false
}
