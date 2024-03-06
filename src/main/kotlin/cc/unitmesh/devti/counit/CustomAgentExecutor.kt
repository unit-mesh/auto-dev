package cc.unitmesh.devti.counit

import cc.unitmesh.devti.counit.model.AuthType
import cc.unitmesh.devti.counit.model.CustomAgentConfig
import cc.unitmesh.devti.counit.model.CustomAgentResponseAction
import cc.unitmesh.devti.llms.custom.CustomRequest
import cc.unitmesh.devti.llms.custom.Message
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.theokanning.openai.service.SSE
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Service(Service.Level.PROJECT)
class CustomAgentExecutor(val project: Project) {
    private var client = OkHttpClient()
    private val logger = logger<CustomAgentExecutor>()

    fun execute(input: String, agent: CustomAgentConfig): Flow<String>? {
        val customRequest = CustomRequest(listOf(Message("user", input)))
        val request = Json.encodeToString<CustomRequest>(customRequest)

        val body = request.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val builder = Request.Builder()

        val auth = agent.auth
        when (auth?.type) {
            AuthType.Bearer -> {
                builder.addHeader("Authorization", "Bearer ${auth.token}")
                builder.addHeader("Content-Type", "application/json")
            }

            null -> {
                logger.info("No auth type found for agent ${agent.name}")
            }
        }

        client = client.newBuilder().build()
        val call = client.newCall(builder.url(agent.url).post(body).build())

        return when (agent.responseAction) {
            CustomAgentResponseAction.Stream -> {
                streamSSE(call)
            }

            else -> {
                streamJson(call)
            }
        }
    }

    private fun streamJson(call: Call): Flow<String> = callbackFlow {
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runBlocking {
                    send("error. ${e.message}")
                }
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                runBlocking() {
                    withContext(Dispatchers.IO) {
                        val res: String = response.body?.string()
                            ?.removeSurrounding("\"")
                            ?.removePrefix("\n") ?: ""

                        send(res)
                    }
                    close()
                }
            }
        })
        awaitClose()
    }

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
                            val chunk: String = sse!!.data
                            trySend(chunk)
                        }

                    close()
                }
                awaitClose()
            }
        } catch (e: Exception) {
            return callbackFlow {
                close()
            }
        }
    }

}