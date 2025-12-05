package cc.unitmesh.devti.llms

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.llm2.model.ModelType
import cc.unitmesh.devti.llms.custom.Message
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test

class LLMProviderAdapterTest : BasePlatformTestCase() {

    @Test
    fun testAdapterCreation() {
        val adapter = LLMProviderAdapter(project)
        assertNotNull(adapter)
        assertEquals(600L, adapter.defaultTimeout)
    }

    @Test
    fun testAdapterWithModelType() {
        val adapter = LLMProviderAdapter(project, ModelType.Completion)
        assertNotNull(adapter)
    }

    @Test
    fun testMessageManagement() {
        val adapter = LLMProviderAdapter(project)
        
        // Initially empty
        assertTrue(adapter.getAllMessages().isEmpty())
        
        // Add a message
        adapter.appendLocalMessage("Hello", ChatRole.User)
        assertEquals(1, adapter.getAllMessages().size)
        assertEquals("user", adapter.getAllMessages()[0].role)
        assertEquals("Hello", adapter.getAllMessages()[0].content)
        
        // Add another message
        adapter.appendLocalMessage("Hi there", ChatRole.Assistant)
        assertEquals(2, adapter.getAllMessages().size)
        
        // Clear messages
        adapter.clearMessage()
        assertTrue(adapter.getAllMessages().isEmpty())
    }

    @Test
    fun testEmptyMessageIgnored() {
        val adapter = LLMProviderAdapter(project)
        
        // Empty message should be ignored
        adapter.appendLocalMessage("", ChatRole.User)
        assertTrue(adapter.getAllMessages().isEmpty())
        
        // Non-empty message should be added
        adapter.appendLocalMessage("Hello", ChatRole.User)
        assertEquals(1, adapter.getAllMessages().size)
    }

    // Note: We can't easily test the stream() method without a real LLM provider
    // as it requires network connectivity and actual LLM configuration.
    // In a real test environment, you would mock the LLMProvider2 dependency.
}
