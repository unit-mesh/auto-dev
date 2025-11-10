package cc.unitmesh.agent.parser

import cc.unitmesh.agent.tool.ToolType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for ToolCallParser to ensure it:
 * 1. Only parses tool calls within <devin> blocks
 * 2. Does NOT parse false positives from natural language text (e.g., "/blog/", "/Hibernate")
 * 3. Handles multiple tool calls within devin blocks
 * 4. Properly extracts tool parameters
 */
class ToolCallParserTest {
    private val parser = ToolCallParser()

    @Test
    fun `should not parse tool calls from plain text without devin blocks`() {
        // This is the key test - prevent false positives from LLM natural language responses
        val response = """
            I have successfully analyzed the blog creation system. Here's what I found:
            
            ## Blog Creation System Summary
            
            The project already has a complete blog creation system implemented with:
            
            **✅ Core Components:**
            - **BlogController** - REST API endpoints for blog operations
            - POST `/blog/` endpoint for creating blogs
            - GET `/blog/{id}` endpoint for retrieving blogs
            
            **✅ Key Features:**
            - Full CRUD operations implemented in service layer
            - MariaDB database integration
            - JPA/Hibernate for data persistence
            - Swagger API documentation
            
            The system is production-ready and follows Spring Boot best practices.
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertTrue(
            toolCalls.isEmpty(),
            "Should not parse any tool calls from plain text. Found: ${toolCalls.map { it.toolName }}"
        )
    }

    @Test
    fun `should parse tool call from devin block`() {
        val response = """
            Let me read the configuration file:
            
            <devin>
            /read-file path="config.yaml"
            </devin>
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertEquals(1, toolCalls.size)
        assertEquals(ToolType.ReadFile.name, toolCalls[0].toolName)
        assertEquals("config.yaml", toolCalls[0].params["path"])
    }

    @Test
    fun `should parse multiple tool calls from multiple devin blocks`() {
        val response = """
            First, let me read the file:
            
            <devin>
            /read-file path="config.yaml"
            </devin>
            
            Now let me check another file:
            
            <devin>
            /read-file path="settings.json"
            </devin>
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertEquals(2, toolCalls.size)
        assertEquals(ToolType.ReadFile.name, toolCalls[0].toolName)
        assertEquals("config.yaml", toolCalls[0].params["path"])
        assertEquals(ToolType.ReadFile.name, toolCalls[1].toolName)
        assertEquals("settings.json", toolCalls[1].params["path"])
    }

    @Test
    fun `should parse shell command with slashes in arguments`() {
        val response = """
            Let me run a curl command:
            
            <devin>
            /shell command="curl http://localhost:8080/api/blog/123"
            </devin>
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertEquals(1, toolCalls.size)
        assertEquals(ToolType.Shell.name, toolCalls[0].toolName)
        assertEquals("curl http://localhost:8080/api/blog/123", toolCalls[0].params["command"])
    }

    @Test
    fun `should not confuse API paths with tool calls`() {
        val response = """
            The API endpoints are:
            - POST /blog/create for creating blogs
            - GET /blog/{id} for retrieving blogs
            - PUT /blog/{id} for updating blogs
            - DELETE /blog/{id} for deleting blogs
            
            You can test with: curl -X POST http://localhost:8080/blog/create
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertTrue(
            toolCalls.isEmpty(),
            "Should not parse API paths as tool calls. Found: ${toolCalls.map { it.toolName }}"
        )
    }

    @Test
    fun `should not confuse technical terms starting with slash`() {
        val response = """
            The system uses the following technologies:
            - /Hibernate for ORM
            - /Spring Boot for the framework
            - /MariaDB for the database
            - /Swagger for API documentation
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertTrue(
            toolCalls.isEmpty(),
            "Should not parse technical terms as tool calls. Found: ${toolCalls.map { it.toolName }}"
        )
    }

    @Test
    fun `should parse write-file with content parameter`() {
        val response = """
            Let me create a new file:
            
            <devin>
            /write-file path="test.txt" content="Hello World"
            </devin>
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertEquals(1, toolCalls.size)
        assertEquals(ToolType.WriteFile.name, toolCalls[0].toolName)
        assertEquals("test.txt", toolCalls[0].params["path"])
        assertEquals("Hello World", toolCalls[0].params["content"])
    }

    @Test
    fun `should parse write-file and extract content from context`() {
        val response = """
            Let me create a configuration file:
            
            <devin>
            /write-file path="config.yaml"
            ```yaml
            server:
              port: 8080
            ```
            </devin>
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertEquals(1, toolCalls.size)
        assertEquals(ToolType.WriteFile.name, toolCalls[0].toolName)
        assertEquals("config.yaml", toolCalls[0].params["path"])
        assertTrue(toolCalls[0].params.containsKey("content"))
        val content = toolCalls[0].params["content"] as String
        assertTrue(content.contains("server:"))
        assertTrue(content.contains("port: 8080"))
    }

    @Test
    fun `should parse tool call with JSON parameters`() {
        val response = """
            Let me search for files:
            
            <devin>
            /glob
            ```json
            {
              "pattern": "**/*.kt",
              "exclude": ["**/build/**", "**/node_modules/**"]
            }
            ```
            </devin>
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertEquals(1, toolCalls.size)
        assertEquals(ToolType.Glob.name, toolCalls[0].toolName)
        assertEquals("**/*.kt", toolCalls[0].params["pattern"])
    }

    @Test
    fun `should handle complex LLM response with mixed content`() {
        val response = """
            I've analyzed the blog system and found:
            
            **API Endpoints:**
            - POST /blog/ - Create new blog
            - GET /blog/{id} - Get blog by ID
            - PUT /blog/{id} - Update blog
            - DELETE /blog/{id} - Delete blog
            
            The implementation uses /Hibernate for ORM and /Spring Data JPA for repositories.
            
            Let me read the controller to verify:
            
            <devin>
            /read-file path="src/main/kotlin/BlogController.kt"
            </devin>
            
            The database schema includes /blog table with fields for /id, /title, /content, and /author.
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        // Should only find the one tool call in the devin block
        assertEquals(1, toolCalls.size)
        assertEquals(ToolType.ReadFile.name, toolCalls[0].toolName)
        assertEquals("src/main/kotlin/BlogController.kt", toolCalls[0].params["path"])
    }

    @Test
    fun `should handle empty devin block`() {
        val response = """
            Here's my analysis:
            
            <devin>
            </devin>
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertTrue(toolCalls.isEmpty())
    }

    @Test
    fun `should handle devin block with only comments`() {
        val response = """
            Let me think about this:
            
            <devin>
            // This is a comment
            // No tool calls here
            </devin>
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertTrue(toolCalls.isEmpty())
    }

    @Test
    fun `should parse grep with complex pattern`() {
        val response = """
            Let me search for the pattern:
            
            <devin>
            /grep pattern="BlogController" path="src/"
            </devin>
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertEquals(1, toolCalls.size)
        assertEquals(ToolType.Grep.name, toolCalls[0].toolName)
        assertEquals("BlogController", toolCalls[0].params["pattern"])
        assertEquals("src/", toolCalls[0].params["path"])
    }

    @Test
    fun `should handle multiline shell command`() {
        val response = """
            Let me run a build command:
            
            <devin>
            /shell command="./gradlew clean build"
            </devin>
        """.trimIndent()

        val toolCalls = parser.parseToolCalls(response)
        
        assertEquals(1, toolCalls.size)
        assertEquals(ToolType.Shell.name, toolCalls[0].toolName)
        assertEquals("./gradlew clean build", toolCalls[0].params["command"])
    }
}
