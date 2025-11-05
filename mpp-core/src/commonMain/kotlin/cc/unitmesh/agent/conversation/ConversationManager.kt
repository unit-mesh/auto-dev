package cc.unitmesh.agent.conversation

import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.compression.CompressionResult
import cc.unitmesh.llm.compression.CompressionStatus
import cc.unitmesh.llm.compression.TokenInfo
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable

/**
 * 对话管理器 - 负责管理 Agent 与 LLM 的多轮对话
 * 
 * 职责：
 * 1. 维护对话历史
 * 2. 管理 system prompt 和 user prompt
 * 3. 处理流式响应
 * 4. 支持对话上下文管理
 * 5. 自动上下文压缩
 */
class ConversationManager(
    private val llmService: KoogLLMService,
    private val systemPrompt: String,
    private val autoCompress: Boolean = true
) {
    private val conversationHistory = mutableListOf<Message>()
    
    // 压缩相关回调
    var onTokenUpdate: ((TokenInfo) -> Unit)? = null
    var onCompressionNeeded: ((currentTokens: Int, maxTokens: Int) -> Unit)? = null
    var onCompressionCompleted: ((CompressionResult) -> Unit)? = null
    
    init {
        // 添加系统消息作为对话的开始
        conversationHistory.add(Message(MessageRole.SYSTEM, systemPrompt))
    }
    
    /**
     * 发送用户消息并获取流式响应
     * 
     * @param userMessage 用户消息内容
     * @return 流式响应
     */
    suspend fun sendMessage(userMessage: String): Flow<String> {
        // 添加用户消息到历史
        conversationHistory.add(Message(MessageRole.USER, userMessage))
        
        // 检查是否需要自动压缩
        if (autoCompress && needsCompression()) {
            tryAutoCompress()
        }
        
        // 调用 LLM 服务，传入完整的对话历史
        return llmService.streamPrompt(
            userPrompt = userMessage,
            fileSystem = EmptyFileSystem(),
            historyMessages = conversationHistory.dropLast(1), // 排除当前用户消息，因为它会在 buildPrompt 中添加
            compileDevIns = false, // Agent 自己处理 DevIns
            onTokenUpdate = { tokenInfo ->
                onTokenUpdate?.invoke(tokenInfo)
            },
            onCompressionNeeded = { current, max ->
                onCompressionNeeded?.invoke(current, max)
            }
        ).cancellable()
    }
    
    /**
     * 添加助手响应到对话历史
     * 
     * @param assistantResponse 助手的完整响应
     */
    fun addAssistantResponse(assistantResponse: String) {
        conversationHistory.add(Message(MessageRole.ASSISTANT, assistantResponse))
    }
    
    /**
     * 添加工具执行结果作为用户消息
     * 
     * @param toolResults 工具执行结果的描述
     */
    fun addToolResults(toolResults: String) {
        conversationHistory.add(Message(MessageRole.USER, toolResults))
    }
    
    /**
     * 获取对话历史
     */
    fun getHistory(): List<Message> = conversationHistory.toList()
    
    /**
     * 清空对话历史（保留系统消息）
     */
    fun clearHistory() {
        val systemMessage = conversationHistory.firstOrNull { it.role == MessageRole.SYSTEM }
        conversationHistory.clear()
        systemMessage?.let { conversationHistory.add(it) }
    }
    
    /**
     * 获取最近的对话历史（用于上下文限制）
     * 
     * @param maxMessages 最大消息数量
     */
    fun getRecentHistory(maxMessages: Int): List<Message> {
        val systemMessage = conversationHistory.firstOrNull { it.role == MessageRole.SYSTEM }
        val otherMessages = conversationHistory.filter { it.role != MessageRole.SYSTEM }
        
        val recentMessages = otherMessages.takeLast(maxMessages - 1)
        
        return if (systemMessage != null) {
            listOf(systemMessage) + recentMessages
        } else {
            recentMessages
        }
    }
    
    /**
     * 更新系统提示词
     * 
     * @param newSystemPrompt 新的系统提示词
     */
    fun updateSystemPrompt(newSystemPrompt: String) {
        // 移除旧的系统消息
        conversationHistory.removeAll { it.role == MessageRole.SYSTEM }
        // 添加新的系统消息到开头
        conversationHistory.add(0, Message(MessageRole.SYSTEM, newSystemPrompt))
    }
    
    /**
     * 检查是否需要压缩
     */
    fun needsCompression(): Boolean {
        val tokenInfo = llmService.getLastTokenInfo()
        val maxTokens = llmService.getMaxTokens()
        return tokenInfo.needsCompression(maxTokens, 0.7)
    }
    
    /**
     * 手动压缩历史
     * 
     * @param force 是否强制压缩
     * @return 压缩结果
     */
    suspend fun compressHistory(force: Boolean = false): CompressionResult {
        val result = llmService.tryCompressHistory(conversationHistory, force)
        
        // 如果压缩成功，更新对话历史
        if (result.info.compressionStatus == CompressionStatus.COMPRESSED && result.newMessages != null) {
            conversationHistory.clear()
            conversationHistory.addAll(result.newMessages)
            onCompressionCompleted?.invoke(result)
        }
        
        return result
    }
    
    /**
     * 尝试自动压缩
     */
    private suspend fun tryAutoCompress() {
        val result = llmService.tryCompressHistory(conversationHistory, force = false)
        
        if (result.info.compressionStatus == CompressionStatus.COMPRESSED && result.newMessages != null) {
            conversationHistory.clear()
            conversationHistory.addAll(result.newMessages)
            onCompressionCompleted?.invoke(result)
        }
    }
    
    /**
     * 获取对话统计信息
     */
    data class ConversationStats(
        val messageCount: Int,
        val tokenInfo: TokenInfo,
        val maxTokens: Int,
        val utilizationRatio: Double
    )
    
    fun getConversationStats(): ConversationStats {
        val tokenInfo = llmService.getLastTokenInfo()
        val maxTokens = llmService.getMaxTokens()
        val utilizationRatio = if (maxTokens > 0) {
            tokenInfo.inputTokens.toDouble() / maxTokens.toDouble()
        } else {
            0.0
        }
        
        return ConversationStats(
            messageCount = conversationHistory.size,
            tokenInfo = tokenInfo,
            maxTokens = maxTokens,
            utilizationRatio = utilizationRatio
        )
    }
}
