package cc.unitmesh.devti.history

import cc.unitmesh.devti.llms.custom.Message
import kotlinx.serialization.Serializable

@Serializable
data class ChatSessionHistory(
    val id: String,
    val name: String,
    val messages: List<Message>,
    val createdAt: Long
)