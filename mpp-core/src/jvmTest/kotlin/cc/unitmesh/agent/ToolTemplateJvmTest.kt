package cc.unitmesh.agent

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.devins.compiler.template.TemplateCompiler
import cc.unitmesh.devins.filesystem.DefaultFileSystem
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test JVM version of tool template generation with JSON Schema format
 */
class ToolTemplateJvmTest {
    
    @Test
    fun `should generate tool list with JSON Schema format in JVM`() {
        println("🔧 Testing JVM Tool Template Generation with JSON Schema")
        
        // Step 1: Create tool registry
        val projectPath = System.getProperty("java.io.tmpdir")
        val fileSystem = DefaultToolFileSystem(projectPath)
        val shellExecutor = DefaultShellExecutor()
        val mcpService = McpToolConfigService(ToolConfigFile.default())
        val toolRegistry = ToolRegistry(fileSystem, shellExecutor, mcpService)
        
        val availableTools = toolRegistry.getAllTools().values.toList()
        println("✅ Tool registry created with ${availableTools.size} tools")
        
        // Step 2: Generate tool list with new JSON Schema format
        val toolList = CodingAgentContext.formatToolListForAI(availableTools)
        
        println("✅ Tool list generated: ${toolList.length} characters")
        
        // Step 3: Verify JSON Schema format
        assertTrue(toolList.contains("## "), "Should use Markdown headers")
        assertTrue(toolList.contains("```json"), "Should have JSON Schema blocks")
        assertTrue(toolList.contains("\"${'$'}schema\""), "Should contain \$schema field")
        assertTrue(toolList.contains("draft-07/schema#"), "Should use draft-07 schema")
        assertTrue(toolList.contains("\"type\": \"object\""), "Should contain type object")
        assertTrue(toolList.contains("\"properties\""), "Should have properties field")
        assertTrue(toolList.contains("\"required\""), "Should have required field")
        assertTrue(toolList.contains("\"additionalProperties\""), "Should have additionalProperties")
        assertFalse(toolList.contains("<tool name="), "Should not contain XML tags")
        assertFalse(toolList.contains("<parameters>"), "Should not contain XML parameters")
        assertTrue(toolList.contains("**Example:**"), "Should have example blocks")
        
        // Step 4: Verify specific tools are present
        assertTrue(toolList.contains("## read-file"), "Should contain read-file tool")
        assertTrue(toolList.contains("## write-file"), "Should contain write-file tool")
        assertTrue(toolList.contains("## shell"), "Should contain shell tool")
        
        // Step 5: Verify JSON Schema structure for read-file
        val readFileStart = toolList.indexOf("## read-file")
        val readFileEnd = toolList.indexOf("## ", readFileStart + 1)
        val readFileSection = if (readFileEnd > 0) {
            toolList.substring(readFileStart, readFileEnd)
        } else {
            toolList.substring(readFileStart)
        }
        
        assertTrue(readFileSection.contains("\"path\""), "read-file should have path parameter")
        assertTrue(readFileSection.contains("\"type\": \"string\""), "path should be string type")
        assertTrue(readFileSection.contains("\"startLine\""), "read-file should have startLine parameter")
        assertTrue(readFileSection.contains("\"type\": \"integer\""), "startLine should be integer type")
        assertTrue(readFileSection.contains("\"minimum\""), "Should have minimum constraints")
        assertTrue(readFileSection.contains("\"default\""), "Should have default values")
        
        println("✅ All JSON Schema format checks passed!")
        
        // Step 6: Create complete template
        val context = CodingAgentContext(
            projectPath = projectPath,
            osInfo = "Linux Ubuntu 22.04",
            timestamp = "2024-01-01T00:00:00Z",
            toolList = toolList,
            buildTool = "gradle",
            shell = "/bin/bash"
        )
        
        val variableTable = context.toVariableTable()
        val fileSystemForTemplate = DefaultFileSystem(projectPath)
        val compiler = TemplateCompiler(variableTable, fileSystemForTemplate)
        val template = compiler.compile(CodingAgentTemplate.EN)
        
        println("✅ Template compiled: ${template.length} characters")
        
        // Step 7: Verify template quality
        assertTrue(template.contains("Available Tools"), "Template should have Available Tools section")
        assertTrue(template.contains("```json"), "Template should contain JSON Schema blocks")
        assertTrue(template.contains("draft-07/schema#"), "Template should use standard schema")
        assertTrue(template.contains("## read-file"), "Template should contain tool headers")
        assertTrue(template.contains("\"description\""), "Template should have parameter descriptions")
        assertTrue(template.contains("\"type\""), "Template should have type information")
        assertTrue(template.contains("\"required\""), "Template should have required fields")
        assertTrue(template.contains("**Example:**"), "Template should have examples")
        
        // Step 8: Export results for manual inspection
        val results = buildString {
            appendLine("# JVM Tool Template Test Results")
            appendLine("Generated at: ${java.time.LocalDateTime.now()}")
            appendLine()
            appendLine("## Metrics")
            appendLine("- Tool count: ${availableTools.size}")
            appendLine("- Tool list length: ${toolList.length} characters")
            appendLine("- Template length: ${template.length} characters")
            appendLine()
            appendLine("## Tool List Sample")
            appendLine("```")
            appendLine(toolList.substring(0, minOf(1000, toolList.length)))
            appendLine("```")
            appendLine()
            appendLine("## Template Sample")
            appendLine("```")
            appendLine(template.substring(0, minOf(1000, template.length)))
            appendLine("```")
        }
        
        File("/tmp/jvm-tool-template-test-results.md").writeText(results)
        println("✅ Results exported to /tmp/jvm-tool-template-test-results.md")
        
        println("🎉 JVM version works perfectly with JSON Schema format!")
    }
}
