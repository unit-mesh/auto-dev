package cc.unitmesh.devti.a2a

import com.intellij.openapi.diagnostic.logger
import io.a2a.A2A
import io.a2a.client.*
import io.a2a.client.config.ClientConfig
import io.a2a.client.http.A2ACardResolver
import io.a2a.client.transport.jsonrpc.JSONRPCTransport
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig
import io.a2a.spec.AgentCard
import io.a2a.spec.Message
import io.a2a.spec.TextPart
import kotlinx.serialization.Serializable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer
import java.util.function.Consumer

class A2AClientConsumer {
    private val clientMap: MutableMap<String, Client> = ConcurrentHashMap()
    private val cardMap: MutableMap<String, AgentCard> = ConcurrentHashMap()
    private val responseMap: MutableMap<String, CompletableFuture<String>> = ConcurrentHashMap()

    fun init(servers: List<A2aServer>): List<Client> {
        servers.forEach { server ->
            try {
                val cardResolver = A2ACardResolver(server.url)
                val agentCard = cardResolver.getAgentCard()
                val agentName = agentCard.name()

                val clientConfig = ClientConfig.Builder()
                    .setAcceptedOutputModes(listOf("text", "application/json"))
                    .build()

                val consumers = listOf<BiConsumer<ClientEvent, AgentCard>>(
                    BiConsumer { event, card ->
                        handleClientEvent(event, card, agentName)
                    }
                )

                val errorHandler = Consumer<Throwable> { error ->
                    handleStreamingError(error, agentName)
                }

                val client = Client.builder(agentCard)
                    .clientConfig(clientConfig)
                    .withTransport(JSONRPCTransport::class.java, JSONRPCTransportConfig())
                    .addConsumers(consumers)
                    .streamingErrorHandler(errorHandler)
                    .build()

                cardMap[agentName] = agentCard
                clientMap[agentName] = client
            } catch (e: Exception) {
                throw RuntimeException("Failed to initialize A2A client for ${server.url}: ${e.message}", e)
            }
        }

        return clientMap.values.toList()
    }

    fun listAgents(): List<AgentCard> {
        return cardMap.values.toList()
    }

    fun sendMessage(agentName: String, msgText: String): String {
        val client = clientMap[agentName]
            ?: return "Failed to find A2A client for $agentName. Available clients: ${clientMap.keys}"

        return try {
            val message = A2A.toUserMessage(msgText)

            val responseFuture = CompletableFuture<String>()
            responseMap[agentName] = responseFuture

            client.sendMessage(message, null)

            responseFuture.get(120, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: Exception) {
            responseMap.remove(agentName)
            logger<A2AClientConsumer>().error("Failed to send message to $agentName: ${e.message}", e)
            return e.message ?: "Unknown error"
        }
    }

    private fun handleClientEvent(event: ClientEvent, card: AgentCard, agentName: String) {
        when (event) {
            is MessageEvent -> {
                // Handle message response
                val message = event.message
                val responseFuture = responseMap[agentName]
                if (responseFuture != null && !responseFuture.isDone) {
                    try {
                        val responseText = extractTextFromMessage(message)
                        responseFuture.complete(responseText)
                    } catch (e: Exception) {
                        responseFuture.completeExceptionally(e)
                    } finally {
                        responseMap.remove(agentName)
                    }
                }
            }

            is TaskEvent -> {
                // Handle task events if needed
                // For now, we'll just log or ignore
            }

            is TaskUpdateEvent -> {
                // Handle task update events if needed
                // For now, we'll just log or ignore
            }
        }
    }

    private fun handleStreamingError(error: Throwable, agentName: String) {
        val responseFuture = responseMap[agentName]
        if (responseFuture != null && !responseFuture.isDone) {
            responseFuture.completeExceptionally(error)
            responseMap.remove(agentName)
        }
    }

    private fun extractTextFromMessage(message: Message): String {
        val parts = message.parts ?: return ""

        return parts.joinToString("") { part ->
            when {
                part is TextPart -> {
                    return@joinToString part.getText()
                }

                else -> part.toString()
            }
        }
    }
}

@Serializable
data class A2aServer(
    val url: String
) {}
