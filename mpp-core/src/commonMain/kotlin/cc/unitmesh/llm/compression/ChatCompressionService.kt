package cc.unitmesh.llm.compression

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import kotlinx.serialization.json.Json

/**
 * 聊天上下文压缩服务
 * 
 * 负责将长对话历史压缩为简洁的状态快照，保留关键信息同时大幅减少 token 使用量
 */
class ChatCompressionService(
    private val executor: SingleLLMPromptExecutor,
    private val model: LLModel,
    private val config: CompressionConfig = CompressionConfig()
) {
    
    /**
     * 压缩对话历史
     * 
     * @param messages 完整的对话历史
     * @param currentTokenCount 当前使用的 token 数量
     * @param maxTokens 模型的最大 token 限制
     * @param force 是否强制压缩（忽略阈值检查）
     * @return 压缩结果
     */
    suspend fun compress(
        messages: List<Message>,
        currentTokenCount: Int,
        maxTokens: Int,
        force: Boolean = false
    ): CompressionResult {
        // 1. 检查是否需要压缩
        if (messages.isEmpty()) {
            return CompressionResult(
                newMessages = null,
                info = ChatCompressionInfo(
                    originalTokenCount = 0,
                    newTokenCount = 0,
                    compressionStatus = CompressionStatus.NOOP
                )
            )
        }
        
        // 2. 如果不是强制压缩，检查阈值
        if (!force) {
            val threshold = config.contextPercentageThreshold
            if (currentTokenCount < threshold * maxTokens) {
                return CompressionResult(
                    newMessages = null,
                    info = ChatCompressionInfo(
                        originalTokenCount = currentTokenCount,
                        newTokenCount = currentTokenCount,
                        compressionStatus = CompressionStatus.NOOP
                    )
                )
            }
        }
        
        // 3. 找到分割点
        val splitPoint = findCompressSplitPoint(messages, 1 - config.preserveRecentRatio)
        
        val messagesToCompress = messages.subList(0, splitPoint)
        val messagesToKeep = messages.subList(splitPoint, messages.size)
        
        // 4. 如果没有可压缩的消息
        if (messagesToCompress.isEmpty()) {
            return CompressionResult(
                newMessages = null,
                info = ChatCompressionInfo(
                    originalTokenCount = currentTokenCount,
                    newTokenCount = currentTokenCount,
                    compressionStatus = CompressionStatus.NOOP
                )
            )
        }
        
        // 5. 使用 LLM 生成摘要
        val summary = try {
            generateSummary(messagesToCompress)
        } catch (e: Exception) {
            return CompressionResult(
                newMessages = null,
                info = ChatCompressionInfo(
                    originalTokenCount = currentTokenCount,
                    newTokenCount = currentTokenCount,
                    compressionStatus = CompressionStatus.COMPRESSION_FAILED_ERROR,
                    errorMessage = e.message
                )
            )
        }
        
        // 6. 构建新的历史
        val compressedMessages = buildList {
            // 添加摘要作为用户消息
            add(Message(MessageRole.USER, summary))
            // 添加模型确认
            add(Message(MessageRole.ASSISTANT, CompressionPrompts.getCompressionAcknowledgment()))
            // 添加保留的最近消息
            addAll(messagesToKeep)
        }
        
        // 7. 估算新的 token 数量（粗略估计：4 字符 ≈ 1 token）
        val newTokenCount = estimateTokenCount(compressedMessages)
        
        // 8. 检查压缩是否有效（防止 token 膨胀）
        if (newTokenCount > currentTokenCount) {
            return CompressionResult(
                newMessages = null,
                info = ChatCompressionInfo(
                    originalTokenCount = currentTokenCount,
                    newTokenCount = newTokenCount,
                    compressionStatus = CompressionStatus.COMPRESSION_FAILED_INFLATED_TOKEN_COUNT,
                    errorMessage = "压缩后 token 数量反而增加了"
                )
            )
        }
        
        // 9. 压缩成功
        return CompressionResult(
            newMessages = compressedMessages,
            info = ChatCompressionInfo(
                originalTokenCount = currentTokenCount,
                newTokenCount = newTokenCount,
                compressionStatus = CompressionStatus.COMPRESSED
            )
        )
    }
    
    /**
     * 找到压缩分割点
     * 
     * 规则：
     * 1. 只在用户消息处分割
     * 2. 不在包含工具响应的消息处分割
     * 3. 确保保留足够的最近上下文
     */
    private fun findCompressSplitPoint(messages: List<Message>, compressFraction: Double): Int {
        if (compressFraction <= 0 || compressFraction >= 1) {
            throw IllegalArgumentException("Fraction must be between 0 and 1")
        }
        
        // 计算每条消息的字符数
        val charCounts = messages.map { Json.encodeToString(Message.serializer(), it).length }
        val totalCharCount = charCounts.sum()
        val targetCharCount = (totalCharCount * compressFraction).toInt()
        
        var lastSplitPoint = 0
        var cumulativeCharCount = 0
        
        for (i in messages.indices) {
            val message = messages[i]
            
            // 只在用户消息处分割
            if (message.role == MessageRole.USER) {
                if (cumulativeCharCount >= targetCharCount) {
                    return i
                }
                lastSplitPoint = i
            }
            
            cumulativeCharCount += charCounts[i]
        }
        
        // 如果没有找到合适的分割点，检查是否可以压缩所有内容
        val lastMessage = messages.lastOrNull()
        if (lastMessage?.role == MessageRole.ASSISTANT) {
            return messages.size
        }
        
        // 返回最后一个有效分割点
        return lastSplitPoint
    }
    
    /**
     * 生成对话历史的摘要
     */
    private suspend fun generateSummary(messages: List<Message>): String {
        // 构建压缩提示词
        val compressionPrompt = buildString {
            append(CompressionPrompts.getCompressionSystemPrompt())
            append("\n\n")
            
            // 添加要压缩的历史消息
            messages.forEach { message ->
                append("\n[${message.role}]: ${message.content}\n")
            }
            
            append("\n\n")
            append(CompressionPrompts.getCompressionUserPrompt())
        }
        
        // 使用 AIAgent 执行压缩
        val agent = AIAgent(
            promptExecutor = executor, 
            llmModel = model
        )
        
        return agent.run(compressionPrompt)
    }
    
    /**
     * 估算消息列表的 token 数量
     * 
     * 粗略估计：4 字符 ≈ 1 token
     */
    private fun estimateTokenCount(messages: List<Message>): Int {
        val totalChars = messages.sumOf { 
            Json.encodeToString(Message.serializer(), it).length 
        }
        return totalChars / 4
    }
}

