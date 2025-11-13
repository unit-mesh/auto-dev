package cc.unitmesh.devins.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JS implementation of SessionStorage
 * Uses localStorage for browser environment
 */
actual object SessionStorage {
    private const val STORAGE_KEY = "autodev_chat_sessions"
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * 加载所有本地 sessions
     * JS: 使用 localStorage (浏览器) 或内存缓存 (Node.js)
     */
    actual suspend fun loadSessions(): List<ChatSession> = withContext(Dispatchers.Default) {
        try {
            val stored = getLocalStorage()?.getItem(STORAGE_KEY)
            if (stored != null) {
                json.decodeFromString<List<ChatSession>>(stored)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("⚠️ Failed to load sessions from localStorage: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 保存所有本地 sessions
     * JS: 使用 localStorage (浏览器) 或内存缓存 (Node.js)
     */
    actual suspend fun saveSessions(sessions: List<ChatSession>) = withContext(Dispatchers.Default) {
        try {
            val jsonString = json.encodeToString(sessions)
            getLocalStorage()?.setItem(STORAGE_KEY, jsonString)
            println("✅ Saved ${sessions.size} chat sessions to localStorage (JS)")
        } catch (e: Exception) {
            println("⚠️ Failed to save sessions to localStorage: ${e.message}")
        }
    }
    
    /**
     * 获取存储路径
     */
    actual fun getStoragePath(): String = "localStorage://$STORAGE_KEY"
    
    /**
     * 检查存储文件是否存在
     */
    actual suspend fun exists(): Boolean = withContext(Dispatchers.Default) {
        try {
            getLocalStorage()?.getItem(STORAGE_KEY) != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取 localStorage 实例（仅浏览器环境可用）
     */
    private fun getLocalStorage(): Storage? {
        return try {
            js("typeof localStorage !== 'undefined' ? localStorage : null") as? Storage
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Storage interface for JS interop
 */
external interface Storage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
    fun clear()
}
