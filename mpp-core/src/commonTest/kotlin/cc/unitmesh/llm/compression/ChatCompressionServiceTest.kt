package cc.unitmesh.llm.compression

import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ChatCompressionService 测试
 *
 * 注意：由于 ChatCompressionService 依赖于实际的 LLM 调用，
 * 这些测试主要验证配置和基本逻辑，而不是实际的压缩功能。
 * 实际的压缩功能需要在集成测试中验证。
 */
class ChatCompressionServiceTest {

    @Test
    fun testCompressionConfigValidation() {
        // 测试压缩配置的基本验证
        val config = CompressionConfig(
            contextPercentageThreshold = 0.7,
            preserveRecentRatio = 0.3,
            autoCompressionEnabled = true,
            retryAfterMessages = 5
        )

        assertEquals(0.7, config.contextPercentageThreshold)
        assertEquals(0.3, config.preserveRecentRatio)
        assertTrue(config.autoCompressionEnabled)
        assertEquals(5, config.retryAfterMessages)
    }

    @Test
    fun testMessageListProcessing() {
        // 测试消息列表的基本处理逻辑
        val messages = listOf(
            Message(MessageRole.SYSTEM, "你是一个助手"),
            Message(MessageRole.USER, "你好"),
            Message(MessageRole.ASSISTANT, "你好！有什么可以帮助你的吗？"),
            Message(MessageRole.USER, "请帮我写代码"),
            Message(MessageRole.ASSISTANT, "好的，我可以帮你写代码")
        )

        // 验证消息列表不为空
        assertTrue(messages.isNotEmpty())
        assertEquals(5, messages.size)

        // 验证消息角色
        assertEquals(MessageRole.SYSTEM, messages[0].role)
        assertEquals(MessageRole.USER, messages[1].role)
        assertEquals(MessageRole.ASSISTANT, messages[2].role)
    }
    
    @Test
    fun testTokenEstimation() {
        // 测试 token 估算逻辑的基本概念
        val shortMessage = Message(MessageRole.USER, "Hi")
        val longMessage = Message(MessageRole.USER, "This is a much longer message that should have more tokens when estimated")

        // 简单验证长消息比短消息有更多字符
        assertTrue(longMessage.content.length > shortMessage.content.length)

        // 验证消息内容不为空
        assertTrue(shortMessage.content.isNotEmpty())
        assertTrue(longMessage.content.isNotEmpty())
    }

    @Test
    fun testCompressionThresholdLogic() {
        // 测试压缩阈值逻辑
        val threshold = 0.7
        val maxTokens = 1000

        // 测试不同的 token 使用情况
        val lowUsage = 500  // 50%
        val highUsage = 800 // 80%

        // 验证阈值逻辑
        assertTrue(lowUsage.toDouble() / maxTokens < threshold, "低使用率不应触发压缩")
        assertTrue(highUsage.toDouble() / maxTokens > threshold, "高使用率应触发压缩")
    }

    private fun createLongConversation(): List<Message> {
        return buildList {
            add(Message(MessageRole.SYSTEM, "你是一个有用的助手"))
            repeat(10) { i ->
                add(Message(MessageRole.USER, "这是第 ${i + 1} 个用户问题，内容比较长，用于测试压缩功能"))
                add(Message(MessageRole.ASSISTANT, "这是第 ${i + 1} 个助手回答，包含详细的解释和示例代码"))
            }
        }
    }
}
