package cc.unitmesh.agent

import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.ToolCategory
import cc.unitmesh.agent.tool.ToolInvocation
import cc.unitmesh.agent.tool.ToolMetadata
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.impl.ReadFileTool
import cc.unitmesh.agent.tool.impl.WriteFileTool
import cc.unitmesh.agent.tool.impl.GrepTool
import cc.unitmesh.agent.tool.impl.ShellTool
import cc.unitmesh.agent.tool.schema.ToolSchema
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.agent.tool.shell.ShellResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for AgentToolFormatter
 */
class AgentToolFormatterTest {
    
    // Simple mock tool schema for testing
    private class SimpleToolSchema : ToolSchema {
        override fun toJsonSchema(): JsonElement = buildJsonObject {
            // Empty schema for testing
        }
        override fun getParameterDescription(): String = "Test parameters"
        override fun getExampleUsage(toolName: String): String = "/$toolName test=\"example\""
    }
    
    // Mock FileSystem for testing
    private class MockToolFileSystem : ToolFileSystem {
        override fun getProjectPath(): String? = "/test"
        override suspend fun readFile(path: String): String? = "mock content"
        override suspend fun writeFile(path: String, content: String, createDirectories: Boolean) {}
        override fun exists(path: String): Boolean = true
        override fun listFiles(path: String, pattern: String?): List<String> = emptyList()
        override fun resolvePath(relativePath: String): String = relativePath
        override fun getFileInfo(path: String): cc.unitmesh.agent.tool.filesystem.FileInfo? = null
        override fun createDirectory(path: String, createParents: Boolean) {}
        override fun delete(path: String, recursive: Boolean) {}
    }
    
    // Mock ShellExecutor for testing
    private class MockShellExecutor : ShellExecutor {
        override fun isAvailable(): Boolean = true
        override fun getDefaultShell(): String? = "/bin/bash"
        override suspend fun execute(
            command: String,
            config: cc.unitmesh.agent.tool.shell.ShellExecutionConfig
        ): ShellResult {
            return ShellResult(
                exitCode = 0,
                stdout = "mock output",
                stderr = "",
                command = command
            )
        }
    }
    
    @Test
    fun `formatToolListForAI should return message for empty tool list`() {
        val result = AgentToolFormatter.formatToolListForAI(emptyList())
        assertEquals("No tools available.", result)
    }
    
    @Test
    fun `formatToolListForAI should format single tool with schema`() = runTest {
        val mockFileSystem = MockToolFileSystem()
        val readFileTool = ReadFileTool(mockFileSystem)
        
        val result = AgentToolFormatter.formatToolListForAI(listOf(readFileTool))
        
        // Verify tool name
        assertContains(result, "## read-file")
        
        // Verify description is included
        assertContains(result, "**Description:**")
        assertContains(result, "Reads and returns the content")
        
        // Verify JSON schema is included
        assertContains(result, "**Parameters JSON Schema:**")
        assertContains(result, "```json")
        assertContains(result, "path")
        
        // Verify example is included
        assertContains(result, "**Example:**")
        assertContains(result, "/read-file")
    }
    
    @Test
    fun `formatToolListForAI should format multiple tools`() = runTest {
        val mockFileSystem = MockToolFileSystem()
        val readFileTool = ReadFileTool(mockFileSystem)
        val writeFileTool = WriteFileTool(mockFileSystem)
        
        val result = AgentToolFormatter.formatToolListForAI(listOf(readFileTool, writeFileTool))
        
        // Verify both tools are included
        assertContains(result, "## read-file")
        assertContains(result, "## write-file")
        
        // Verify they are in correct order
        assertTrue(result.indexOf("## read-file") < result.indexOf("## write-file"))
    }
    
    @Test
    fun `formatToolListForAI should handle tool with complex schema`() = runTest {
        val mockFileSystem = MockToolFileSystem()
        val grepTool = GrepTool(mockFileSystem)
        
        val result = AgentToolFormatter.formatToolListForAI(listOf(grepTool))
        
        assertContains(result, "## grep")
        assertContains(result, "pattern")
        assertContains(result, "**Example:**")
        assertContains(result, "/grep")
    }
    
    @Test
    fun `formatToolListForAI should include schema field in JSON`() = runTest {
        val mockShellExecutor = MockShellExecutor()
        val shellTool = ShellTool(mockShellExecutor)
        
        val result = AgentToolFormatter.formatToolListForAI(listOf(shellTool))
        
        // Verify $schema field is present
        assertContains(result, "\$schema")
        assertContains(result, "http://json-schema.org/draft-07/schema#")
    }
    
    @Test
    fun `formatToolListSimple should format tools as bullet list`() = runTest {
        val mockFileSystem = MockToolFileSystem()
        val readFileTool = ReadFileTool(mockFileSystem)
        val writeFileTool = WriteFileTool(mockFileSystem)
        
        val result = AgentToolFormatter.formatToolListSimple(listOf(readFileTool, writeFileTool))
        
        // Verify simple format
        assertContains(result, "- read-file:")
        assertContains(result, "- write-file:")
        
        // Should NOT contain JSON schema
        assertTrue(!result.contains("```json"))
        assertTrue(!result.contains("**Parameters JSON Schema:**"))
    }
    
