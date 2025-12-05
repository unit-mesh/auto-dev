package cc.unitmesh.devti.llm2

import cc.unitmesh.devti.llm2.MessageStatus.*
import cc.unitmesh.devti.llms.custom.Message
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 后台返回消息状态：[BEGIN] 首次返回 [CONTENT] 中间内容 [END] 内容结束
 */
enum class MessageStatus {
    BEGIN, CONTENT, END
}

/**
 * 聊天会话，每个会话包含此次会话的所有消息和名称。
 */
data class ChatSession<T>(
    val conversionName: String,
    val chatHistory: List<SessionMessageItem<T>>,
    /**
     * 当前会话状态，为 [MessageStatus.END] 时才允许发下一个对话
     */
    var status: MessageStatus = END,
) {
    companion object {
        /**
         * 为方便使用，暂时以 invoke 工厂提供默认的 ChatSession 实现
         */
        operator fun invoke(conversionName: String, messages: List<Message> = listOf()): ChatSession<Message> =
            ChatSession<Message>(conversionName, messages.map { SessionMessageItem(it, END) })
    }
}

fun <T> ChatSession<T>.canSendNextMessage() = this.status == END

fun <T> ChatSession<T>.appendMessage(newMessage: T, status: MessageStatus = CONTENT): ChatSession<T> =
    this.copy(chatHistory = this.chatHistory.toMutableList().apply {
        add(SessionMessageItem(newMessage, status))
    })

/**
 * http 请求的 body 数据
 */
val ChatSession<Message>.httpContent: String
    get() {
        return chatHistory.filter {
            // exception 不需要发送给 llm server
            // TODO 对异常消息的处理需有更合理的设计
            it.chatMessage.role != "Error"
        }.joinToString(prefix = """{"messages":[""", postfix = "]}", separator = ",") { item ->
            Json.encodeToString(item.chatMessage)
        }
    }

/**
 * 聊天消息项
 * 后续可扩展为多模态消息，T 可为做任意内容
 *
 * @property chatMessage 聊天消息
 * @property status 消息状态，标记此消息的状态是刚开始、中间还是消息已经结束。
 */
open class SessionMessageItem<T>(
    val chatMessage: T,
    val status: MessageStatus = CONTENT,
) {
    companion object {

        /**
         * 为方便使用，暂时以 invoke 工厂提供默认的文本实现
         */
        operator fun invoke(
            chatMessage: Message,
            status: MessageStatus = CONTENT,
        ): SessionMessageItem<Message> =
            DefaultSessionMessageItem(chatMessage, status)

        val Null = SessionMessageItem(Message("", ""), END)
    }
}

/**
 * 默认的聊天消息项
 */
private class DefaultSessionMessageItem(
    chatMessage: Message,
    status: MessageStatus = CONTENT,
) : SessionMessageItem<Message>(chatMessage, status)
