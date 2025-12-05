package cc.unitmesh.devti.llms

class LlmMsg {
    data class ChatMessage(
        var role: ChatRole,
        var content: String,
        var name: String? = null,
    )

    companion object {
        fun fromMap(msgs: Map<String, String>): List<ChatMessage> {
            return msgs.map {
                ChatMessage(
                    role = ChatRole.from(it.key),
                    content = it.value,
                )
            }
        }
    }

    data class ChatChoice(
        val index: Int,
        val message: ChatMessage,
        val finishReason: FinishReason,
    )

    enum class FinishReason(val value: String) {
        Stopped("stop"),
        ContentFiltered("content_filter"),
        FunctionCall("function_call"),
        TokenLimitReached("length"),
        ;

        companion object
    }

    enum class ChatRole(val value: String) {
        System("system"),
        User("user"),
        Assistant("assistant"),
        Function("function"),
        ;

        companion object {
            fun from(key: String): ChatRole {
                return when (key) {
                    "system" -> System
                    "user" -> User
                    "assistant" -> Assistant
                    "function" -> Function
                    else -> throw IllegalArgumentException("Unknown chat role: $key")
                }
            }
        }
    }
}