    @Test
    fun `formatToolListSimple should handle empty list`() {
        val result = AgentToolFormatter.formatToolListSimple(emptyList())
        assertEquals("", result)
    }
    
    @Test
    fun `formatToolListForAI should generate appropriate examples for different tools`() = runTest {
        val mockFileSystem = MockToolFileSystem()
        val mockShellExecutor = MockShellExecutor()
        
        val readFileTool = ReadFileTool(mockFileSystem)
        val writeFileTool = WriteFileTool(mockFileSystem)
        val grepTool = GrepTool(mockFileSystem)
        val shellTool = ShellTool(mockShellExecutor)
        
        val tools = listOf(readFileTool, writeFileTool, grepTool, shellTool)
        val result = AgentToolFormatter.formatToolListForAI(tools)
        
        // Verify each tool has an example with appropriate parameters
        assertContains(result, "/read-file")
        assertContains(result, "/write-file")
        assertContains(result, "/grep")
        assertContains(result, "/shell")
        
        // Verify JSON format examples
        assertContains(result, "```json")
    }
    
    @Test
    fun `formatToolListForAI should handle MCP-style tools gracefully`() {
        // Create a mock MCP tool (with underscore in name)
        val mcpTool = object : cc.unitmesh.agent.tool.BaseExecutableTool<Map<String, Any>, ToolResult>() {
            override val name = "mcp_custom_tool"
            override val description = "A custom MCP tool for testing"
            override val metadata = ToolMetadata(
                displayName = "Custom MCP Tool",
                tuiEmoji = "🔧",
                composeIcon = "build",
                category = ToolCategory.SubAgent,
                schema = SimpleToolSchema()
            )
            
            override fun createToolInvocation(params: Map<String, Any>): ToolInvocation<Map<String, Any>, ToolResult> {
                val self = this
                return object : ToolInvocation<Map<String, Any>, ToolResult> {
                    override val params: Map<String, Any> = params
                    override val tool: ExecutableTool<Map<String, Any>, ToolResult> = self
                    
                    override fun getDescription(): String = "Test MCP tool invocation"
                    override fun getToolLocations(): List<cc.unitmesh.agent.tool.ToolLocation> = emptyList()
                    
                    override suspend fun execute(context: cc.unitmesh.agent.tool.ToolExecutionContext): ToolResult {
                        return ToolResult.Success("mcp result")
                    }
                }
            }
            
            override fun getParameterClass(): String = "Map"
        }
        
        val result = AgentToolFormatter.formatToolListForAI(listOf(mcpTool))
        
        assertContains(result, "## mcp_custom_tool")
        assertContains(result, "A custom MCP tool")
        assertContains(result, "/mcp_custom_tool")
    }
    
    @Test
    fun `formatToolListForAI should handle tool without description`() {
        val toolWithoutDescription = object : cc.unitmesh.agent.tool.BaseExecutableTool<Map<String, Any>, ToolResult>() {
            override val name = "test_tool"
            override val description = ""  // Empty description
            override val metadata = ToolMetadata(
                displayName = "Test Tool",
                tuiEmoji = "🧪",
                composeIcon = "science",
                category = ToolCategory.SubAgent,
                schema = SimpleToolSchema()
            )
            
            override fun createToolInvocation(params: Map<String, Any>): ToolInvocation<Map<String, Any>, ToolResult> {
                val self = this
                return object : ToolInvocation<Map<String, Any>, ToolResult> {
                    override val params: Map<String, Any> = params
                    override val tool: ExecutableTool<Map<String, Any>, ToolResult> = self
                    
                    override fun getDescription(): String = "Test tool invocation"
                    override fun getToolLocations(): List<cc.unitmesh.agent.tool.ToolLocation> = emptyList()
                    
                    override suspend fun execute(context: cc.unitmesh.agent.tool.ToolExecutionContext): ToolResult {
                        return ToolResult.Success("test result")
                    }
                }
            }
            
            override fun getParameterClass(): String = "Map"
        }
        
        val result = AgentToolFormatter.formatToolListForAI(listOf(toolWithoutDescription))
        
        assertContains(result, "## test_tool")
        assertContains(result, "Tool description not available")
    }
    
    @Test
    fun `formatToolListForAI should format schema with correct structure`() = runTest {
        val mockFileSystem = MockToolFileSystem()
        val readFileTool = ReadFileTool(mockFileSystem)
        
        val result = AgentToolFormatter.formatToolListForAI(listOf(readFileTool))
        
        // Verify JSON schema structure
        assertContains(result, "```json")
        assertContains(result, "\$schema")
        assertContains(result, "type")
        assertContains(result, "properties")
        assertContains(result, "```")
    }
    
    @Test
    fun `formatToolListSimple should include tool descriptions`() = runTest {
        val mockFileSystem = MockToolFileSystem()
        val readFileTool = ReadFileTool(mockFileSystem)
        
        val result = AgentToolFormatter.formatToolListSimple(listOf(readFileTool))
        
        // Verify format includes name and description
        assertContains(result, "read-file")
        assertContains(result, "Reads and returns")
    }
}

