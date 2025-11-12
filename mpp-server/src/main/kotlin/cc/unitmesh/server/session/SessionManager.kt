package cc.unitmesh.server.session

import cc.unitmesh.agent.AgentEvent
import cc.unitmesh.session.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * SessionManager - 会话管理器
 * 核心组件，管理所有活跃会话和订阅者
 */
class SessionManager {
    // 活跃会话：sessionId -> Session
    private val activeSessions = ConcurrentHashMap<String, Session>()
    
    // 会话订阅者：sessionId -> List<EventChannel>
    private val sessionSubscribers = ConcurrentHashMap<String, MutableList<Channel<SessionEventEnvelope>>>()
    
    // 事件序列号：sessionId -> AtomicLong
    private val eventSequences = ConcurrentHashMap<String, AtomicLong>()
    
    // 简单的内存存储（后续可以替换为数据库）
    private val sessionStore = ConcurrentHashMap<String, Session>()
    private val eventStore = ConcurrentHashMap<String, MutableList<SessionEventEnvelope>>()
    
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    
    /**
     * 创建新会话
     */
    fun createSession(request: CreateSessionRequest): Session {
        val session = Session(
            id = UUID.randomUUID().toString(),
            projectId = request.projectId,
            task = request.task,
            status = SessionStatus.PENDING,
            ownerId = request.userId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            metadata = request.metadata
        )
        
        activeSessions[session.id] = session
        sessionStore[session.id] = session
        eventSequences[session.id] = AtomicLong(0)
        eventStore[session.id] = mutableListOf()
        
        logger.info { "Created session: ${session.id} for user: ${session.ownerId}" }
        
        return session
    }
    
    /**
     * 订阅会话（用于 SSE）
     */
    suspend fun subscribeToSession(sessionId: String, userId: String): Flow<SessionEventEnvelope> = flow {
        val session = getSession(sessionId)
        if (session == null) {
            logger.warn { "Session not found: $sessionId" }
            throw SessionNotFoundException(sessionId)
        }
        
        logger.info { "User $userId subscribing to session: $sessionId" }
        
        // 创建订阅通道
        val channel = Channel<SessionEventEnvelope>(Channel.BUFFERED)
        
        // 注册订阅者
        sessionSubscribers.computeIfAbsent(sessionId) { mutableListOf() }.add(channel)
        
        try {
            // 先发送历史事件
            val historicalEvents = eventStore[sessionId] ?: emptyList()
            logger.info { "Sending ${historicalEvents.size} historical events to user $userId" }
            
            historicalEvents.forEach { event ->
                emit(event)
            }
            
            // 然后监听实时事件
            for (event in channel) {
                emit(event)
            }
        } finally {
            // 取消订阅时移除通道
            sessionSubscribers[sessionId]?.remove(channel)
            channel.close()
            logger.info { "User $userId unsubscribed from session: $sessionId" }
        }
    }
    
    /**
     * 广播事件到所有订阅者
     */
    suspend fun broadcastEvent(sessionId: String, event: AgentEvent) {
        val sequence = eventSequences[sessionId]?.incrementAndGet()
        if (sequence == null) {
            logger.warn { "Session $sessionId not found, cannot broadcast event" }
            return
        }
        
        // 将 AgentEvent 序列化为 JSON
        val eventType = when (event) {
            is AgentEvent.IterationStart -> "iteration"
            is AgentEvent.LLMResponseChunk -> "llm_chunk"
            is AgentEvent.ToolCall -> "tool_call"
            is AgentEvent.ToolResult -> "tool_result"
            is AgentEvent.CloneLog -> "clone_log"
            is AgentEvent.CloneProgress -> "clone_progress"
            is AgentEvent.Error -> "error"
            is AgentEvent.Complete -> "complete"
        }
        
        val eventData = json.encodeToString(event)
        
        val envelope = SessionEventEnvelope(
            sessionId = sessionId,
            eventId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            sequenceNumber = sequence,
            eventType = eventType,
            eventData = eventData
        )
        
        // 持久化事件
        eventStore.computeIfAbsent(sessionId) { mutableListOf() }.add(envelope)
        
        // 广播到所有订阅者
        val subscribers = sessionSubscribers[sessionId]
        if (subscribers != null && subscribers.isNotEmpty()) {
            logger.debug { "Broadcasting event to ${subscribers.size} subscribers for session $sessionId" }
            subscribers.forEach { channel ->
                channel.trySend(envelope)
            }
        }
        
        // 更新会话状态
        when (event) {
            is AgentEvent.Complete -> {
                updateSessionStatus(sessionId, if (event.success) SessionStatus.COMPLETED else SessionStatus.FAILED)
            }
            is AgentEvent.IterationStart -> {
                // 更新为 RUNNING 状态
                if (getSession(sessionId)?.status == SessionStatus.PENDING) {
                    updateSessionStatus(sessionId, SessionStatus.RUNNING)
                }
            }
            else -> {}
        }
    }
    
    /**
     * 获取会话状态快照
     */
    fun getSessionState(sessionId: String): SessionState? {
        val session = getSession(sessionId) ?: return null
        val events = eventStore[sessionId] ?: emptyList()
        
        return SessionState(
            sessionId = sessionId,
            status = session.status,
            currentIteration = session.metadata?.currentIteration ?: 0,
            maxIterations = session.metadata?.maxIterations ?: 100,
            events = events,
            lastEventSequence = eventSequences[sessionId]?.get() ?: 0
        )
    }
    
    /**
     * 更新会话状态
     */
    fun updateSessionStatus(sessionId: String, status: SessionStatus) {
        val session = activeSessions[sessionId] ?: sessionStore[sessionId]
        if (session != null) {
            val updatedSession = session.copy(
                status = status,
                updatedAt = System.currentTimeMillis()
            )
            activeSessions[sessionId] = updatedSession
            sessionStore[sessionId] = updatedSession
            
            logger.info { "Updated session $sessionId status to $status" }
        }
    }
    
    fun getSession(sessionId: String): Session? {
        return activeSessions[sessionId] ?: sessionStore[sessionId]
    }
    
    fun getAllActiveSessions(): List<Session> {
        return activeSessions.values.toList()
    }
    
    fun getSessionsByOwner(ownerId: String): List<Session> {
        return sessionStore.values.filter { it.ownerId == ownerId }
    }
    
    fun getActiveSessionsByOwner(ownerId: String): List<Session> {
        return sessionStore.values.filter { 
            it.ownerId == ownerId && (it.status == SessionStatus.RUNNING || it.status == SessionStatus.PENDING)
        }
    }
    
    fun deleteSession(sessionId: String) {
        activeSessions.remove(sessionId)
        sessionStore.remove(sessionId)
        eventStore.remove(sessionId)
        eventSequences.remove(sessionId)
        
        // 关闭所有订阅者
        sessionSubscribers[sessionId]?.forEach { it.close() }
        sessionSubscribers.remove(sessionId)
        
        logger.info { "Deleted session: $sessionId" }
    }
}

class SessionNotFoundException(sessionId: String) : 
    Exception("Session not found: $sessionId")

