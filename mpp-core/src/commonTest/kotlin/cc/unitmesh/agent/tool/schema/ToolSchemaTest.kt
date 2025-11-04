package cc.unitmesh.agent.tool.schema

import cc.unitmesh.agent.subagent.ErrorRecoverySchema
import cc.unitmesh.agent.subagent.LogSummarySchema
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.impl.GrepSchema
import cc.unitmesh.agent.tool.impl.ReadFileSchema
import cc.unitmesh.agent.tool.impl.ShellSchema
import cc.unitmesh.agent.tool.impl.WriteFileSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test the new declarative tool schema system
 */
class ToolSchemaTest {

    @Test
    fun testReadFileSchemaGeneration() {
        val schema = ReadFileSchema
        val jsonSchema = schema.toJsonSchema()
        
        assertTrue(jsonSchema is JsonObject)
        val schemaObj = jsonSchema as JsonObject
        
        // Check basic structure
        assertEquals("object", schemaObj["type"]?.toString()?.removeSurrounding("\""))
        assertTrue(schemaObj.containsKey("properties"))
        assertTrue(schemaObj.containsKey("required"))
        
        // Check properties
        val properties = schemaObj["properties"] as JsonObject
        assertTrue(properties.containsKey("path"))
        assertTrue(properties.containsKey("startLine"))
        assertTrue(properties.containsKey("endLine"))
        assertTrue(properties.containsKey("maxLines"))
        
        // Check required fields
        val required = schemaObj["required"] as JsonArray
        assertTrue(required.any { it.toString().removeSurrounding("\"") == "path" })
        
        // Check path property details
        val pathProp = properties["path"] as JsonObject
        assertEquals("string", pathProp["type"]?.toString()?.removeSurrounding("\""))
        assertTrue(pathProp.containsKey("description"))
    }

    @Test
    fun testWriteFileSchemaGeneration() {
        val schema = WriteFileSchema
        val jsonSchema = schema.toJsonSchema()
        
        assertTrue(jsonSchema is JsonObject)
        val schemaObj = jsonSchema as JsonObject
        
        // Check properties
        val properties = schemaObj["properties"] as JsonObject
        assertTrue(properties.containsKey("path"))
        assertTrue(properties.containsKey("content"))
        assertTrue(properties.containsKey("createDirectories"))
        assertTrue(properties.containsKey("overwrite"))
        assertTrue(properties.containsKey("append"))
        
        // Check required fields
        val required = schemaObj["required"] as JsonArray
        assertTrue(required.any { it.toString().removeSurrounding("\"") == "path" })
        assertTrue(required.any { it.toString().removeSurrounding("\"") == "content" })
        
        // Check boolean properties have defaults
        val createDirProp = properties["createDirectories"] as JsonObject
        assertEquals("boolean", createDirProp["type"]?.toString()?.removeSurrounding("\""))
        assertNotNull(createDirProp["default"])
    }

    @Test
    fun testGrepSchemaWithEnums() {
        val schema = GrepSchema
        val jsonSchema = schema.toJsonSchema()
        
        assertTrue(jsonSchema is JsonObject)
        val schemaObj = jsonSchema as JsonObject
        
        val properties = schemaObj["properties"] as JsonObject
        
        // Check pattern is required
        val required = schemaObj["required"] as JsonArray
        assertTrue(required.any { it.toString().removeSurrounding("\"") == "pattern" })
        
        // Check boolean properties with defaults
        val caseSensitiveProp = properties["caseSensitive"] as JsonObject
        assertEquals("boolean", caseSensitiveProp["type"]?.toString()?.removeSurrounding("\""))
        assertEquals("false", caseSensitiveProp["default"]?.toString())
        
        // Check integer properties with ranges
        val maxMatchesProp = properties["maxMatches"] as JsonObject
        assertEquals("integer", maxMatchesProp["type"]?.toString()?.removeSurrounding("\""))
        assertEquals("1", maxMatchesProp["minimum"]?.toString())
        assertEquals("1000", maxMatchesProp["maximum"]?.toString())
    }

