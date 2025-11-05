package cc.unitmesh.llm

import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.llm.compression.CompressionConfig
import cc.unitmesh.llm.compression.CompressionStatus
import cc.unitmesh.llm.compression.TokenInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KoogLLMServiceCompressionTest {
    
    private fun createMockModelConfig(): ModelConfig {
        return ModelConfig(
            provider = LLMProviderType.OPENAI,
            modelName = "gpt-3.5-turbo",
            apiKey = "mock-api-key",
            baseUrl = "https://api.openai.com/v1",
            maxTokens = 1000
        )
    }
    
    @Test
    fun testCompressionConfigInitialization() {
        val compressionConfig = CompressionConfig(
            contextPercentageThreshold = 0.8,
            autoCompressionEnabled = true
        )
        
        val llmService = KoogLLMService.create(
            config = createMockModelConfig(),
            compressionConfig = compressionConfig
        )
        
        // 验证服务创建成功
        assertNotNull(llmService)
        assertEquals(1000, llmService.getMaxTokens())
    }
    
    @Test
    fun testDefaultCompressionConfig() {
        val llmService = KoogLLMService.create(createMockModelConfig())
        
        // 验证默认配置
        assertNotNull(llmService)
        assertEquals(1000, llmService.getMaxTokens())
    }
    
    @Test
    fun testGetLastTokenInfo() {
        val llmService = KoogLLMService.create(createMockModelConfig())
        
        // 初始状态应该是空的 TokenInfo
        val initialTokenInfo = llmService.getLastTokenInfo()
        assertEquals(0, initialTokenInfo.totalTokens)
        assertEquals(0, initialTokenInfo.inputTokens)
        assertEquals(0, initialTokenInfo.outputTokens)
    }
    
    @Test
    fun testResetCompressionState() {
        val llmService = KoogLLMService.create(createMockModelConfig())
        
        // 重置压缩状态不应该抛出异常
        llmService.resetCompressionState()
        
        // 验证状态重置后的初始状态
        val tokenInfo = llmService.getLastTokenInfo()
        assertEquals(0, tokenInfo.totalTokens)
    }
    
    @Test
    fun testTryCompressHistoryWithEmptyMessages() = runTest {
        val llmService = KoogLLMService.create(createMockModelConfig())
        
        val result = llmService.tryCompressHistory(
            historyMessages = emptyList(),
            force = false
        )
        
        assertEquals(CompressionStatus.NOOP, result.info.compressionStatus)
        assertEquals(0, result.info.originalTokenCount)
        assertEquals(0, result.info.newTokenCount)
    }
    
    @Test
    fun testTryCompressHistoryWithForce() = runTest {
        val llmService = KoogLLMService.create(createMockModelConfig())
        
        val messages = listOf(
            Message(MessageRole.SYSTEM, "你是一个助手"),
            Message(MessageRole.USER, "测试问题"),
            Message(MessageRole.ASSISTANT, "测试回答")
        )
        
        val result = llmService.tryCompressHistory(
            historyMessages = messages,
            force = true
        )
        
        // 由于是 Mock 环境，压缩可能会失败，但不应该抛出异常
        assertNotNull(result)
        assertNotNull(result.info)
    }
}
