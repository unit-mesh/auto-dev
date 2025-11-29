package cc.unitmesh.agent.subagent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test for DomainDictAgent data classes
 */
class DomainDictAgentTest {

    // ============= Context Tests =============

    @Test
    fun testDomainDictContextCreation() {
        val context = DomainDictContext(
            userQuery = "Add authentication related terms",
            maxIterations = 7,
            focusArea = "auth"
        )

        assertEquals("Add authentication related terms", context.userQuery)
        assertEquals(7, context.maxIterations)
        assertEquals("auth", context.focusArea)
    }

    @Test
    fun testDomainDictContextDefaults() {
        val context = DomainDictContext(
            userQuery = "Simple query"
        )

        assertEquals("Simple query", context.userQuery)
        assertEquals(1, context.maxIterations) // Default value is 1
        assertEquals(null, context.focusArea)
        assertEquals(null, context.currentDict)
    }

    @Test
    fun testDomainDictContextToString() {
        val context = DomainDictContext(
            userQuery = "A very long query that should be truncated in the toString representation",
            maxIterations = 5,
            focusArea = "auth"
        )

        val str = context.toString()
        assertTrue(str.contains("DomainDictContext"))
        assertTrue(str.contains("focusArea=auth"))
        assertTrue(str.contains("maxIterations=5"))
    }

    // ============= Domain Entry Tests =============

    @Test
    fun testDomainEntryToCsvRow() {
        val entry = DomainEntry(
            chinese = "User Auth",
            codeTranslation = "UserAuth | Authentication",
            description = "User auth feature"
        )

        val csvRow = entry.toCsvRow()
        assertEquals("User Auth,UserAuth | Authentication,User auth feature", csvRow)
    }

    @Test
    fun testDomainEntryWithSpecialCharacters() {
        val entry = DomainEntry(
            chinese = "API Gateway",
            codeTranslation = "Gateway | API | GatewayService",
            description = "API endpoint (proxy) for services"
        )

        val csvRow = entry.toCsvRow()
        assertTrue(csvRow.contains("API Gateway"))
        assertTrue(csvRow.contains("Gateway | API | GatewayService"))
    }

    // ============= Schema Tests =============

    @Test
    fun testDomainDictAgentSchemaExampleUsage() {
        val example = DomainDictAgentSchema.getExampleUsage("domain-dict-agent")
        assertTrue(example.contains("/domain-dict-agent"))
        assertTrue(example.contains("focusArea"))
    }

    // ============= CSV Generation Tests =============

    @Test
    fun testMultipleEntriesCsv() {
        val entries = listOf(
            DomainEntry("User", "User | UserEntity", "User entity"),
            DomainEntry("Auth", "Auth | Authentication", "Auth module"),
            DomainEntry("Payment", "Payment | PaymentService", "Payment processing")
        )

        val csvContent = entries.joinToString("\n") { it.toCsvRow() }

        assertEquals(3, csvContent.lines().size)
        assertTrue(csvContent.contains("User,User | UserEntity,User entity"))
        assertTrue(csvContent.contains("Auth,Auth | Authentication,Auth module"))
        assertTrue(csvContent.contains("Payment,Payment | PaymentService,Payment processing"))
    }

    @Test
    fun testCsvWithHeader() {
        val header = "Chinese,Code Translation,Description"
        val entries = listOf(
            DomainEntry("Agent", "Agent | CodingAgent", "AI coding agent")
        )

        val fullCsv = listOf(header) + entries.map { it.toCsvRow() }
        val csvContent = fullCsv.joinToString("\n")

        assertTrue(csvContent.startsWith("Chinese,Code Translation,Description"))
        assertTrue(csvContent.contains("Agent,Agent | CodingAgent,AI coding agent"))
    }
}
