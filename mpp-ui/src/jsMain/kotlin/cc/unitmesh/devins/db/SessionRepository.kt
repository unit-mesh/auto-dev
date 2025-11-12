@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
package cc.unitmesh.devins.db

import cc.unitmesh.session.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SessionRepository - JS 实现
 * 
 * SQLDelight 生成类名：SessionDb, SessionEventDb, UserDb
 * Domain 类名：Session, SessionEvent, User
 */
actual class SessionRepository(private val database: DevInsDatabase) {
    private val sessionQueries = database.sessionQueries
    private val json = Json { ignoreUnknownKeys = true }
    
    // Session CRUD
    actual fun getAllSessions(): List<Session> {
        return sessionQueries.selectAll().executeAsList().map { it.toDomain() }
    }
    
    actual fun getSessionById(id: String): Session? {
        return sessionQueries.selectById(id).executeAsOneOrNull()?.toDomain()
    }
    
    actual fun getSessionsByOwner(ownerId: String): List<Session> {
        return sessionQueries.selectByOwner(ownerId).executeAsList().map { it.toDomain() }
    }
    
    actual fun getActiveSessionsByOwner(ownerId: String): List<Session> {
        return sessionQueries.selectActiveByOwner(ownerId).executeAsList().map { it.toDomain() }
    }
    
    actual fun getSessionsByProject(projectId: String): List<Session> {
        return sessionQueries.selectByProject(projectId).executeAsList().map { it.toDomain() }
    }
    
    actual fun createSession(session: Session): String {
        sessionQueries.insert(
            id = session.id,
            projectId = session.projectId,
            task = session.task,
            status = session.status.toString(),
            ownerId = session.ownerId,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt,
            metadata = session.metadata?.let { json.encodeToString(it) }
        )
        return session.id
    }
    
    actual fun updateSessionStatus(id: String, status: SessionStatus, updatedAt: Long) {
        sessionQueries.updateStatus(status = status.toString(), updatedAt = updatedAt, id = id)
    }
    
    actual fun updateSessionMetadata(id: String, metadata: SessionMetadata, updatedAt: Long) {
        sessionQueries.updateMetadata(
            metadata = json.encodeToString(metadata),
            updatedAt = updatedAt,
            id = id
        )
    }
    
    actual fun deleteSession(id: String) {
        sessionQueries.delete(id)
    }
    
    actual fun deleteAllSessions() {
        sessionQueries.deleteAll()
    }
    
    // SessionEvent CRUD
    actual fun getSessionEvents(sessionId: String): List<SessionEventEnvelope> {
        return sessionQueries.selectEventsBySession(sessionId).executeAsList().map { it.toDomain() }
    }
    
    actual fun getSessionEventsPaginated(sessionId: String, limit: Long, offset: Long): List<SessionEventEnvelope> {
        return sessionQueries.selectEventsBySessionPaginated(sessionId, limit, offset)
            .executeAsList()
            .map { it.toDomain() }
    }
    
    actual fun getLatestEvent(sessionId: String): SessionEventEnvelope? {
        return sessionQueries.selectLatestEventBySession(sessionId).executeAsOneOrNull()?.toDomain()
    }
    
    actual fun countSessionEvents(sessionId: String): Long {
        return sessionQueries.countEventsBySession(sessionId).executeAsOne()
    }
    
    actual fun appendEvent(event: SessionEventEnvelope) {
        sessionQueries.insertEvent(
            id = "${event.sessionId}_${event.sequenceNumber}",
            sessionId = event.sessionId,
            eventId = event.eventId,
            timestamp = event.timestamp,
            sequenceNumber = event.sequenceNumber,
            eventType = event.eventType,
            eventData = event.eventData
        )
    }
    
    actual fun deleteSessionEvents(sessionId: String) {
        sessionQueries.deleteEventsBySession(sessionId)
    }
    
    // User CRUD
    actual fun getAllUsers(): List<User> {
        return sessionQueries.selectAllUsers().executeAsList().map { it.toDomain() }
    }
    
    actual fun getUserByUsername(username: String): User? {
        return sessionQueries.selectUserByUsername(username).executeAsOneOrNull()?.toDomain()
    }
    
    actual fun createUser(user: User) {
        sessionQueries.insertUser(
            username = user.username,
            passwordHash = user.passwordHash,
            createdAt = user.createdAt
        )
    }
    
    actual fun updateUserPassword(username: String, passwordHash: String) {
        sessionQueries.updateUserPassword(passwordHash = passwordHash, username = username)
    }
    
    actual fun deleteUser(username: String) {
        sessionQueries.deleteUser(username)
    }
    
    // 数据库模型转换为领域模型
    private fun SessionDb.toDomain(): Session {
        return Session(
            id = this.id,
            projectId = this.projectId,
            task = this.task,
            status = SessionStatus.valueOf(this.status),
            ownerId = this.ownerId,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            metadata = this.metadata?.let { metadataStr -> 
                json.decodeFromString<SessionMetadata>(metadataStr) 
            }
        )
    }
    
    private fun SessionEventDb.toDomain(): SessionEventEnvelope {
        return SessionEventEnvelope(
            sessionId = this.sessionId,
            eventId = this.eventId,
            timestamp = this.timestamp,
            sequenceNumber = this.sequenceNumber,
            eventType = this.eventType,
            eventData = this.eventData
        )
    }
    
    private fun UserDb.toDomain(): User {
        return User(
            username = this.username,
            passwordHash = this.passwordHash,
            createdAt = this.createdAt
        )
    }
    
    actual companion object {
        private var instance: SessionRepository? = null
        
        actual fun getInstance(): SessionRepository {
            return instance ?: run {
                val driverFactory = DatabaseDriverFactory()
                val database = createDatabase(driverFactory)
                SessionRepository(database).also { instance = it }
            }
        }
    }
}
