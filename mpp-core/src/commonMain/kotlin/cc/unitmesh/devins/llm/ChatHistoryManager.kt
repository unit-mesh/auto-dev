package cc.unitmesh.devins.llm

import cc.unitmesh.agent.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * - 提供 StateFlow 用于 UI 响应式更新
 * - 只保存有消息的会话（空会话不保存）
 */
class ChatHistoryManager {
    private val sessions = mutableMapOf<String, ChatSession>()
    private var currentSessionId: String? = null

    private val dispatcher = if (Platform.isWasm || Platform.isJs) Dispatchers.Main else Dispatchers.Default
    private val scope = CoroutineScope(dispatcher)
    private var initialized = false

    // 用于通知 UI 更新的 StateFlow
    private val _sessionsUpdateTrigger = MutableStateFlow(0)
    val sessionsUpdateTrigger: StateFlow<Int> = _sessionsUpdateTrigger.asStateFlow()

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
     * 保存所有会话到磁盘（同步版本）
     * 只保存有消息的会话
     * 用于需要立即保存的场景（如添加消息后）
     */
    private suspend fun saveSessions() {
        try {
            // 过滤掉空会话（没有消息的会话）
            val nonEmptySessions = sessions.values.filter { it.messages.isNotEmpty() }
            SessionStorage.saveSessions(nonEmptySessions)

            // 通知 UI 更新
            _sessionsUpdateTrigger.value++
        } catch (e: Exception) {
            println("⚠️ Failed to save sessions: ${e.message}")
        }
    }

    /**
     * 保存所有会话到磁盘（异步版本）
     * 用于不需要等待保存完成的场景（如切换会话、删除会话）
     */
    private fun saveSessionsAsync() {
        scope.launch {
            saveSessions()
        }
    }

    /**
     * 创建新会话
     * 注意：空会话不会被保存，只有添加消息后才会保存
     */
    @OptIn(ExperimentalUuidApi::class)
    fun createSession(): ChatSession {
        val sessionId = Uuid.random().toString()
        val session = ChatSession(id = sessionId)
        sessions[sessionId] = session
        currentSessionId = sessionId

        // 空会话不保存，等有消息时再保存
        // 但通知 UI 更新（虽然不会显示空会话）
        _sessionsUpdateTrigger.value++

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
     * 会先保存当前会话，再切换到新会话
     */
    fun switchSession(sessionId: String): ChatSession? {
        currentSessionId?.let { currentId ->
            sessions[currentId]?.let { currentSession ->
                if (currentSession.messages.isNotEmpty()) {
                    saveSessionsAsync()
                }
            }
        }

        return sessions[sessionId]?.also {
            currentSessionId = sessionId
            _sessionsUpdateTrigger.value++
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

        // 自动保存并通知 UI 更新
        saveSessionsAsync()
    }

    /**
     * 获取所有会话（包括空会话，以便用户可以看到新建的会话）
     */
    fun getAllSessions(): List<ChatSession> {
        return sessions.values
            .sortedByDescending { it.updatedAt }
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
     * 立即同步保存到磁盘，确保消息不会丢失
     */
    suspend fun addUserMessage(content: String) {
        getCurrentSession().addUserMessage(content)

        // 立即同步保存
        saveSessions()
    }

    /**
     * 添加助手消息到当前会话
     * 立即同步保存到磁盘，确保消息不会丢失
     */
    suspend fun addAssistantMessage(content: String) {
        getCurrentSession().addAssistantMessage(content)

        // 立即同步保存
        saveSessions()
    }

    /**
     * 重命名会话（添加标题）
     */
    fun renameSession(sessionId: String, title: String) {
        sessions[sessionId]?.let { session ->
            session.title = title
            session.updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            saveSessionsAsync()
        }
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

