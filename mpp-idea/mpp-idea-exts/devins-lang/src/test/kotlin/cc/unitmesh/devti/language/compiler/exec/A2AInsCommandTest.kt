package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.a2a.AgentRequest
import cc.unitmesh.devti.language.compiler.exec.agents.A2AInsCommand
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json.Default.decodeFromString

class A2AInsCommandTest : BasePlatformTestCase() {
    fun testParseRequestWithJsonFormat() = runBlocking {
        val jsonContent = """{"agent": "code-reviewer", "message": "Please review this code"}"""
        val command = A2AInsCommand(myFixture.project, "", jsonContent)

        // First test JSON parsing directly
        try {
            val directParse = decodeFromString<AgentRequest>(jsonContent)
            assertNotNull("Direct JSON parsing should work", directParse)
            assertEquals("code-reviewer", directParse.agent)
            assertEquals("Please review this code", directParse.message)
        } catch (e: Exception) {
            fail("Direct JSON parsing failed: ${e.message}")
        }

        val result = command.parseRequest("", jsonContent)
        assertNotNull("parseRequest should return a valid A2ARequest for JSON input", result)
        assertEquals("code-reviewer", result!!.agent)
        assertEquals("Please review this code", result.message)
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
