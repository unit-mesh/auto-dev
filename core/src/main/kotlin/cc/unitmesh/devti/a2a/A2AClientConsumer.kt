package cc.unitmesh.devti.a2a

import io.a2a.client.A2AClient
import io.a2a.spec.*
import kotlinx.serialization.Serializable

class A2AClientConsumer {
    var cardMap: MutableMap<String, A2AClient> = mutableMapOf()
    fun init(servers: List<A2aServer>): List<A2AClient> {
        servers.forEach {
            cardMap[it.url] = A2AClient(it.url)
        }

        return cardMap.values.toList()
    }

    fun sendMessage(agentName: String, msg: String) {
        val client = cardMap[agentName] ?: throw IllegalArgumentException("No client found for $agentName")
        val msgParams = MessageSendParams.Builder()
            .message(msg.toUserMessage())
            .build()

        client.sendMessage(msgParams)
        return
    }
}
fun createTaskId(): String = "task-${System.currentTimeMillis()}"

fun String.toUserMessage(): Message? = Message.Builder()
    .role(Message.Role.USER)
    .taskId(createTaskId())
    .build()

@Serializable
data class A2aServer(
    val url: String
) {}
