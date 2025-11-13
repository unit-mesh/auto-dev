package cc.unitmesh.devins.llm

/**
 * Session 存储接口
 * 使用 expect/actual 模式实现跨平台文件 I/O
 * 
 * 存储位置：~/.autodev/sessions/chat-sessions.json
 */
expect object SessionStorage {
    /**
     * 加载所有本地 sessions
     */
    suspend fun loadSessions(): List<ChatSession>
    
    /**
     * 保存所有本地 sessions
     */
    suspend fun saveSessions(sessions: List<ChatSession>)
    
    /**
     * 获取存储路径
     */
    fun getStoragePath(): String
    
    /**
     * 检查存储文件是否存在
     */
    suspend fun exists(): Boolean
}

