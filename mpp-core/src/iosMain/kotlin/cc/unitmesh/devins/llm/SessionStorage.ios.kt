package cc.unitmesh.devins.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS implementation of SessionStorage
 * Uses in-memory storage for simplicity
 * 
 * For production use, this could be enhanced to use:
 * - UserDefaults for small data
 * - FileManager for file-based storage
 * - Core Data or Realm for database storage
 */
actual object SessionStorage {
    private var memoryCache: List<ChatSession> = emptyList()
    
    /**
     * 加载所有本地 sessions
     * iOS: 使用内存缓存
     */
    actual suspend fun loadSessions(): List<ChatSession> = withContext(Dispatchers.Default) {
        memoryCache
    }
    
    /**
     * 保存所有本地 sessions
     * iOS: 使用内存缓存
     */
    actual suspend fun saveSessions(sessions: List<ChatSession>) = withContext(Dispatchers.Default) {
        memoryCache = sessions
        println("✅ Saved ${sessions.size} chat sessions to memory cache (iOS)")
    }
    
    /**
     * 获取存储路径
     */
    actual fun getStoragePath(): String = "memory://ios/chat-sessions"
    
    /**
     * 检查存储文件是否存在
     */
    actual suspend fun exists(): Boolean = withContext(Dispatchers.Default) {
        memoryCache.isNotEmpty()
    }
}

