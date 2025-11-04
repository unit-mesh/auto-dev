package cc.unitmesh.devins.integration

import cc.unitmesh.agent.CodingAgentContext
import cc.unitmesh.agent.CodingAgentTemplate
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.mcp.McpToolConfigService
import cc.unitmesh.devins.compiler.template.TemplateCompiler
import cc.unitmesh.devins.filesystem.DefaultFileSystem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.*
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * JVM Integration Test for Tool Template Generation
 * Tests the complete flow from tool registry to template generation with JSON Schema format
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JvmToolTemplateIntegrationTest {
    
    private lateinit var toolRegistry: ToolRegistry
    private lateinit var toolList: String
    
    @BeforeAll
    fun setup() {
        val projectPath = System.getProperty("java.io.tmpdir")
        val fileSystem = DefaultToolFileSystem(projectPath, DefaultFileSystem())
        val shellExecutor = DefaultShellExecutor()
        val mcpService = McpToolConfigService()
        
        toolRegistry = ToolRegistry(fileSystem, shellExecutor, mcpService)
        toolList = CodingAgentContext.formatToolListForAI(toolRegistry.getAllTools().values.toList())
    }
    
    @Test
    fun `should create tool registry successfully`() {
        assertNotNull(toolRegistry)
        val availableTools = toolRegistry.getAllTools().values.toList()
        assertTrue(availableTools.isNotEmpty(), "Should have available tools")
        println("✅ Tool registry created with ${availableTools.size} tools")
    }
    
    @Test
    fun `should generate tool list with JSON Schema format`() {
        assertNotNull(toolList)
        assertTrue(toolList.length > 1000, "Tool list should be substantial")
        
        // Check JSON Schema format
        assertTrue(toolList.contains("## "), "Should use Markdown headers")
        assertTrue(toolList.contains("```json"), "Should have JSON Schema blocks")
        assertTrue(toolList.contains("\"${'$'}schema\""), "Should contain \$schema field")
        assertTrue(toolList.contains("draft-07/schema#"), "Should use draft-07 schema")
        assertTrue(toolList.contains("\"type\": \"object\""), "Should contain type object")
        assertTrue(toolList.contains("\"properties\""), "Should have properties field")
        assertTrue(toolList.contains("\"required\""), "Should have required field")
        assertTrue(toolList.contains("\"additionalProperties\""), "Should have additionalProperties")
        
        // Should not contain XML format
        assertFalse(toolList.contains("<tool name="), "Should not contain XML tags")
        assertFalse(toolList.contains("<parameters>"), "Should not contain XML parameters")
        
        // Should contain examples
        assertTrue(toolList.contains("**Example:**"), "Should have example blocks")
        
        println("✅ Tool list generated with JSON Schema format: ${toolList.length} characters")
    }
    
    @Test
    fun `should contain all expected tools`() {
        val expectedTools = listOf("read-file", "write-file", "shell", "grep", "glob")
        
        expectedTools.forEach { toolName ->
            assertTrue(toolList.contains("## $toolName"), "Should contain $toolName tool")
        }
        
        println("✅ All expected tools found in tool list")
    }
    
    @Test
    fun `should have detailed parameter information for read-file`() {
        val readFileStart = toolList.indexOf("## read-file")
        assertTrue(readFileStart >= 0, "Should contain read-file tool")
        
        val readFileEnd = toolList.indexOf("## ", readFileStart + 1)
        val readFileSection = if (readFileEnd > 0) {
            toolList.substring(readFileStart, readFileEnd)
        } else {
            toolList.substring(readFileStart)
        }
        
        // Check parameter details
        assertTrue(readFileSection.contains("\"path\""), "Should have path parameter")
        assertTrue(readFileSection.contains("\"type\": \"string\""), "path should be string type")
        assertTrue(readFileSection.contains("\"startLine\""), "Should have startLine parameter")
        assertTrue(readFileSection.contains("\"type\": \"integer\""), "startLine should be integer type")
        assertTrue(readFileSection.contains("\"minimum\""), "Should have minimum constraints")
        assertTrue(readFileSection.contains("\"default\""), "Should have default values")
        assertTrue(readFileSection.contains("\"description\""), "Should have descriptions")
        
        println("✅ read-file tool has detailed parameter information")
    }
    
    @Test
    fun `should generate complete template with JSON Schema`() {
        val projectPath = System.getProperty("java.io.tmpdir")
        val context = CodingAgentContext(
            projectPath = projectPath,
            osInfo = "Linux Ubuntu 22.04",
            timestamp = "2024-01-01T00:00:00Z",
            toolList = toolList,
            buildTool = "gradle",
            shell = "/bin/bash"
        )
        
        val variableTable = context.toVariableTable()
        val fileSystem = DefaultFileSystem()
        val compiler = TemplateCompiler(variableTable, fileSystem)
        val template = compiler.compile(CodingAgentTemplate.EN)
        
        assertNotNull(template)
        assertTrue(template.length > 5000, "Template should be substantial")
        
        // Check template structure
        assertTrue(template.contains("Environment Information"), "Should have Environment Information")
        assertTrue(template.contains("Available Tools"), "Should have Available Tools section")
        assertTrue(template.contains("Task Execution Guidelines"), "Should have Task Execution Guidelines")
        assertTrue(template.contains("Response Format"), "Should have Response Format")
        
        // Check JSON Schema format in template
        assertTrue(template.contains("```json"), "Template should contain JSON Schema blocks")
        assertTrue(template.contains("\"${'$'}schema\""), "Template should contain \$schema field")
        assertTrue(template.contains("draft-07/schema#"), "Template should use standard schema")
        assertTrue(template.contains("## read-file"), "Template should contain tool headers")
        assertTrue(template.contains("**Example:**"), "Template should have examples")
        
        println("✅ Complete template generated: ${template.length} characters")
        
        // Export template for manual inspection
        File("/tmp/jvm-integration-test-template.md").writeText(template)
        println("✅ Template exported to /tmp/jvm-integration-test-template.md")
    }
    
    @Test
    fun `should generate tool list efficiently`() {
        val duration = measureTimeMillis {
            val testToolRegistry = toolRegistry
            val testToolList = CodingAgentContext.formatToolListForAI(testToolRegistry.getAllTools().values.toList())
            assertTrue(testToolList.length > 5000, "Should generate substantial content")
        }
        
        assertTrue(duration < 2000, "Should complete within 2 seconds, took ${duration}ms")
        println("✅ Tool list generation completed in ${duration}ms")
    }
    
    @Test
    fun `should have improved information density`() {
        // The new format should be significantly longer than the old XML format
        assertTrue(toolList.length > 8000, "Expected ~8.5k+ characters, got ${toolList.length}")
        
        // Should have rich parameter information
        val parameterMatches = "\"type\":".toRegex().findAll(toolList).count()
        assertTrue(parameterMatches > 20, "Should have many type definitions, found $parameterMatches")
        
        val descriptionMatches = "\"description\":".toRegex().findAll(toolList).count()
        assertTrue(descriptionMatches > 15, "Should have many descriptions, found $descriptionMatches")
        
        println("✅ Information density improved: ${toolList.length} chars, $parameterMatches types, $descriptionMatches descriptions")
    }
}
