package cc.unitmesh.devins.llm

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * WASM implementation of SessionStorage
 * Uses in-memory storage (localStorage is not available in WASM yet)
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
     * WASM: 使用内存缓存
     */
    actual suspend fun loadSessions(): List<ChatSession> {
        return memoryCache
    }
    
    /**
     * 保存所有本地 sessions
     * WASM: 使用内存缓存
     */
    actual suspend fun saveSessions(sessions: List<ChatSession>) {
        memoryCache = sessions
        println("✅ Saved ${sessions.size} chat sessions to memory cache (WASM)")
    }
    
    /**
     * 获取存储路径
     */
    actual fun getStoragePath(): String = "memory://wasm/chat-sessions"
    
    /**
     * 检查存储文件是否存在
     */
    actual suspend fun exists(): Boolean {
        return memoryCache.isNotEmpty()
    }
}

