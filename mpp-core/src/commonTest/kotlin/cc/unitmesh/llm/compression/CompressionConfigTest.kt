package cc.unitmesh.llm.compression

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompressionConfigTest {
    
    @Test
    fun testDefaultValues() {
        val config = CompressionConfig()
        
        assertEquals(0.7, config.contextPercentageThreshold, "默认上下文阈值应为 70%")
        assertEquals(0.3, config.preserveRecentRatio, "默认保留比例应为 30%")
        assertTrue(config.autoCompressionEnabled, "默认应启用自动压缩")
        assertEquals(5, config.retryAfterMessages, "默认重试间隔应为 5 条消息")
    }
    
    @Test
    fun testValidConfiguration() {
        val config = CompressionConfig(
            contextPercentageThreshold = 0.8,
            preserveRecentRatio = 0.2,
            autoCompressionEnabled = false,
            retryAfterMessages = 10
        )
        
        assertEquals(0.8, config.contextPercentageThreshold)
        assertEquals(0.2, config.preserveRecentRatio)
        assertEquals(false, config.autoCompressionEnabled)
        assertEquals(10, config.retryAfterMessages)
    }
    
    @Test
    fun testInvalidContextPercentageThreshold() {
        // 测试负值
        assertFailsWith<IllegalArgumentException> {
            CompressionConfig(contextPercentageThreshold = -0.1)
        }
        
        // 测试超过 1.0
        assertFailsWith<IllegalArgumentException> {
            CompressionConfig(contextPercentageThreshold = 1.1)
        }
        
        // 测试边界值 0.0 和 1.0 应该有效
        CompressionConfig(contextPercentageThreshold = 0.0)
        CompressionConfig(contextPercentageThreshold = 1.0)
    }
    
    @Test
    fun testInvalidPreserveRecentRatio() {
        // 测试负值
        assertFailsWith<IllegalArgumentException> {
            CompressionConfig(preserveRecentRatio = -0.1)
        }
        
        // 测试超过 1.0
        assertFailsWith<IllegalArgumentException> {
            CompressionConfig(preserveRecentRatio = 1.1)
        }
        
        // 测试边界值 0.0 和 1.0 应该有效
        CompressionConfig(preserveRecentRatio = 0.0)
        CompressionConfig(preserveRecentRatio = 1.0)
    }
    
    @Test
    fun testInvalidRetryAfterMessages() {
        // 测试零值
        assertFailsWith<IllegalArgumentException> {
            CompressionConfig(retryAfterMessages = 0)
        }
        
        // 测试负值
        assertFailsWith<IllegalArgumentException> {
            CompressionConfig(retryAfterMessages = -1)
        }
        
        // 测试正值应该有效
        CompressionConfig(retryAfterMessages = 1)
    }
}
