package cc.unitmesh.devti.agent.custom

import cc.unitmesh.devti.agent.custom.model.AuthType
import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.agent.custom.model.CustomAgentResponseAction
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.llms.custom.CustomRequest
import cc.unitmesh.devti.llms.custom.CustomSSEProcessor
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.llms.custom.updateCustomFormat
import cc.unitmesh.devti.provider.devins.CustomAgentContext
import cc.unitmesh.devti.provider.devins.LanguageProcessor
import cc.unitmesh.devti.util.readText
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Service(Service.Level.PROJECT)
class CustomAgentExecutor(val project: Project) : CustomSSEProcessor(project) {
    private var client = OkHttpClient()
    private val logger = logger<CustomAgentExecutor>()
    private val messages: MutableList<Message> = mutableListOf()

    override var requestFormat: String = ""
    override var responseFormat: String = ""

    fun execute(promptText: String, agent: CustomAgentConfig): Flow<String>? {
        var prompt = promptText

        if (agent.isFromDevIns) {
            val devin = LanguageProcessor.devin()!!
            val file = project.baseDir.findFileByRelativePath(agent.devinScriptPath)!!
            prompt = runBlocking {
                val context = CustomAgentContext(
                    agent, "", filePath = file,
                    initVariables = mapOf("input" to promptText)
                )
                devin.execute(project, context)
            }

            messages.add(Message("user", prompt))
            return LlmFactory.create(project).stream(prompt, "")
        }

        messages.add(Message("user", promptText))

        this.requestFormat = agent.connector?.requestFormat ?: this.requestFormat
        this.responseFormat = agent.connector?.responseFormat ?: this.responseFormat

        val customRequest = CustomRequest(listOf(Message("user", prompt)))
        var request = if (requestFormat.isNotEmpty()) {
            customRequest.updateCustomFormat(requestFormat)
        } else {
            Json.encodeToString<CustomRequest>(customRequest)
        }

        request = replacePlaceholders(request, prompt)

        val body = request.toRequestBody("application/json".toMediaTypeOrNull())
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

        client = client.newBuilder().connectTimeout(agent.defaultTimeout, TimeUnit.SECONDS)
            .readTimeout(agent.defaultTimeout, TimeUnit.SECONDS).build()
        val call = client.newCall(builder.url(agent.url).post(body).build())

        return when (agent.responseAction) {
            CustomAgentResponseAction.Stream -> {
                streamSSE(call, promptText, messages = messages)
            }

            else -> {
                streamJson(call, promptText, messages)
            }
        }
    }

    companion object {
        private const val SIMPLE_CONTENT_PLACEHOLDER = "\"content\":\"\$content\""
        private const val JSON_VALUE_PLACEHOLDER_PATTERN = ":\\s*\"\\\$content\""

        /**
         * Replace placeholders in a request string with the actual prompt text
         */
        fun replacePlaceholders(request: String, promptText: String): String {
            var result = request

            // Replace simple content placeholder
            if (result.contains(SIMPLE_CONTENT_PLACEHOLDER)) {
                result = result.replace(SIMPLE_CONTENT_PLACEHOLDER, "\"content\": \"$promptText\"")
                return result
            }

            // Replace JSON value placeholders
            val regex = Regex(JSON_VALUE_PLACEHOLDER_PATTERN)
            if (result.contains(regex)) {
                result = regex.replace(result, ": \"$promptText\"")
            }

            return result
        }
    }
}
