package cc.unitmesh.devins.llm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 聊天历史管理器
 * 管理多个聊天会话
 * 
 * 功能增强：
 * - 自动持久化到磁盘（~/.autodev/sessions/chat-sessions.json）
 * - 启动时自动加载历史会话
 * - 保持现有 API 完全兼容
 */
class ChatHistoryManager {
    private val sessions = mutableMapOf<String, ChatSession>()
    private var currentSessionId: String? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var initialized = false
    
    /**
     * 初始化：从磁盘加载历史会话
     */
    suspend fun initialize() {
        if (initialized) return
        
        try {
            val loadedSessions = SessionStorage.loadSessions()
            loadedSessions.forEach { session ->
                sessions[session.id] = session
            }
            
            // 如果有会话，设置最新的为当前会话
            if (sessions.isNotEmpty()) {
                currentSessionId = sessions.values.maxByOrNull { it.updatedAt }?.id
            }
            
            println("✅ Loaded ${sessions.size} chat sessions from disk")
            initialized = true
        } catch (e: Exception) {
            println("⚠️ Failed to initialize ChatHistoryManager: ${e.message}")
            initialized = true
        }
    }
    
    /**
     * 保存所有会话到磁盘
     */
    private fun saveSessionsAsync() {
        scope.launch {
            try {
                val sessionsList = sessions.values.toList()
                SessionStorage.saveSessions(sessionsList)
            } catch (e: Exception) {
                println("⚠️ Failed to save sessions: ${e.message}")
            }
        }
    }
    
    /**
     * 创建新会话
     */
    @OptIn(ExperimentalUuidApi::class)
    fun createSession(): ChatSession {
        val sessionId = Uuid.random().toString()
        val session = ChatSession(id = sessionId)
        sessions[sessionId] = session
        currentSessionId = sessionId
        
        // 自动保存
        saveSessionsAsync()
        
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
        
        // 自动保存
        saveSessionsAsync()
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
        
        // 自动保存
        saveSessionsAsync()
    }
    
    /**
     * 添加用户消息到当前会话
     */
    fun addUserMessage(content: String) {
        getCurrentSession().addUserMessage(content)
        
        // 自动保存
        saveSessionsAsync()
    }
    
    /**
     * 添加助手消息到当前会话
     */
    fun addAssistantMessage(content: String) {
        getCurrentSession().addAssistantMessage(content)
        
        // 自动保存
        saveSessionsAsync()
    }
    
    /**
     * 重命名会话（添加标题）
     */
    fun renameSession(sessionId: String, title: String) {
        // ChatSession 目前没有 title 字段，可以通过第一条消息推断
        // 这里暂时不实现，SessionSidebar 会显示第一条消息的摘要
        saveSessionsAsync()
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

