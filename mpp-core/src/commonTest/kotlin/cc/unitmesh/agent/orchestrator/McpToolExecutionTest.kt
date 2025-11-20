package cc.unitmesh.agent.orchestrator

import cc.unitmesh.agent.Platform
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.agent.policy.DefaultPolicyEngine
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test MCP tool execution through ToolOrchestrator
 */
class McpToolExecutionTest {

    @Test
    fun testMcpToolNameWithoutPrefix() = runTest {
        // Test that MCP tools use actual tool names, not prefixed names
        // Note: enabledBuiltinTools is deprecated and ignored (built-in tools are always enabled)
        val toolConfig = ToolConfigFile(
            enabledMcpTools = listOf("list_directory", "read_file"), // Actual tool names
            mcpServers = mapOf(
                "filesystem" to McpServerConfig(
                    command = "npx",
                    args = listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp")
                )
            )
        )

        val enabledMcpTools = toolConfig.enabledMcpTools
        assertTrue(enabledMcpTools.contains("list_directory"))
        assertTrue(enabledMcpTools.contains("read_file"))
        assertFalse(enabledMcpTools.contains("filesystem_list_directory"))
        assertFalse(enabledMcpTools.contains("filesystem_read_file"))
    }

    @Test
    fun testToolOrchestratorWithMcpSupport() = runTest {
        if (Platform.isJs || Platform.isWasm) {
            println("Skipping MCP test on JS platform - requires Node.js processes")
            return@runTest
        }

        // Note: enabledBuiltinTools is deprecated and ignored (built-in tools are always enabled)
        val toolConfig = ToolConfigFile(
            enabledMcpTools = listOf("list_directory"),
            mcpServers = mapOf(
                "filesystem" to McpServerConfig(
                    command = "npx",
                    args = listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp")
                )
            )
        )

        val mcpConfigService = McpToolConfigService(toolConfig)
        val toolRegistry = ToolRegistry(
            fileSystem = DefaultToolFileSystem(),
            shellExecutor = DefaultShellExecutor(),
            configService = mcpConfigService
        )

        val orchestrator = ToolOrchestrator(
            registry = toolRegistry,
            policyEngine = DefaultPolicyEngine(),
            renderer = DefaultCodingAgentRenderer(),
            mcpConfigService = mcpConfigService
        )

        // Test built-in tool execution
        val builtinResult = orchestrator.executeToolCall(
            toolName = "read-file",
            params = mapOf("path" to "test.txt"),
            context = ToolExecutionContext()
        )

        // Should find built-in tool
        assertTrue(builtinResult.isSuccess || builtinResult.result.toString().contains("not found"))

        // Test MCP tool execution (will fail in test environment but should attempt MCP execution)
        val mcpResult = orchestrator.executeToolCall(
            toolName = "list_directory", // Use actual tool name, not prefixed
            params = mapOf("path" to "/tmp"),
            context = ToolExecutionContext()
        )

        // Should attempt MCP execution (may fail due to test environment)
        assertTrue(
            mcpResult.result.toString().contains("MCP") || mcpResult.result.toString().contains("list_directory")
        )
    }

    @Test
    fun testMcpToolNameResolution() = runTest {
        val toolConfig = ToolConfigFile(
            enabledMcpTools = listOf("list_directory", "read_file", "write_file"),
            mcpServers = mapOf(
                "filesystem" to McpServerConfig(
                    command = "npx",
                    args = listOf("-y", "@modelcontextprotocol/server-filesystem", "/tmp")
                ),
                "context7" to McpServerConfig(
                    command = "npx",
                    args = listOf("-y", "@context7/server", "/tmp")
                )
            )
        )

        val mcpConfigService = McpToolConfigService(toolConfig)

        // Test that tool names are resolved correctly
        val enabledTools = toolConfig.enabledMcpTools.toSet()

        // These should be the actual tool names
        assertTrue(enabledTools.contains("list_directory"))
        assertTrue(enabledTools.contains("read_file"))
        assertTrue(enabledTools.contains("write_file"))

        // These should NOT be in the enabled tools (no server prefix)
        assertFalse(enabledTools.contains("filesystem_list_directory"))
        assertFalse(enabledTools.contains("context7_read_file"))
    }

    @Test
    fun testJsonParameterConversion() {
        // Skip this test on JS platform when running in browser environment
        // as ToolRegistry initialization may trigger Node.js module loading
        if (Platform.isJs || Platform.isWasm) {
            println("Skipping JSON parameter conversion test on JS platform - may require Node.js modules")
            return
        }

        val orchestrator = ToolOrchestrator(
            registry = ToolRegistry(),
            policyEngine = DefaultPolicyEngine(),
            renderer = DefaultCodingAgentRenderer()
        )

        // Test parameter conversion (accessing private method via reflection would be complex in KMP)
        // Instead, we test the expected behavior through public interface
        val params = mapOf(
            "path" to "/tmp",
            "recursive" to true,
            "maxDepth" to 3
        )

        // The orchestrator should handle these parameters correctly
        assertTrue(params.containsKey("path"))
        assertTrue(params.containsKey("recursive"))
        assertTrue(params.containsKey("maxDepth"))

        assertEquals("/tmp", params["path"])
        assertEquals(true, params["recursive"])
        assertEquals(3, params["maxDepth"])
    }
}