    @Test
    fun testShellSchemaWithEnvironment() {
        val schema = ShellSchema
        val jsonSchema = schema.toJsonSchema()
        
        assertTrue(jsonSchema is JsonObject)
        val schemaObj = jsonSchema as JsonObject
        
        val properties = schemaObj["properties"] as JsonObject
        
        // Check command is required
        val required = schemaObj["required"] as JsonArray
        assertTrue(required.any { it.toString().removeSurrounding("\"") == "command" })
        
        // Check shell enum
        val shellProp = properties["shell"] as JsonObject
        assertEquals("string", shellProp["type"]?.toString()?.removeSurrounding("\""))
        assertTrue(shellProp.containsKey("enum"))
        
        val enumArray = shellProp["enum"] as JsonArray
        val enumValues = enumArray.map { it.toString().removeSurrounding("\"") }
        assertTrue(enumValues.contains("bash"))
        assertTrue(enumValues.contains("zsh"))
        assertTrue(enumValues.contains("sh"))
    }

    @Test
    fun testSubAgentSchemas() {
        // Test ErrorRecovery schema
        val errorSchema = ErrorRecoverySchema
        val errorJsonSchema = errorSchema.toJsonSchema() as JsonObject
        val errorProperties = errorJsonSchema["properties"] as JsonObject
        
        assertTrue(errorProperties.containsKey("errorMessage"))
        assertTrue(errorProperties.containsKey("context"))
        assertTrue(errorProperties.containsKey("codeSnippet"))
        assertTrue(errorProperties.containsKey("errorType"))
        
        // Check errorType enum
        val errorTypeProp = errorProperties["errorType"] as JsonObject
        assertTrue(errorTypeProp.containsKey("enum"))
        val errorTypeEnum = errorTypeProp["enum"] as JsonArray
        val errorTypeValues = errorTypeEnum.map { it.toString().removeSurrounding("\"") }
        assertTrue(errorTypeValues.contains("compilation"))
        assertTrue(errorTypeValues.contains("runtime"))
        
        // Test LogSummary schema
        val logSchema = LogSummarySchema
        val logJsonSchema = logSchema.toJsonSchema() as JsonObject
        val logProperties = logJsonSchema["properties"] as JsonObject
        
        assertTrue(logProperties.containsKey("logContent"))
        assertTrue(logProperties.containsKey("logType"))
        assertTrue(logProperties.containsKey("maxLines"))
        assertTrue(logProperties.containsKey("includeTimestamps"))
        assertTrue(logProperties.containsKey("focusLevel"))
    }

    @Test
    fun testParameterDescriptionGeneration() {
        val schema = ReadFileSchema
        val description = schema.getParameterDescription()
        
        assertTrue(description.contains("Read file content"))
        assertTrue(description.contains("Parameters:"))
        assertTrue(description.contains("path: string (required)"))
        assertTrue(description.contains("startLine: integer (optional)"))
        assertTrue(description.contains("maxLines: integer (optional)"))
    }

    @Test
    fun testExampleUsageGeneration() {
        val readFileExample = ReadFileSchema.getExampleUsage("read-file")
        assertTrue(readFileExample.contains("/read-file"))
        assertTrue(readFileExample.contains("path="))
        assertTrue(readFileExample.contains("startLine="))
        
        val writeFileExample = WriteFileSchema.getExampleUsage("write-file")
        assertTrue(writeFileExample.contains("/write-file"))
        assertTrue(writeFileExample.contains("path="))
        assertTrue(writeFileExample.contains("content="))
        
        val grepExample = GrepSchema.getExampleUsage("grep")
        assertTrue(grepExample.contains("/grep"))
        assertTrue(grepExample.contains("pattern="))
        assertTrue(grepExample.contains("caseSensitive="))
    }

    @Test
    fun testToolTypeSchemaIntegration() {
        // Test that ToolType objects have schemas
        assertNotNull(ToolType.ReadFile.schema)
        assertNotNull(ToolType.WriteFile.schema)
        assertNotNull(ToolType.Grep.schema)
        assertNotNull(ToolType.Glob.schema)
        assertNotNull(ToolType.Shell.schema)
        assertNotNull(ToolType.ErrorRecovery.schema)
        assertNotNull(ToolType.LogSummary.schema)
        assertNotNull(ToolType.CodebaseInvestigator.schema)
        
        // Test schema consistency
        assertEquals(ReadFileSchema, ToolType.ReadFile.schema)
        assertEquals(WriteFileSchema, ToolType.WriteFile.schema)
        assertEquals(GrepSchema, ToolType.Grep.schema)
    }
}
