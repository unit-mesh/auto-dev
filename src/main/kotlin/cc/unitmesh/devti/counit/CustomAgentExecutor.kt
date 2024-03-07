package cc.unitmesh.devti.counit

import cc.unitmesh.devti.counit.model.AuthType
import cc.unitmesh.devti.counit.model.CustomAgentConfig
import cc.unitmesh.devti.counit.model.CustomAgentResponseAction
import cc.unitmesh.devti.llms.custom.CustomRequest
import cc.unitmesh.devti.llms.custom.CustomSSEProcessor
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.llms.custom.updateCustomFormat
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Service(Service.Level.PROJECT)
class CustomAgentExecutor(val project: Project) : CustomSSEProcessor() {
    private var client = OkHttpClient()
    private val logger = logger<CustomAgentExecutor>()

    override var requestFormat: String = "{ \"messageKeys\": {\"role\": \"role\", \"content\": \"content\"} }"
    override var responseFormat: String = "\$.choices[0].delta.content"

    fun execute(input: String, agent: CustomAgentConfig): Flow<String>? {
        this.requestFormat = agent.connector?.requestFormat ?: this.requestFormat
        this.responseFormat = agent.connector?.responseFormat ?: this.responseFormat

        val customRequest = CustomRequest(listOf(Message("user", input)))
        val request = customRequest.updateCustomFormat(requestFormat)

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
}