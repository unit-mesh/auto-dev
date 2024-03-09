package cc.unitmesh.devti.llms.custom

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.logger
import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.extension.read
import com.theokanning.openai.completion.chat.ChatCompletionResult
import com.theokanning.openai.service.SSE
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.Call
import okhttp3.Request
import org.jetbrains.annotations.VisibleForTesting

/**
 * The `CustomSSEProcessor` class is responsible for processing server-sent events (SSE) in a custom manner.
 * It provides functions to stream JSON and SSE data from a given `Call` instance, and exposes properties for request and response formats.
 *
 * @property hasSuccessRequest A boolean flag indicating whether the request was successful.
 * @property requestFormat A string representing the format of the request.
 * @property responseFormat A string representing the format of the response.
 * @property logger An instance of the logger for logging purposes.
 *
 * @constructor Creates an instance of `CustomSSEProcessor`.
 */
open class CustomSSEProcessor {
    open var hasSuccessRequest: Boolean = true
    open val requestFormat: String = ""
    open val responseFormat: String = ""
    private val logger = logger<CustomSSEProcessor>()

    fun streamJson(call: Call): Flow<String> = callbackFlow {
        call.enqueue(JSONBodyResponseCallback(responseFormat) {
            withContext(Dispatchers.IO) {
                send(it)
            }
            close()
        })
        awaitClose()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun streamSSE(call: Call): Flow<String> {
        val sseFlowable = Flowable
            .create({ emitter: FlowableEmitter<SSE> ->
                call.enqueue(cc.unitmesh.devti.llms.azure.ResponseBodyCallback(emitter, true))
            }, BackpressureStrategy.BUFFER)

        try {
            return callbackFlow {
                withContext(Dispatchers.IO) {
                    sseFlowable
                        .doOnError {
                            it.printStackTrace()
                            close()
                        }
                        .blockingForEach { sse ->
                            if (responseFormat.isNotEmpty()) {
                                // {"id":"cmpl-a22a0d78fcf845be98660628fe5d995b","object":"chat.completion.chunk","created":822330,"model":"moonshot-v1-8k","choices":[{"index":0,"delta":{},"finish_reason":"stop","usage":{"prompt_tokens":434,"completion_tokens":68,"total_tokens":502}}]}
                                // in some case, the response maybe not equal to our response format, so we need to ignore it
//                                logger.info("SSE: ${sse.data}")
                                val chunk: String = try {
                                    JsonPath.parse(sse!!.data)?.read(responseFormat) ?: ""
                                } catch (e: Exception) {
                                    if (hasSuccessRequest) {
                                        logger.info("Failed to parse response", e)
                                    } else {
                                        logger.error("Failed to parse response", e)
                                    }
                                    return@blockingForEach
                                }

                                if (chunk.isEmpty()) {
                                    return@blockingForEach
                                }

                                hasSuccessRequest = true
                                trySend(chunk)
                            } else {
                                val result: ChatCompletionResult =
                                    ObjectMapper().readValue(sse!!.data, ChatCompletionResult::class.java)

                                val completion = result.choices[0].message
                                if (completion != null && completion.content != null) {
                                    trySend(completion.content)
                                }
                            }
                        }

                    close()
                }
                awaitClose()
            }
        } catch (e: Exception) {
            if (hasSuccessRequest) {
                logger.info("Failed to stream", e)
            } else {
                logger.error("Failed to stream", e)
            }

            return callbackFlow {
                close()
            }
        }
    }
}

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class CustomRequest(val messages: List<Message>)

@VisibleForTesting
fun Request.Builder.appendCustomHeaders(customRequestHeader: String): Request.Builder = apply {
    runCatching {
        Json.parseToJsonElement(customRequestHeader)
            .jsonObject["customHeaders"].let { customFields ->
            customFields?.jsonObject?.forEach { (key, value) ->
                header(key, value.jsonPrimitive.content)
            }
        }
    }.onFailure {
        logger<CustomLLMProvider>().warn("Failed to parse custom request header", it)
    }
}

@VisibleForTesting
fun JsonObject.updateCustomBody(customRequest: String): JsonObject {
    return runCatching {
        buildJsonObject {
            // copy origin object
            this@updateCustomBody.forEach { u, v -> put(u, v) }

            val customRequestJson = Json.parseToJsonElement(customRequest).jsonObject
            customRequestJson["customFields"]?.let { customFields ->
                customFields.jsonObject.forEach { (key, value) ->
                    put(key, value.jsonPrimitive)
                }
            }

            // TODO clean code with magic literals
            var roleKey = "role"
            var contentKey = "content"
            customRequestJson.jsonObject["messageKeys"]?.let {
                roleKey = it.jsonObject["role"]?.jsonPrimitive?.content ?: "role"
                contentKey = it.jsonObject["content"]?.jsonPrimitive?.content ?: "content"
            }

            val messages: JsonArray = this@updateCustomBody["messages"]?.jsonArray ?: buildJsonArray { }
            this.put("messages", buildJsonArray {
                messages.forEach { message ->
                    val role: String = message.jsonObject["role"]?.jsonPrimitive?.content ?: "user"
                    val content: String = message.jsonObject["content"]?.jsonPrimitive?.content ?: ""
                    add(buildJsonObject {
                        put(roleKey, role)
                        put(contentKey, content)
                    })
                }
            })
        }
    }.getOrElse {
        logger<CustomLLMProvider>().error("Failed to parse custom request body", it)
        this
    }
}

fun CustomRequest.updateCustomFormat(format: String): String {
    val requestContentOri = Json.encodeToString<CustomRequest>(this)
    return Json.parseToJsonElement(requestContentOri)
        .jsonObject.updateCustomBody(format).toString()
}