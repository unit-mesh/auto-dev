package cc.unitmesh.devins.llm

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 聊天历史管理器
 * 管理多个聊天会话
 */
class ChatHistoryManager {
    private val sessions = mutableMapOf<String, ChatSession>()
    private var currentSessionId: String? = null
    
    /**
     * 创建新会话
     */
    @OptIn(ExperimentalUuidApi::class)
    fun createSession(): ChatSession {
        val sessionId = Uuid.random().toString()
        val session = ChatSession(id = sessionId)
        sessions[sessionId] = session
        currentSessionId = sessionId
        return session
    }
    
    /**
     * 获取当前会话
     */
    fun getCurrentSession(): ChatSession {
        return currentSessionId?.let { sessions[it] } 
            ?: createSession()
    }
    
    /**
     * 切换到指定会话
     */
    fun switchSession(sessionId: String): ChatSession? {
        return sessions[sessionId]?.also {
            currentSessionId = sessionId
        }
    }
    
    /**
     * 删除会话
     */
    fun deleteSession(sessionId: String) {
        sessions.remove(sessionId)
        if (currentSessionId == sessionId) {
            currentSessionId = null
        }
    }
    
    /**
     * 获取所有会话
     */
    fun getAllSessions(): List<ChatSession> {
        return sessions.values.sortedByDescending { it.updatedAt }
    }
    
    /**
     * 清空当前会话历史
     */
    fun clearCurrentSession() {
        getCurrentSession().clear()
    }
    
    /**
     * 添加用户消息到当前会话
     */
    fun addUserMessage(content: String) {
        getCurrentSession().addUserMessage(content)
    }
    
    /**
     * 添加助手消息到当前会话
     */
    fun addAssistantMessage(content: String) {
        getCurrentSession().addAssistantMessage(content)
    }
    
    /**
     * 获取当前会话的消息历史
     */
    fun getMessages(): List<Message> {
        return getCurrentSession().messages
    }
    
    /**
     * 获取当前会话的最近 N 条消息
     */
    fun getRecentMessages(count: Int): List<Message> {
        return getCurrentSession().getRecentMessages(count)
    }
    
    companion object {
        private var instance: ChatHistoryManager? = null

        /**
         * 获取全局单例
         */
        fun getInstance(): ChatHistoryManager {
            return instance ?: run {
                val newInstance = ChatHistoryManager()
                instance = newInstance
                newInstance
            }
        }
    }
}

