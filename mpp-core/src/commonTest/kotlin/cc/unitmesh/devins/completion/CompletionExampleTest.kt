package cc.unitmesh.devins.completion

import cc.unitmesh.agent.Platform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CompletionExampleTest {
    
    @Test
    fun testCompletionManagerCreation() {
        // Skip this test on JS/WasmJS platforms where shell executor is not available
        if (Platform.isJs || Platform.isWasm) {
            return
        }

        val manager = CompletionExample.createCompletionManager()

        assertTrue(manager.supports(CompletionTriggerType.COMMAND))
        assertEquals(setOf(CompletionTriggerType.COMMAND), manager.getSupportedTriggerTypes())
    }
    
    @Test
    fun testCompletionTriggerAnalysis() {
        // Test command trigger
        val commandContext = CompletionTrigger.analyzeTrigger("/read", 5)
        assertNotNull(commandContext)
        assertEquals(CompletionTriggerType.COMMAND, commandContext.triggerType)
        assertEquals(0, commandContext.triggerOffset)
        assertEquals("read", commandContext.queryText)
        
        // Test agent trigger
        val agentContext = CompletionTrigger.analyzeTrigger("@clarify", 8)
        assertNotNull(agentContext)
        assertEquals(CompletionTriggerType.AGENT, agentContext.triggerType)
        assertEquals(0, agentContext.triggerOffset)
        assertEquals("clarify", agentContext.queryText)
        
        // Test variable trigger
        val variableContext = CompletionTrigger.analyzeTrigger("\$input", 6)
        assertNotNull(variableContext)
        assertEquals(CompletionTriggerType.VARIABLE, variableContext.triggerType)
        assertEquals(0, variableContext.triggerOffset)
        assertEquals("input", variableContext.queryText)
    }
    
    @Test
    fun testCompletionTriggerWithWhitespace() {
        // Should trigger when there's no whitespace between trigger and cursor
        val context1 = CompletionTrigger.analyzeTrigger("/read", 5)
        assertNotNull(context1)
        assertEquals("read", context1.queryText)

        // Should trigger when cursor is right after trigger
        val context2 = CompletionTrigger.analyzeTrigger("/", 1)
        assertNotNull(context2)
        assertEquals("", context2.queryText)
    }
    
    @Test
    fun testCompletionManagerIntegration() {
        // Skip this test on JS/WasmJS platforms where shell executor is not available
        if (Platform.isJs || Platform.isWasm) {
            return
        }

        val manager = CompletionExample.createCompletionManager()

        val context = CompletionContext(
            fullText = "/grep",
            cursorPosition = 5,
            triggerType = CompletionTriggerType.COMMAND,
            triggerOffset = 0,
            queryText = "grep"
        )

        val completions = manager.getCompletions(context)
        assertTrue(completions.isNotEmpty(), "Should have completions")

        val grepCompletion = completions.find { it.text == "grep" }
        assertNotNull(grepCompletion, "Should find grep completion")
        assertEquals("🔍", grepCompletion.icon)
    }
    
    @Test
    fun testUnsupportedTriggerType() {
        // Skip this test on JS/WasmJS platforms where shell executor is not available
        if (Platform.isJs || Platform.isWasm) {
            return
        }

        val manager = CompletionExample.createCompletionManager()

        val context = CompletionContext(
            fullText = "@agent",
            cursorPosition = 6,
            triggerType = CompletionTriggerType.AGENT,
            triggerOffset = 0,
            queryText = "agent"
        )

        val completions = manager.getCompletions(context)
        assertTrue(completions.isEmpty(), "Should have no completions for unsupported trigger type")
    }
    
    @Test
    fun testCommandValueTrigger() {
        val context = CompletionTrigger.analyzeTrigger("/file:path", 10)
        assertNotNull(context)
        assertEquals(CompletionTriggerType.COMMAND_VALUE, context.triggerType)
        assertEquals(5, context.triggerOffset) // Position of ':'
        assertEquals("path", context.queryText)
    }
    
    @Test
    fun testEmptyQuery() {
        val context = CompletionTrigger.analyzeTrigger("/", 1)
        assertNotNull(context)
        assertEquals(CompletionTriggerType.COMMAND, context.triggerType)
        assertEquals("", context.queryText)
        assertTrue(context.isEmptyQuery)
    }
    
    @Test
    fun testInvalidCursorPosition() {
        // Cursor before start
        val context1 = CompletionTrigger.analyzeTrigger("/read", -1)
        assertEquals(null, context1)
        
        // Cursor after end
        val context2 = CompletionTrigger.analyzeTrigger("/read", 10)
        assertEquals(null, context2)
        
        // Cursor at position 0
        val context3 = CompletionTrigger.analyzeTrigger("/read", 0)
        assertEquals(null, context3)
    }
}
