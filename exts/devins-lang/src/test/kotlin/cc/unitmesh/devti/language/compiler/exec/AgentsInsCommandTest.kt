package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.exec.agents.AgentsInsCommand
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking

class AgentsInsCommandTest : BasePlatformTestCase() {

    fun testCommandName() {
        val command = AgentsInsCommand(myFixture.project, "", "")
        assertEquals(BuiltinCommand.AGENTS, command.commandName)
    }

    fun testIsApplicable() {
        val command = AgentsInsCommand(myFixture.project, "", "")
        assertTrue(command.isApplicable())
    }

    fun testListAllAgentsWithoutParameters() = runBlocking {
        val command = AgentsInsCommand(myFixture.project, "", "")
        val result = command.execute()
        
        assertNotNull(result)
        assertTrue(result!!.contains("Available AI Agents"))
    }

    fun testInvokeNonExistentAgent() = runBlocking {
        val command = AgentsInsCommand(myFixture.project, "non-existent-agent \"test input\"", "")
        val result = command.execute()

        assertNotNull(result)
        assertTrue(result!!.contains("not found"))
    }

    fun testInvokeAgentWithJsonFormat() = runBlocking {
        val jsonContent = """{
  "agent": "non-existent-agent",
  "message": "test input"
}"""
        val command = AgentsInsCommand(myFixture.project, "", jsonContent)
        val result = command.execute()

        assertNotNull(result)
        // Result should contain error message about agent not found or invalid format
        assertTrue("Expected error message in result but got: $result",
            result!!.contains("not found") ||
            result.contains("Invalid request format") ||
            result.contains("Agent name is required"))
    }

    fun testParseCommandWithQuotes() = runBlocking {
        val command = AgentsInsCommand(myFixture.project, "test-agent \"Hello world\"", "")

        // Use reflection to access private parseCommand method for testing
        val parseMethod = AgentsInsCommand::class.java.getDeclaredMethod("parseCommand", String::class.java)
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(command, "test-agent \"Hello world\"") as Pair<String, String>
        assertEquals("test-agent", result.first)
        assertEquals("Hello world", result.second)
    }

    fun testListAgentsShowsA2ASection() = runBlocking {
        val command = AgentsInsCommand(myFixture.project, "", "")
        val result = command.execute()
        
        assertNotNull(result)
        // The result should contain either A2A Agents section or DevIns Agents section
        // or a message saying no agents available
        assertTrue(
            result!!.contains("A2A Agents") || 
            result.contains("DevIns Agents") || 
            result.contains("No agents available")
        )
    }

    fun testListAgentsShowsDevInsSection() = runBlocking {
        val command = AgentsInsCommand(myFixture.project, "", "")
        val result = command.execute()
        
        assertNotNull(result)
        // Should show total count or no agents message
        assertTrue(
            result!!.contains("Total:") || 
            result.contains("No agents available")
        )
    }
}

