package cc.unitmesh.devti.a2a

import io.a2a.A2A
import io.a2a.client.Client
import io.a2a.client.ClientEvent
import io.a2a.client.MessageEvent
import io.a2a.client.TaskEvent
import io.a2a.client.TaskUpdateEvent
import io.a2a.client.config.ClientConfig
import io.a2a.client.transport.jsonrpc.JSONRPCTransport
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig
import io.a2a.spec.AgentCard
import io.a2a.spec.Message
import io.a2a.client.http.A2ACardResolver
import kotlinx.serialization.Serializable
import com.fasterxml.jackson.databind.ObjectMapper
import io.a2a.client.http.JdkA2AHttpClient
import io.a2a.spec.Part
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class A2AClientConsumer {
    private val clientMap: MutableMap<String, Client> = ConcurrentHashMap()
    private val cardMap: MutableMap<String, AgentCard> = ConcurrentHashMap()
    private val jacksonObjectMapper = ObjectMapper()
    private val responseMap: MutableMap<String, CompletableFuture<String>> = ConcurrentHashMap()

    fun init(servers: List<A2aServer>): List<Client> {
        servers.forEach { server ->
            try {
                // Get agent card using A2ACardResolver
                val cardResolver = A2ACardResolver(server.url)
                val agentCard = cardResolver.getAgentCard()
                val agentName = agentCard.name()

                // Create client configuration
                val clientConfig = ClientConfig.Builder()
                    .setAcceptedOutputModes(listOf("text", "application/json"))
                    .build()

                // Create event consumers to handle responses
                val consumers = listOf<BiConsumer<ClientEvent, AgentCard>>(
                    BiConsumer { event, card ->
                        handleClientEvent(event, card, agentName)
                    }
                )

                // Create error handler
                val errorHandler = Consumer<Throwable> { error ->
                    handleStreamingError(error, agentName)
                }

                // Create the client using the builder pattern
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
        val client = clientMap[agentName] ?: throw IllegalArgumentException("No client found for $agentName")

        return try {
            // Create message using A2A utility
            val message = A2A.toUserMessage(msgText)

            // Create a future to capture the response
            val responseFuture = CompletableFuture<String>()
            responseMap[agentName] = responseFuture

            // Send the message using the new API
            client.sendMessage(message, null)

            // Wait for response (with timeout)
            responseFuture.get(30, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: Exception) {
            responseMap.remove(agentName)
            throw RuntimeException("Failed to send message to agent $agentName: ${e.message}", e)
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
        val parts = message.getParts()
        if (parts != null) {
            return parts.joinToString("") { part ->
                when {
                    part.javaClass.simpleName == "TextPart" -> {
                        // Try to use the text() method for TextPart record
                        try {
                            val textMethod = part.javaClass.getMethod("text")
                            textMethod.invoke(part) as? String ?: ""
                        } catch (e: Exception) {
                            // Fallback to reflection for field access
                            try {
                                val textField = part.javaClass.getDeclaredField("text")
                                textField.isAccessible = true
                                textField.get(part) as? String ?: ""
                            } catch (e2: Exception) {
                                part.toString()
                            }
                        }
                    }
                    else -> part.toString()
                }
            }
        }
        return ""
    }
}

@Serializable
data class A2aServer(
    val url: String
) {}
