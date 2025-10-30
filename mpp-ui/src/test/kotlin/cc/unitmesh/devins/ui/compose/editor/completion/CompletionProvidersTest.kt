package cc.unitmesh.devins.ui.compose.editor.completion

import cc.unitmesh.devins.ui.compose.editor.model.CompletionContext
import cc.unitmesh.devins.ui.compose.editor.model.CompletionTriggerType
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * 补全提供者测试
 */
class CompletionProvidersTest {
    
    @Test
    fun `AgentCompletionProvider should provide agents`() {
        val provider = AgentCompletionProvider()
        val context = CompletionContext(
            fullText = "@cla",
            cursorPosition = 4,
            triggerType = CompletionTriggerType.AGENT,
            triggerOffset = 0,
            queryText = "cla"
        )
        
        val completions = provider.getCompletions(context)
        
        assertTrue(completions.isNotEmpty(), "Should provide completions")
        assertTrue(completions.any { it.text == "clarify" }, "Should include 'clarify' agent")
    }
    
    @Test
    fun `AgentCompletionProvider should filter by query`() {
        val provider = AgentCompletionProvider()
        val context = CompletionContext(
            fullText = "@test",
            cursorPosition = 5,
            triggerType = CompletionTriggerType.AGENT,
            triggerOffset = 0,
            queryText = "test"
        )
        
        val completions = provider.getCompletions(context)
        
        assertTrue(completions.isNotEmpty(), "Should provide completions")
        assertTrue(completions.any { it.text == "test-gen" }, "Should include 'test-gen' agent")
    }
    
    @Test
    fun `CommandCompletionProvider should provide commands`() {
        val provider = CommandCompletionProvider()
        val context = CompletionContext(
            fullText = "/fi",
            cursorPosition = 3,
            triggerType = CompletionTriggerType.COMMAND,
            triggerOffset = 0,
            queryText = "fi"
        )
        
        val completions = provider.getCompletions(context)
        
        assertTrue(completions.isNotEmpty(), "Should provide completions")
        assertTrue(completions.any { it.text == "file" }, "Should include 'file' command")
    }
    
    @Test
    fun `CommandCompletionProvider should support all commands`() {
        val provider = CommandCompletionProvider()
        val context = CompletionContext(
            fullText = "/",
            cursorPosition = 1,
            triggerType = CompletionTriggerType.COMMAND,
            triggerOffset = 0,
            queryText = ""
        )
        
        val completions = provider.getCompletions(context)
        
        assertTrue(completions.size >= 4, "Should provide at least 4 commands")
        val commandNames = completions.map { it.text }
        assertTrue("file" in commandNames, "Should include 'file'")
        assertTrue("symbol" in commandNames, "Should include 'symbol'")
        assertTrue("write" in commandNames, "Should include 'write'")
        assertTrue("run" in commandNames, "Should include 'run'")
    }
    
    @Test
    fun `VariableCompletionProvider should extract variables from frontmatter`() {
        val provider = VariableCompletionProvider()
        val text = """
            ---
            name: "Test"
            input: "value"
            output: "result"
            ---
            
            Text here
        """.trimIndent()
        
        val context = CompletionContext(
            fullText = text + "\n\$in",
            cursorPosition = text.length + 4,
            triggerType = CompletionTriggerType.VARIABLE,
            triggerOffset = text.length + 1,
            queryText = "in"
        )
        
        val completions = provider.getCompletions(context)
        
        assertTrue(completions.isNotEmpty(), "Should provide completions")
        assertTrue(completions.any { it.text == "input" }, "Should include 'input' variable")
    }
    
    @Test
    fun `VariableCompletionProvider should include predefined variables`() {
        val provider = VariableCompletionProvider()
        val context = CompletionContext(
            fullText = "\$con",
            cursorPosition = 4,
            triggerType = CompletionTriggerType.VARIABLE,
            triggerOffset = 0,
            queryText = "con"
        )
        
        val completions = provider.getCompletions(context)
        
        assertTrue(completions.isNotEmpty(), "Should provide completions")
        assertTrue(completions.any { it.text == "context" }, "Should include 'context' variable")
    }
    
    @Test
    fun `FilePathCompletionProvider should provide common paths`() {
        val provider = FilePathCompletionProvider()
        val context = CompletionContext(
            fullText = "/file:src",
            cursorPosition = 9,
            triggerType = CompletionTriggerType.COMMAND_VALUE,
            triggerOffset = 5,
            queryText = "src"
        )
        
        val completions = provider.getCompletions(context)
        
        assertTrue(completions.isNotEmpty(), "Should provide completions")
        assertTrue(completions.any { it.text.startsWith("src/") }, "Should include paths starting with 'src/'")
    }
    
    @Test
    fun `CompletionManager should select correct provider`() {
        val manager = CompletionManager()
        
        // Test agent completion
        val agentContext = CompletionContext(
            fullText = "@test",
            cursorPosition = 5,
            triggerType = CompletionTriggerType.AGENT,
            triggerOffset = 0,
            queryText = "test"
        )
        val agentCompletions = manager.getCompletions(agentContext)
        assertTrue(agentCompletions.isNotEmpty(), "Should provide agent completions")
        
        // Test command completion
        val commandContext = CompletionContext(
            fullText = "/file",
            cursorPosition = 5,
            triggerType = CompletionTriggerType.COMMAND,
            triggerOffset = 0,
            queryText = "file"
        )
        val commandCompletions = manager.getCompletions(commandContext)
        assertTrue(commandCompletions.isNotEmpty(), "Should provide command completions")
    }
    
    @Test
    fun `CompletionItem matchScore should prioritize exact matches`() {
        val item = cc.unitmesh.devins.ui.compose.editor.model.CompletionItem(
            text = "file",
            displayText = "file"
        )
        
        val exactScore = item.matchScore("file")
        val prefixScore = item.matchScore("fi")
        val containsScore = item.matchScore("il")
        
        assertTrue(exactScore > prefixScore, "Exact match should score higher than prefix")
        assertTrue(prefixScore > containsScore, "Prefix match should score higher than contains")
    }
}

