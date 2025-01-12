package cc.unitmesh.devti.llms.custom

import cc.unitmesh.devti.coder.recording.EmptyRecording
import cc.unitmesh.devti.coder.recording.JsonlRecording
import cc.unitmesh.devti.coder.recording.Recording
import cc.unitmesh.devti.coder.recording.RecordingInstruction
import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.settings.coder.coderSetting
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.extension.read
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
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
open class CustomSSEProcessor(private val project: Project) {
    open var hasSuccessRequest: Boolean = false
    private var parseFailedResponses: MutableList<String> = mutableListOf()
    open val requestFormat: String = ""
    open val responseFormat: String = ""
    private val logger = logger<CustomSSEProcessor>()

    private val recording: Recording
        get() {
            if (project.coderSetting.state.recordingInLocal) {
                return project.service<JsonlRecording>()
            }

            return EmptyRecording()
        }


    fun streamJson(call: Call, promptText: String, messages: MutableList<Message>): Flow<String> = callbackFlow {
        call.enqueue(JSONBodyResponseCallback(responseFormat) {
            withContext(Dispatchers.IO) {
                send(it)
            }

            if (it.isNotEmpty()) {
                messages += Message(ChatRole.Assistant.roleName(), it)
            }
            recording.write(RecordingInstruction(promptText, it))
            close()
        })
        awaitClose()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun streamSSE(call: Call, promptText: String, keepHistory: Boolean = false, messages: MutableList<Message>): Flow<String> {
        val sseFlowable = Flowable
            .create({ emitter: FlowableEmitter<SSE> ->
                call.enqueue(ResponseBodyCallback(emitter, true))
            }, BackpressureStrategy.BUFFER)

        try {
            var output = ""
            return callbackFlow {
                withContext(Dispatchers.IO) {
                    sseFlowable
                        .doOnError {
                            it.printStackTrace()
                            trySend(it.message ?: "Error occurs")
                            close()
                        }
                        .blockingForEach { sse ->
                            if (sse.data == "[DONE]") {
                                return@blockingForEach
                            }

                            if (responseFormat.isNotEmpty()) {
                                // {"id":"cmpl-a22a0d78fcf845be98660628fe5d995b","object":"chat.completion.chunk","created":822330,"model":"moonshot-v1-8k","choices":[{"index":0,"delta":{},"finish_reason":"stop","usage":{"prompt_tokens":434,"completion_tokens":68,"total_tokens":502}}]}
                                // in some case, the response maybe not equal to our response format, so we need to ignore it
                                // {"id":"cmpl-ac26a17e","object":"chat.completion.chunk","created":1858403,"model":"yi-34b-chat","choices":[{"delta":{"role":"assistant"},"index":0}],"content":"","lastOne":false}

                                val chunk: String? = JsonPath.parse(sse!!.data)?.read(responseFormat)
                                // new JsonPath lib caught the exception, so we need to handle when it is null
                                if (chunk == null) {
                                    parseFailedResponses.add(sse.data)
                                    logger.warn("Failed to parse response.origin response is: ${sse.data}, response format: $responseFormat")
                                } else {
                                    hasSuccessRequest = true
                                    output += chunk
                                    trySend(chunk)
                                }
                            } else {
                                val result: ChatCompletionResult =
                                    ObjectMapper().readValue(sse!!.data, ChatCompletionResult::class.java)

                                val completion = result.choices[0].message
                                if (completion != null && completion.content != null) {
                                    hasSuccessRequest = true

                                    output += completion.content
                                    trySend(completion.content)
                                }
                            }
                        }

                    // when stream finished, check if any response parsed succeeded
                    // if not, notice user check response format
                    if (!hasSuccessRequest) {
                        val errorMsg = """
                                        |**Failed** to parse response.please check your response format: 
                                        |**$responseFormat** origin responses is: 
                                        |- ${parseFailedResponses.joinToString("\n- ")}
                                        |""".trimMargin()

                        // TODO add refresh feature
                        // don't use trySend, it may be ignored by 'close()` op
                        send(errorMsg)
                    }

                    if (output.isNotEmpty()) {
                        messages += Message(ChatRole.Assistant.roleName(), output)
                    }

                    recording.write(RecordingInstruction(promptText, output))
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
        } finally {
            parseFailedResponses.clear()
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
            val customRequestJson = Json.parseToJsonElement(customRequest).jsonObject
            customRequestJson["fields"]?.jsonObject?.let { fieldsObj ->
                val messages: JsonArray = this@updateCustomBody["messages"]?.jsonArray ?: buildJsonArray {}
                val contentOfFirstMessage = if (messages.isNotEmpty()) {
                    messages.last().jsonObject["content"]?.jsonPrimitive?.content ?: ""
                } else ""
                fieldsObj.forEach { (fieldKey, fieldValue) ->
                    if (fieldValue is JsonObject) {
                        put(fieldKey, buildJsonObject {
                            fieldValue.forEach { (subKey, subValue) ->
                                if (subValue is JsonPrimitive && subValue.content == "\$content") {
                                    put(subKey, JsonPrimitive(contentOfFirstMessage))
                                } else {
                                    put(subKey, subValue)
                                }
                            }
                        })
                    } else if (fieldValue is JsonPrimitive && fieldValue.content == "\$content") {
                        put(fieldKey, JsonPrimitive(contentOfFirstMessage))
                    } else {
                        put(fieldKey, fieldValue)
                    }
                }

                return@buildJsonObject
            }

            this@updateCustomBody.forEach { u, v -> put(u, v) }
            customRequestJson["customFields"]?.let { customFields ->
                customFields.jsonObject.forEach { (key, value) ->
                    put(key, value)
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
    val updateCustomBody = Json.parseToJsonElement(requestContentOri)
        .jsonObject.updateCustomBody(format)

    return updateCustomBody.toString()
}

fun JsonObject.removeFields(vararg fields: String): JsonObject {
    return JsonObject(
        toMutableMap()
            .apply {
                fields.forEach { remove(it) }
            }
    )
}