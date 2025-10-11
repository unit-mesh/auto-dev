package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.a2a.AgentRequest
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.exec.agents.A2AInsCommand
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking

class A2AInsCommandTest : BasePlatformTestCase() {

    fun testCommandName() {
        val command = A2AInsCommand(myFixture.project, "test-agent \"Hello world\"", "")
        assertEquals(BuiltinCommand.A2A, command.commandName)
    }

    fun testParseCommandWithQuotes() = runBlocking {
        val command = A2AInsCommand(myFixture.project, "test-agent \"Hello world\"", "")

        // Use reflection to access private parseCommand method for testing
        val parseMethod = A2AInsCommand::class.java.getDeclaredMethod("parseCommand", String::class.java)
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(command, "test-agent \"Hello world\"") as Pair<String, String>
        assertEquals("test-agent", result.first)
        assertEquals("Hello world", result.second)
    }

    fun testParseCommandWithSingleQuotes() = runBlocking {
        val command = A2AInsCommand(myFixture.project, "test-agent 'Hello world'", "")

        val parseMethod = A2AInsCommand::class.java.getDeclaredMethod("parseCommand", String::class.java)
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(command, "test-agent 'Hello world'") as Pair<String, String>
        assertEquals("test-agent", result.first)
        assertEquals("Hello world", result.second)
    }

    fun testParseCommandWithoutQuotes() = runBlocking {
        val command = A2AInsCommand(myFixture.project, "test-agent Hello world", "")

        val parseMethod = A2AInsCommand::class.java.getDeclaredMethod("parseCommand", String::class.java)
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(command, "test-agent Hello world") as Pair<String, String>
        assertEquals("test-agent", result.first)
        assertEquals("Hello world", result.second)
    }

    fun testParseCommandEmpty() = runBlocking {
        val command = A2AInsCommand(myFixture.project, "", "")

        val parseMethod = A2AInsCommand::class.java.getDeclaredMethod("parseCommand", String::class.java)
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(command, "") as Pair<String, String>
        assertEquals("", result.first)
        assertEquals("", result.second)
    }

    fun testParseCommandOnlyAgentName() = runBlocking {
        val command = A2AInsCommand(myFixture.project, "test-agent", "")

        val parseMethod = A2AInsCommand::class.java.getDeclaredMethod("parseCommand", String::class.java)
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(command, "test-agent") as Pair<String, String>
        assertEquals("test-agent", result.first)
        assertEquals("", result.second)
    }

    fun testParseRequestWithJsonFormat() = runBlocking {
        val jsonContent = """{"agent": "code-reviewer", "message": "Please review this code"}"""
        val command = A2AInsCommand(myFixture.project, "", jsonContent)

        // First test JSON parsing directly
        try {
            val directParse = kotlinx.serialization.json.Json.decodeFromString<AgentRequest>(jsonContent)
            assertNotNull("Direct JSON parsing should work", directParse)
            assertEquals("code-reviewer", directParse.agent)
            assertEquals("Please review this code", directParse.message)
        } catch (e: Exception) {
            fail("Direct JSON parsing failed: ${e.message}")
        }

        val parseMethod = A2AInsCommand::class.java.getDeclaredMethod("parseRequest", String::class.java, String::class.java)
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(command, "", jsonContent) as AgentRequest?
        assertNotNull("parseRequest should return a valid A2ARequest for JSON input", result)
        assertEquals("code-reviewer", result!!.agent)
        assertEquals("Please review this code", result.message)
    }

    fun testParseRequestWithLegacyFormat() = runBlocking {
        val command = A2AInsCommand(myFixture.project, "test-agent \"Hello world\"", "")

        val parseMethod = A2AInsCommand::class.java.getDeclaredMethod("parseRequest", String::class.java, String::class.java)
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(command, "test-agent \"Hello world\"", "") as AgentRequest?
        assertNotNull(result)
        assertEquals("test-agent", result!!.agent)
        assertEquals("Hello world", result.message)
    }

    fun testIsApplicableWhenServiceNotAvailable() {
        val command = A2AInsCommand(myFixture.project, "test-agent \"Hello\"", "")

        // By default, A2AService should not be available in test environment
        assertFalse(command.isApplicable())
    }

    fun testExecuteWhenServiceNotAvailable() = runBlocking {
        val command = A2AInsCommand(myFixture.project, "test-agent \"Hello\"", "")

        val result = command.execute()
        assertNotNull(result)
        assertTrue(result!!.contains("A2A service is not available"))
    }

    fun testExecuteWithEmptyAgentName() = runBlocking {
        val command = A2AInsCommand(myFixture.project, "", "")

        val result = command.execute()
        assertNotNull(result)
        // In test environment, A2A service is not available, so we expect that message
        assertTrue("Expected A2A service not available message, got: $result",
                   result!!.contains("A2A service is not available"))
    }

    fun testExecuteWithEmptyMessage() = runBlocking {
        val command = A2AInsCommand(myFixture.project, "test-agent", "")

        val result = command.execute()
        assertNotNull(result)
        // In test environment, A2A service is not available, so we expect that message
        assertTrue("Expected A2A service not available message, got: $result",
                   result!!.contains("A2A service is not available"))
    }
}
