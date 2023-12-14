package cc.unitmesh.devti.llms.custom

import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.ResponseType
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.extension.read
import com.theokanning.openai.completion.chat.ChatCompletionResult
import com.theokanning.openai.service.SSE
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.jetbrains.annotations.VisibleForTesting
import java.time.Duration

@Serializable
data class Message(val role: String, val message: String)

@Serializable
data class CustomRequest(val messages: List<Message>)

@Service(Service.Level.PROJECT)
class CustomLLMProvider(val project: Project) : LLMProvider {
    private val autoDevSettingsState = AutoDevSettingsState.getInstance()
    private val url
        get() = autoDevSettingsState.customEngineServer
    private val key
        get() = autoDevSettingsState.customEngineToken
    private val requestFormat: String
        get() = autoDevSettingsState.customEngineRequestFormat
    private val responseFormat
        get() = autoDevSettingsState.customEngineResponseFormat

    private var client = OkHttpClient()
    private val timeout = Duration.ofSeconds(600)
    private val messages: MutableList<Message> = ArrayList()

    private val logger = logger<CustomLLMProvider>()

    override fun clearMessage() {
        messages.clear()
    }

    override fun appendLocalMessage(msg: String, role: ChatRole) {
        messages += Message(role.roleName(), msg)
    }

    override fun prompt(promptText: String): String {
        return this.prompt(promptText, "")
    }

    override fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean): Flow<String> {
        if (!keepHistory) {
            clearMessage()
        }

        messages += Message("user", promptText)

        val customRequest = CustomRequest(messages)
        val requestContent = customRequest.updateCustomFormat(requestFormat)

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), requestContent)

        val builder = Request.Builder()
        if (key.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $key")
            builder.addHeader("Content-Type", "application/json")
        }
        builder.appendCustomHeaders(requestFormat)

        client = client.newBuilder().readTimeout(timeout).build()
        val call = client.newCall(builder.url(url).post(body).build())

        if (autoDevSettingsState.customEngineResponseType == ResponseType.SSE.name) {
            return streamSSE(call)
        } else {
            return streamJson(call)
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun streamJson(call: Call): Flow<String> = callbackFlow {
        call.enqueue(JSONBodyResponseCallback(responseFormat) {
            withContext(Dispatchers.IO) {
                send(it)
            }
            close()
        })
        awaitClose()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun streamSSE(call: Call): Flow<String> {
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
                                val chunk: String = JsonPath.parse(sse!!.data)?.read(responseFormat)
                                    ?: throw Exception("Failed to parse chunk: ${sse.data}")
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
            logger.error("Failed to stream", e)
            return callbackFlow {
                close()
            }
        }
    }

    fun prompt(instruction: String, input: String): String {
        messages += Message("user", instruction)
        val customRequest = CustomRequest(messages)
        val requestContent = Json.encodeToString<CustomRequest>(customRequest)

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), requestContent)

        logger.info("Requesting form: $requestContent ${body.toString()}")
        val builder = Request.Builder()
        if (key.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $key")
        }

        try {
            client = client.newBuilder().readTimeout(timeout).build()

            val request = builder.url(url).post(body).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                logger.error("$response")
                return ""
            }

            return response.body?.string() ?: ""
        } catch (e: IllegalArgumentException) {
            logger.error("Failed to set timeout", e)
            return ""
        }
    }
}

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
        // should I warn user?
        println("Failed to parse custom request header ${it.message}")
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
            var contentKey = "message"
            customRequestJson.jsonObject["messageKeys"]?.let {
                roleKey = it.jsonObject["role"]?.jsonPrimitive?.content ?: "role"
                contentKey = it.jsonObject["content"]?.jsonPrimitive?.content ?: "message"
            }

            val messages: JsonArray = this@updateCustomBody["messages"]?.jsonArray ?: buildJsonArray { }


            this.put("messages", buildJsonArray {
                messages.forEach { message ->
                    val role: String = message.jsonObject["role"]?.jsonPrimitive?.content ?: "user"
                    val content: String = message.jsonObject["message"]?.jsonPrimitive?.content ?: ""
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
