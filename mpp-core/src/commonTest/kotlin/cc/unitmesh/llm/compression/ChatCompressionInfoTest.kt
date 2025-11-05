package cc.unitmesh.llm.compression

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatCompressionInfoTest {
    
    @Test
    fun testCompressionRatio() {
        // 正常压缩情况
        val info1 = ChatCompressionInfo(
            originalTokenCount = 1000,
            newTokenCount = 300,
            compressionStatus = CompressionStatus.COMPRESSED
        )
        assertEquals(0.7, info1.compressionRatio, "压缩比例应为 70%")
        
        // 无压缩情况
        val info2 = ChatCompressionInfo(
            originalTokenCount = 1000,
            newTokenCount = 1000,
            compressionStatus = CompressionStatus.NOOP
        )
        assertEquals(0.0, info2.compressionRatio, "无压缩时比例应为 0%")
        
        // 原始 token 为 0 的边界情况
        val info3 = ChatCompressionInfo(
            originalTokenCount = 0,
            newTokenCount = 0,
            compressionStatus = CompressionStatus.NOOP
        )
        assertEquals(0.0, info3.compressionRatio, "原始 token 为 0 时比例应为 0%")
    }
    
    @Test
    fun testTokensSaved() {
        val info = ChatCompressionInfo(
            originalTokenCount = 1000,
            newTokenCount = 300,
            compressionStatus = CompressionStatus.COMPRESSED
        )
        assertEquals(700, info.tokensSaved, "节省的 token 数应为 700")
        
        // 无压缩情况
        val info2 = ChatCompressionInfo(
            originalTokenCount = 1000,
            newTokenCount = 1000,
            compressionStatus = CompressionStatus.NOOP
        )
        assertEquals(0, info2.tokensSaved, "无压缩时节省的 token 应为 0")
        
        // 压缩失败导致 token 增加的情况
        val info3 = ChatCompressionInfo(
            originalTokenCount = 1000,
            newTokenCount = 1200,
            compressionStatus = CompressionStatus.COMPRESSION_FAILED_INFLATED_TOKEN_COUNT
        )
        assertEquals(-200, info3.tokensSaved, "token 增加时节省数应为负数")
    }
    
    @Test
    fun testWithErrorMessage() {
        val info = ChatCompressionInfo(
            originalTokenCount = 1000,
            newTokenCount = 1000,
            compressionStatus = CompressionStatus.COMPRESSION_FAILED_ERROR,
            errorMessage = "模拟错误"
        )
        assertEquals("模拟错误", info.errorMessage, "错误消息应该正确保存")
    }
    
    @Test
    fun testAllCompressionStatuses() {
        val statuses = listOf(
            CompressionStatus.COMPRESSED,
            CompressionStatus.COMPRESSION_FAILED_INFLATED_TOKEN_COUNT,
            CompressionStatus.COMPRESSION_FAILED_TOKEN_COUNT_ERROR,
            CompressionStatus.NOOP,
            CompressionStatus.COMPRESSION_FAILED_ERROR
        )
        
        statuses.forEach { status ->
            val info = ChatCompressionInfo(
                originalTokenCount = 1000,
                newTokenCount = 500,
                compressionStatus = status
            )
            assertEquals(status, info.compressionStatus, "压缩状态应该正确设置")
        }
    }
}
