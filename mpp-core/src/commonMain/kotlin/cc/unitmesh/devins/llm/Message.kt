package cc.unitmesh.devins.llm

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * 消息角色
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * 聊天消息
 */
@Serializable
data class Message(
    val role: MessageRole,
    val content: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val metadata: MessageMetadata? = null
)

/**
 * 聊天会话历史
 */
@Serializable
data class ChatSession(
    val id: String,
    var title: String? = null,
    val messages: MutableList<Message> = mutableListOf(),
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    var updatedAt: Long = Clock.System.now().toEpochMilliseconds()
) {
    /**
     * 添加用户消息
     */
    fun addUserMessage(content: String) {
        messages.add(Message(MessageRole.USER, content))
        updatedAt = Clock.System.now().toEpochMilliseconds()
    }

    /**
     * 添加助手消息
     */
    fun addAssistantMessage(content: String) {
        messages.add(Message(MessageRole.ASSISTANT, content))
        updatedAt = Clock.System.now().toEpochMilliseconds()
    }

    /**
     * 添加系统消息
     */
    fun addSystemMessage(content: String) {
        messages.add(Message(MessageRole.SYSTEM, content))
        updatedAt = Clock.System.now().toEpochMilliseconds()
    }
    
    /**
     * 获取最近 N 条消息
     */
    fun getRecentMessages(count: Int): List<Message> {
        return messages.takeLast(count)
    }
    
    /**
     * 清空历史
     */
    fun clear() {
        messages.clear()
        updatedAt = Clock.System.now().toEpochMilliseconds()
    }
}

