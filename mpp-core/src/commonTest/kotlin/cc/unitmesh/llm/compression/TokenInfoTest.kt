package cc.unitmesh.llm.compression

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenInfoTest {
    
    @Test
    fun testDefaultValues() {
        val tokenInfo = TokenInfo()
        
        assertEquals(0, tokenInfo.totalTokens)
        assertEquals(0, tokenInfo.inputTokens)
        assertEquals(0, tokenInfo.outputTokens)
        assertEquals(0, tokenInfo.timestamp)
    }
    
    @Test
    fun testCustomValues() {
        val tokenInfo = TokenInfo(
            totalTokens = 1000,
            inputTokens = 600,
            outputTokens = 400,
            timestamp = 1234567890L
        )
        
        assertEquals(1000, tokenInfo.totalTokens)
        assertEquals(600, tokenInfo.inputTokens)
        assertEquals(400, tokenInfo.outputTokens)
        assertEquals(1234567890L, tokenInfo.timestamp)
    }
    
    @Test
    fun testGetUsagePercentage() {
        val tokenInfo = TokenInfo(inputTokens = 750)
        
        // 正常情况
        assertEquals(75.0, tokenInfo.getUsagePercentage(1000), "750/1000 应该是 75%")
        assertEquals(50.0, tokenInfo.getUsagePercentage(1500), "750/1500 应该是 50%")
        
        // 边界情况：maxTokens = 0
        assertEquals(0.0, tokenInfo.getUsagePercentage(0), "maxTokens=0 时应返回 0%")
        
        // 边界情况：负数 maxTokens
        assertEquals(0.0, tokenInfo.getUsagePercentage(-100), "负数 maxTokens 应返回 0%")
        
        // 超过 100% 的情况
        assertEquals(150.0, tokenInfo.getUsagePercentage(500), "750/500 应该是 150%")
    }
    
    @Test
    fun testNeedsCompression() {
        val tokenInfo = TokenInfo(inputTokens = 700)
        
        // 需要压缩的情况（70% 阈值）
        assertTrue(tokenInfo.needsCompression(1000, 0.7), "700/1000 = 70% 应该触发压缩")
        assertTrue(tokenInfo.needsCompression(900, 0.7), "700/900 = 77.8% 应该触发压缩")
        
        // 不需要压缩的情况
        assertFalse(tokenInfo.needsCompression(1100, 0.7), "700/1100 = 63.6% 不应该触发压缩")
        assertFalse(tokenInfo.needsCompression(1000, 0.8), "700/1000 = 70% < 80% 不应该触发压缩")
        
        // 边界情况：maxTokens = 0
        assertFalse(tokenInfo.needsCompression(0, 0.7), "maxTokens=0 时不应该触发压缩")
        
        // 边界情况：负数 maxTokens
        assertFalse(tokenInfo.needsCompression(-100, 0.7), "负数 maxTokens 不应该触发压缩")
        
        // 精确阈值测试
        val exactTokenInfo = TokenInfo(inputTokens = 700)
        assertTrue(exactTokenInfo.needsCompression(1000, 0.7), "正好达到阈值应该触发压缩")
        assertFalse(exactTokenInfo.needsCompression(1000, 0.701), "略高于阈值不应该触发压缩")
    }
    
    @Test
    fun testZeroInputTokens() {
        val tokenInfo = TokenInfo(inputTokens = 0)
        
        assertEquals(0.0, tokenInfo.getUsagePercentage(1000), "0 tokens 使用率应为 0%")
        assertFalse(tokenInfo.needsCompression(1000, 0.7), "0 tokens 不应该触发压缩")
    }
}
