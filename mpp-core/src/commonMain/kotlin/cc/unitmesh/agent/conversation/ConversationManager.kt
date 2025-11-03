package cc.unitmesh.agent.conversation

import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.llm.KoogLLMService
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
 */
class ConversationManager(
    private val llmService: KoogLLMService,
    private val systemPrompt: String
) {
    private val conversationHistory = mutableListOf<Message>()
    
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
        
        // 调用 LLM 服务，传入完整的对话历史
        return llmService.streamPrompt(
            userPrompt = userMessage,
            fileSystem = EmptyFileSystem(),
            historyMessages = conversationHistory.dropLast(1), // 排除当前用户消息，因为它会在 buildPrompt 中添加
            compileDevIns = false // Agent 自己处理 DevIns
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
}
