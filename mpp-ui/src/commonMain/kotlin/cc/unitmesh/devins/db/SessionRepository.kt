package cc.unitmesh.devins.db

import cc.unitmesh.session.*

/**
 * SessionRepository - 会话数据访问层
 * 使用 expect/actual 模式支持跨平台
 */
expect class SessionRepository {
    // Session CRUD
    fun getAllSessions(): List<Session>
    fun getSessionById(id: String): Session?
    fun getSessionsByOwner(ownerId: String): List<Session>
    fun getActiveSessionsByOwner(ownerId: String): List<Session>
    fun getSessionsByProject(projectId: String): List<Session>
    fun createSession(session: Session): String
    fun updateSessionStatus(id: String, status: SessionStatus, updatedAt: Long)
    fun updateSessionMetadata(id: String, metadata: SessionMetadata, updatedAt: Long)
    fun deleteSession(id: String)
    fun deleteAllSessions()
    
    // SessionEvent CRUD
    fun getSessionEvents(sessionId: String): List<SessionEventEnvelope>
    fun getSessionEventsPaginated(sessionId: String, limit: Long, offset: Long): List<SessionEventEnvelope>
    fun getLatestEvent(sessionId: String): SessionEventEnvelope?
    fun countSessionEvents(sessionId: String): Long
    fun appendEvent(event: SessionEventEnvelope)
    fun deleteSessionEvents(sessionId: String)
    
    // User CRUD（简单认证）
    fun getAllUsers(): List<User>
    fun getUserByUsername(username: String): User?
    fun createUser(user: User)
    fun updateUserPassword(username: String, passwordHash: String)
    fun deleteUser(username: String)
    
    companion object {
        fun getInstance(): SessionRepository
    }
}

