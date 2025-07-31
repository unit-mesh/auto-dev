package cc.unitmesh.devti.mcp.client

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class McpConfigTest {

    @Test
    fun should_parse_config_with_command_only() {
        val configJson = """
        {
          "mcpServers": {
            "stdio-server": {
              "command": "npx",
              "args": ["@modelcontextprotocol/server-stdio"]
            }
          }
        }
        """.trimIndent()

        val config = McpServer.tryParse(configJson)

        assertNotNull(config)
        assertEquals(1, config.mcpServers.size)
        
        val server = config.mcpServers["stdio-server"]!!
        assertEquals("npx", server.command)
        assertNull(server.url)
        assertEquals(listOf("@modelcontextprotocol/server-stdio"), server.args)
    }

    @Test
    fun should_parse_config_with_url_only() {
        val configJson = """
        {
          "mcpServers": {
            "http-server": {
              "url": "http://localhost:8080/mcp",
              "args": []
            }
          }
        }
        """.trimIndent()

        val config = McpServer.tryParse(configJson)

        assertNotNull(config)
        assertEquals(1, config.mcpServers.size)
        
        val server = config.mcpServers["http-server"]!!
        assertNull(server.command)
        assertEquals("http://localhost:8080/mcp", server.url)
        assertEquals(emptyList(), server.args)
    }

    @Test
    fun should_parse_config_with_both_command_and_url() {
        val configJson = """
        {
          "mcpServers": {
            "mixed-server": {
              "command": "npx",
              "url": "http://localhost:8080/mcp",
              "args": ["@modelcontextprotocol/server-stdio"]
            }
          }
        }
        """.trimIndent()

        val config = McpServer.tryParse(configJson)

        assertNotNull(config)
        assertEquals(1, config.mcpServers.size)
        
        val server = config.mcpServers["mixed-server"]!!
        assertEquals("npx", server.command)
        assertEquals("http://localhost:8080/mcp", server.url)
        assertEquals(listOf("@modelcontextprotocol/server-stdio"), server.args)
    }

    @Test
    fun should_parse_config_with_all_optional_fields() {
        val configJson = """
        {
          "mcpServers": {
            "full-server": {
              "url": "https://api.example.com/mcp",
              "args": [],
              "disabled": false,
              "autoApprove": ["tool1", "tool2"],
              "env": {
                "API_KEY": "secret123",
                "DEBUG": "true"
              },
              "requiresConfirmation": ["dangerous_tool"]
            }
          }
        }
        """.trimIndent()

        val config = McpServer.tryParse(configJson)

        assertNotNull(config)
        assertEquals(1, config.mcpServers.size)
        
        val server = config.mcpServers["full-server"]!!
        assertNull(server.command)
        assertEquals("https://api.example.com/mcp", server.url)
        assertEquals(emptyList(), server.args)
        assertEquals(false, server.disabled)
        assertEquals(listOf("tool1", "tool2"), server.autoApprove)
        assertEquals(mapOf("API_KEY" to "secret123", "DEBUG" to "true"), server.env)
        assertEquals(listOf("dangerous_tool"), server.requiresConfirmation)
    }

    @Test
    fun should_return_null_for_invalid_json() {
        val invalidJson = """
        {
          "mcpServers": {
            "invalid-server": {
              "command": "npx"
              // missing comma and args field
            }
          }
        }
        """.trimIndent()

        val config = McpServer.tryParse(invalidJson)

        assertNull(config)
    }

    @Test
    fun should_return_null_for_empty_string() {
        val config = McpServer.tryParse("")
        assertNull(config)
    }

    @Test
    fun should_return_null_for_null_string() {
        val config = McpServer.tryParse(null)
        assertNull(config)
    }
}
