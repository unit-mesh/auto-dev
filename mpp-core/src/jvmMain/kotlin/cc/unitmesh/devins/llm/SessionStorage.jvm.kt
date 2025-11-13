package cc.unitmesh.devins.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * JVM implementation of SessionStorage
 * Stores sessions in ~/.autodev/sessions/chat-sessions.json
 */
actual object SessionStorage {
    private val homeDir = System.getProperty("user.home")
    private val sessionsDir = File(homeDir, ".autodev/sessions")
    private val sessionsFile = File(sessionsDir, "chat-sessions.json")
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * 加载所有本地 sessions
     */
    actual suspend fun loadSessions(): List<ChatSession> = withContext(Dispatchers.IO) {
        try {
            if (!sessionsFile.exists()) {
                return@withContext emptyList()
            }
            
            val content = sessionsFile.readText()
            if (content.isBlank()) {
                return@withContext emptyList()
            }
            
            json.decodeFromString<List<ChatSession>>(content)
        } catch (e: Exception) {
            println("⚠️ Failed to load chat sessions: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 保存所有本地 sessions
     */
    actual suspend fun saveSessions(sessions: List<ChatSession>) = withContext(Dispatchers.IO) {
        try {
            // 确保目录存在
            sessionsDir.mkdirs()
            
            // 序列化为 JSON
            val jsonContent = json.encodeToString(sessions)
            
            // 写入文件
            sessionsFile.writeText(jsonContent)
            
            println("✅ Saved ${sessions.size} chat sessions to ${sessionsFile.absolutePath}")
        } catch (e: Exception) {
            println("⚠️ Failed to save chat sessions: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * 获取存储路径
     */
    actual fun getStoragePath(): String = sessionsFile.absolutePath
    
    /**
     * 检查存储文件是否存在
     */
    actual suspend fun exists(): Boolean = withContext(Dispatchers.IO) {
        sessionsFile.exists()
    }
}

