package cc.unitmesh.session

import kotlinx.serialization.Serializable

/**
 * Session - 会话模型
 * 每个 Agent 任务执行对应一个 Session
 */
@Serializable
data class Session(
    val id: String,                      // UUID
    val projectId: String,               // 项目 ID
    val task: String,                    // 任务描述
    val status: SessionStatus,           // 会话状态
    val ownerId: String,                 // 会话所有者（用户名）
    val createdAt: Long,                 // 创建时间
    val updatedAt: Long,                 // 更新时间
    val metadata: SessionMetadata? = null
)

@Serializable
enum class SessionStatus {
    PENDING,      // 等待执行
    RUNNING,      // 执行中
    PAUSED,       // 暂停
    COMPLETED,    // 完成
    FAILED,       // 失败
    CANCELLED     // 取消
}

@Serializable
data class SessionMetadata(
    val gitUrl: String? = null,
    val branch: String? = null,
    val maxIterations: Int = 100,
    val currentIteration: Int = 0,
    val llmConfig: String? = null        // JSON serialized LLMConfig
)

/**
 * SessionParticipant - 会话参与者
 */
@Serializable
data class SessionParticipant(
    val sessionId: String,
    val userId: String,
    val role: ParticipantRole,
    val joinedAt: Long
)

@Serializable
enum class ParticipantRole {
    OWNER,     // 拥有者（可控制执行）
    VIEWER     // 观察者（只读）
}

/**
 * SessionEventEnvelope - 会话事件包装器
 * 包含会话关联信息和序列号，确保事件顺序
 */
@Serializable
data class SessionEventEnvelope(
    val sessionId: String,               // 会话 ID
    val eventId: String,                 // 事件 ID (UUID)
    val timestamp: Long,                 // 时间戳
    val sequenceNumber: Long,            // 序列号（确保顺序）
    val eventType: String,               // 事件类型
    val eventData: String                // 事件数据（JSON）
)

/**
 * SessionState - 会话状态快照
 * 用于断线重连和状态同步
 */
@Serializable
data class SessionState(
    val sessionId: String,
    val status: SessionStatus,
    val currentIteration: Int,
    val maxIterations: Int,
    val events: List<SessionEventEnvelope>,  // 历史事件
    val lastEventSequence: Long              // 最后事件序列号
)

/**
 * CreateSessionRequest - 创建会话请求
 */
@Serializable
data class CreateSessionRequest(
    val projectId: String,
    val task: String,
    val userId: String,
    val metadata: SessionMetadata? = null
)

/**
 * User - 用户模型（简单认证）
 */
@Serializable
data class User(
    val username: String,
    val passwordHash: String,  // 存储密码哈希
    val createdAt: Long
)

/**
 * LoginRequest - 登录请求
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * LoginResponse - 登录响应
 */
@Serializable
data class LoginResponse(
    val success: Boolean,
    val username: String? = null,
    val token: String? = null,  // 简单的 token（可以是 username）
    val message: String? = null
)

