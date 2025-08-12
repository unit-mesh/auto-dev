package cc.unitmesh.devti.a2a

import io.a2a.A2A
import io.a2a.client.A2AClient
import io.a2a.spec.AgentCard
import io.a2a.spec.MessageSendParams
import io.a2a.spec.SendMessageResponse
import kotlinx.serialization.Serializable


class A2AClientConsumer {
    var clientMap: MutableMap<String, A2AClient> = mutableMapOf()
    var cardMap: MutableMap<String, AgentCard> = mutableMapOf()

    fun init(servers: List<A2aServer>): List<A2AClient> {
        servers.forEach {
            val client = A2AClient(it.url)
            val cardName = client.agentCard.name
            cardMap[cardName] = client.getAgentCard()
            clientMap[cardName] = client
        }

        return clientMap.values.toList()
    }

    fun listAgents(): List<AgentCard> {
        return clientMap.values.map { it.getAgentCard() }
    }

    fun sendMessage(agentName: String, msgText: String): String {
        val client = clientMap[agentName] ?: throw IllegalArgumentException("No client found for $agentName")
        val message = A2A.toUserMessage(msgText)
        val msgParams = MessageSendParams.Builder()
            .message(message)
            .build()

        return try {
            val response: SendMessageResponse = client.sendMessage(msgParams)
            response.toString()
        } catch (e: Exception) {
            throw RuntimeException("Failed to send message to agent $agentName: ${e.message}", e)
        }
    }
}

@Serializable
data class A2aServer(
    val url: String
) {}
