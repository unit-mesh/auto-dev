package cc.unitmesh.devins.llm

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChatHistoryManagerTest {
    
    @Test
    fun `should create new session`() = runTest {
        val manager = ChatHistoryManager()
        manager.initialize()
        
        val session = manager.createSession()
        
        assertNotNull(session)
        assertEquals(0, session.messages.size)
    }
    
    @Test
    fun `should add user and assistant messages`() = runTest {
        val manager = ChatHistoryManager()
        manager.initialize()
        
        manager.addUserMessage("Hello")
        manager.addAssistantMessage("Hi there!")
        
        val messages = manager.getMessages()
        
        assertEquals(2, messages.size)
        assertEquals(MessageRole.USER, messages[0].role)
        assertEquals("Hello", messages[0].content)
        assertEquals(MessageRole.ASSISTANT, messages[1].role)
        assertEquals("Hi there!", messages[1].content)
    }
    
    @Test
    fun `should switch between sessions`() = runTest {
        val manager = ChatHistoryManager()
        manager.initialize()
        
        // Create first session and add message
        val session1 = manager.createSession()
        manager.addUserMessage("Session 1 message")
        
        // Create second session and add different message
        val session2 = manager.createSession()
        manager.addUserMessage("Session 2 message")
        
        // Switch back to first session
        manager.switchSession(session1.id)
        val messages = manager.getMessages()
        
        assertEquals(1, messages.size)
        assertEquals("Session 1 message", messages[0].content)
    }
    
    @Test
    fun `should get all non-empty sessions`() = runTest {
        val manager = ChatHistoryManager()
        manager.initialize()
        
        // Create first session with message
        manager.createSession()
        manager.addUserMessage("Message 1")
        
        // Create second empty session
        manager.createSession()
        
        // Create third session with message
        manager.createSession()
        manager.addUserMessage("Message 3")
        
        val sessions = manager.getAllSessions()
        
        // Should only return 2 sessions (non-empty ones)
        assertEquals(2, sessions.size)
    }
    
    @Test
    fun `should clear current session`() = runTest {
        val manager = ChatHistoryManager()
        manager.initialize()
        
        manager.addUserMessage("Test message")
        assertEquals(1, manager.getMessages().size)
        
        manager.clearCurrentSession()
        assertEquals(0, manager.getMessages().size)
    }
    
    @Test
    fun `should delete session`() = runTest {
        val manager = ChatHistoryManager()
        manager.initialize()
        
        val session1 = manager.createSession()
        manager.addUserMessage("Session 1")
        
        val session2 = manager.createSession()
        manager.addUserMessage("Session 2")
        
        manager.deleteSession(session1.id)
        val sessions = manager.getAllSessions()
        
        assertEquals(1, sessions.size)
        assertEquals(session2.id, sessions[0].id)
    }
    
    @Test
    fun `should get recent messages`() = runTest {
        val manager = ChatHistoryManager()
        manager.initialize()
        
        // Add 5 messages
        repeat(5) { i ->
            manager.addUserMessage("Message $i")
        }
        
        val recentMessages = manager.getRecentMessages(3)
        
        assertEquals(3, recentMessages.size)
        assertEquals("Message 2", recentMessages[0].content)
        assertEquals("Message 4", recentMessages[2].content)
    }
    
    @Test
    fun `should maintain session order by updated time`() = runTest {
        val manager = ChatHistoryManager()
        manager.initialize()
        
        // Create session 1
        val session1 = manager.createSession()
        manager.addUserMessage("First")
        
        // Wait a bit and create session 2
        kotlinx.coroutines.delay(10)
        val session2 = manager.createSession()
        manager.addUserMessage("Second")
        
        val sessions = manager.getAllSessions()
        
        // Should be ordered by most recent first
        assertEquals(session2.id, sessions[0].id)
        assertEquals(session1.id, sessions[1].id)
    }
}

