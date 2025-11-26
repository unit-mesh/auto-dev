package cc.unitmesh.agent.mcp

import cc.unitmesh.agent.tool.shell.ShellEnvironmentUtils
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File

/**
 * JVM implementation of MCP process launcher using ProcessBuilder
 */
actual class DefaultMcpProcessLauncher : McpProcessLauncher {
    
    actual override suspend fun launchStdioProcess(config: McpProcessConfig): Transport {
        // Create process builder
        val processBuilder = ProcessBuilder(listOf(config.command) + config.args)
        
        // Set working directory if specified
        config.workingDirectory?.let { cwd ->
            processBuilder.directory(File(cwd))
        }
        
        // Get environment
        val environment = processBuilder.environment()
        
        // Inherit login shell environment if requested
        if (config.inheritLoginEnv) {
            ShellEnvironmentUtils.applyLoginEnvironment(environment)
        }
        
        // Apply user-specified environment variables (these override inherited ones)
        if (config.environment.isNotEmpty()) {
            environment.putAll(config.environment)
        }
        
        // Start the process
        val process = processBuilder.start()
        
        // Create stdio transport from process streams
        val input = process.inputStream.asSource().buffered()
        val output = process.outputStream.asSink().buffered()
        
        return StdioClientTransport(input, output)
    }
    
    actual override fun isSupported(): Boolean = true
}
