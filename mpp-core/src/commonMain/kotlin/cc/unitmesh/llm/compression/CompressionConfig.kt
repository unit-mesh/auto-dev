package cc.unitmesh.llm.compression

import kotlinx.serialization.Serializable

/**
 * 压缩配置
 */
@Serializable
data class CompressionConfig(
    /**
     * 触发压缩的上下文使用百分比阈值
     * 默认：0.7 (70%)
     */
    val contextPercentageThreshold: Double = 0.7,
    
    /**
     * 保留最近对话的比例
     * 默认：0.3 (30%)
     */
    val preserveRecentRatio: Double = 0.3,
    
    /**
     * 是否启用自动压缩
     * 默认：true
     */
    val autoCompressionEnabled: Boolean = true,
    
    /**
     * 压缩失败后等待的消息数量再重试
     * 默认：5
     */
    val retryAfterMessages: Int = 5
) {
    init {
        require(contextPercentageThreshold in 0.0..1.0) {
            "contextPercentageThreshold must be between 0.0 and 1.0"
        }
        require(preserveRecentRatio in 0.0..1.0) {
            "preserveRecentRatio must be between 0.0 and 1.0"
        }
        require(retryAfterMessages > 0) {
            "retryAfterMessages must be positive"
        }
    }
}

/**
 * 压缩状态
 */
enum class CompressionStatus {
    /** 压缩成功 */
    COMPRESSED,
    
    /** 压缩失败：token 反而膨胀 */
    COMPRESSION_FAILED_INFLATED_TOKEN_COUNT,
    
    /** 压缩失败：token 计数错误 */
    COMPRESSION_FAILED_TOKEN_COUNT_ERROR,
    
    /** 无需压缩 */
    NOOP,
    
    /** 压缩失败：其他错误 */
    COMPRESSION_FAILED_ERROR
}

/**
 * Token 信息
 */
@Serializable
data class TokenInfo(
    /** 总 token 数 */
    val totalTokens: Int = 0,
    
    /** 输入 token 数 */
    val inputTokens: Int = 0,
    
    /** 输出 token 数 */
    val outputTokens: Int = 0,
    
    /** 时间戳 */
    val timestamp: Long = 0
) {
    /**
     * 计算使用率百分比
     */
    fun getUsagePercentage(maxTokens: Int): Double {
        if (maxTokens <= 0) return 0.0
        return (inputTokens.toDouble() / maxTokens.toDouble()) * 100.0
    }
    
    /**
     * 检查是否需要压缩
     */
    fun needsCompression(maxTokens: Int, threshold: Double): Boolean {
        if (maxTokens <= 0) return false
        val usage = inputTokens.toDouble() / maxTokens.toDouble()
        return usage >= threshold
    }
}

/**
 * 压缩结果信息
 */
@Serializable
data class ChatCompressionInfo(
    /** 原始 token 数量 */
    val originalTokenCount: Int,
    
    /** 压缩后 token 数量 */
    val newTokenCount: Int,
    
    /** 压缩状态 */
    val compressionStatus: CompressionStatus,
    
    /** 错误消息（如果有） */
    val errorMessage: String? = null
) {
    /**
     * 压缩比例（0-1）
     */
    val compressionRatio: Double
        get() = if (originalTokenCount > 0) {
            1.0 - (newTokenCount.toDouble() / originalTokenCount.toDouble())
        } else {
            0.0
        }
    
    /**
     * 节省的 token 数量
     */
    val tokensSaved: Int
        get() = originalTokenCount - newTokenCount
}

/**
 * 压缩结果
 */
data class CompressionResult(
    /** 压缩后的新消息列表（null 表示未压缩） */
    val newMessages: List<cc.unitmesh.devins.llm.Message>?,
    
    /** 压缩信息 */
    val info: ChatCompressionInfo
)

