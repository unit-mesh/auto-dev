package cc.unitmesh.devins.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android implementation of SessionStorage
 * Stores sessions in app-specific directory
 * 
 * Note: This requires an Android context to get the proper data directory.
 * For now, we use a fallback approach with in-memory storage.
 */
actual object SessionStorage {
    private var memoryCache: List<ChatSession> = emptyList()
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * 加载所有本地 sessions
     * Android: 使用内存缓存（因为需要 Context 才能访问文件系统）
     */
    actual suspend fun loadSessions(): List<ChatSession> = withContext(Dispatchers.Default) {
        memoryCache
    }
    
    /**
     * 保存所有本地 sessions
     * Android: 使用内存缓存
     */
    actual suspend fun saveSessions(sessions: List<ChatSession>) = withContext(Dispatchers.Default) {
        memoryCache = sessions
        println("✅ Saved ${sessions.size} chat sessions to memory cache (Android)")
    }
    
    /**
     * 获取存储路径
     */
    actual fun getStoragePath(): String = "memory://android/chat-sessions"
    
    /**
     * 检查存储文件是否存在
     */
    actual suspend fun exists(): Boolean = withContext(Dispatchers.Default) {
        memoryCache.isNotEmpty()
    }
}

