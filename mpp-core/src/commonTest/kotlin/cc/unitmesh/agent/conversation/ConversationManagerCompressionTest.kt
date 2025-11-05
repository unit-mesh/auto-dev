package cc.unitmesh.agent.conversation

import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.compression.CompressionConfig
import cc.unitmesh.llm.compression.CompressionResult
import cc.unitmesh.llm.compression.CompressionStatus
import cc.unitmesh.llm.compression.TokenInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConversationManagerCompressionTest {
    
    private fun createMockLLMService(): KoogLLMService {
        val config = ModelConfig(
            provider = LLMProviderType.OPENAI,
            modelName = "gpt-3.5-turbo",
            apiKey = "mock-api-key",
            baseUrl = "https://api.openai.com/v1",
            maxTokens = 1000
        )
        
        val compressionConfig = CompressionConfig(
            contextPercentageThreshold = 0.7,
            autoCompressionEnabled = true
        )
        
        return KoogLLMService.create(config, compressionConfig)
    }
    
    @Test
    fun testConversationManagerInitialization() {
        val llmService = createMockLLMService()
        val conversationManager = ConversationManager(
            llmService = llmService,
            systemPrompt = "你是一个有用的助手",
            autoCompress = true
        )
        
        assertNotNull(conversationManager)
        
        // 验证初始对话历史包含系统消息
        val history = conversationManager.getHistory()
        assertEquals(1, history.size)
        assertEquals(MessageRole.SYSTEM, history[0].role)
        assertEquals("你是一个有用的助手", history[0].content)
    }
    
    @Test
    fun testNeedsCompressionCheck() {
        val llmService = createMockLLMService()
        val conversationManager = ConversationManager(
            llmService = llmService,
            systemPrompt = "你是一个助手"
        )
        
        // 初始状态不需要压缩
        assertFalse(conversationManager.needsCompression(), "初始状态不应该需要压缩")
    }
    
    @Test
    fun testGetConversationStats() {
        val llmService = createMockLLMService()
        val conversationManager = ConversationManager(
            llmService = llmService,
            systemPrompt = "你是一个助手"
        )
        
        val stats = conversationManager.getConversationStats()
        
        assertNotNull(stats)
        assertEquals(1, stats.messageCount, "初始应该有 1 条系统消息")
        assertEquals(1000, stats.maxTokens, "最大 token 数应该是 1000")
        assertTrue(stats.utilizationRatio >= 0.0, "使用率应该非负")
    }
    
    @Test
    fun testCompressHistoryManually() = runTest {
        val llmService = createMockLLMService()
        val conversationManager = ConversationManager(
            llmService = llmService,
            systemPrompt = "你是一个助手"
        )

        // 添加一些对话历史
        conversationManager.addToolResults("第一个问题")  // 模拟用户输入
        conversationManager.addAssistantResponse("第一个回答")
        conversationManager.addToolResults("第二个问题")  // 模拟用户输入
        conversationManager.addAssistantResponse("第二个回答")

        // 手动压缩
        val result = conversationManager.compressHistory(force = true)

        assertNotNull(result)
        assertNotNull(result.info)
    }

    @Test
    fun testCallbackMechanisms() {
        val llmService = createMockLLMService()
        val conversationManager = ConversationManager(
            llmService = llmService,
            systemPrompt = "你是一个助手"
        )

        var tokenUpdateCalled = false
        var compressionNeededCalled = false
        var compressionCompletedCalled = false

        // 设置回调
        conversationManager.onTokenUpdate = { tokenInfo ->
            tokenUpdateCalled = true
            assertNotNull(tokenInfo)
        }

        conversationManager.onCompressionNeeded = { currentTokens, maxTokens ->
            compressionNeededCalled = true
            assertTrue(currentTokens > 0)
            assertTrue(maxTokens > 0)
        }

        conversationManager.onCompressionCompleted = { result ->
            compressionCompletedCalled = true
            assertNotNull(result)
        }

        // 验证回调设置成功
        assertNotNull(conversationManager.onTokenUpdate)
        assertNotNull(conversationManager.onCompressionNeeded)
        assertNotNull(conversationManager.onCompressionCompleted)
    }
}
