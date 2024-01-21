package cc.unitmesh.devti.llms.openai

import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.llms.LLMProvider
import cc.unitmesh.devti.coder.recording.EmptyRecording
import cc.unitmesh.devti.coder.recording.JsonlRecording
import cc.unitmesh.devti.coder.recording.Recording
import cc.unitmesh.devti.coder.recording.RecordingInstruction
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.service.OpenAiService.defaultClient
import com.theokanning.openai.service.OpenAiService.defaultObjectMapper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import java.time.Duration


@Service(Service.Level.PROJECT)
class OpenAIProvider(val project: Project) : LLMProvider {
    private val service: OpenAiService
        get() {
            if (openAiKey.isEmpty()) {
                logger.error("openAiKey is empty")
                throw IllegalStateException("openAiKey is empty")
            }

            var openAiProxy = AutoDevSettingsState.getInstance().customOpenAiHost
            if (!openAiProxy.endsWith("/")) {
                openAiProxy += "/"
            }

            return if (openAiProxy.isEmpty()) {
                OpenAiService(openAiKey, timeout)
            } else {
                val mapper = defaultObjectMapper()
                val client = defaultClient(openAiKey, timeout)

                val retrofit = Retrofit.Builder()
                    .baseUrl(openAiProxy)
                    .client(client)
                    .addConverterFactory(JacksonConverterFactory.create(mapper))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()

                val api = retrofit.create(OpenAiApi::class.java)
                OpenAiService(api)
            }
        }

    private val timeout = Duration.ofSeconds(600)
    private val openAiVersion: String
        get() = AutoDevSettingsState.getInstance().openAiModel
    private val openAiKey: String
        get() = AutoDevSettingsState.getInstance().openAiKey

    private val maxTokenLength: Int
        get() = AutoDevSettingsState.getInstance().fetchMaxTokenLength()

    private val messages: MutableList<ChatMessage> = ArrayList()
    private var historyMessageLength: Int = 0

    private val recording: Recording
        get() {
            if (project.coderSetting.state.recordingInLocal) {
                return project.service<JsonlRecording>()
            }

            return EmptyRecording()
        }

    override fun clearMessage() {
        messages.clear()
        historyMessageLength = 0
    }

    override fun appendLocalMessage(msg: String, role: ChatRole) {
        val message = ChatMessage(role.roleName(), msg)
        messages.add(message)
    }

    override fun prompt(promptText: String): String {
        val completionRequest = prepareRequest(promptText, "")

        val completion = service.createChatCompletion(completionRequest)
        val output = completion
            .choices[0].message.content

        return output
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean): Flow<String> {
        if (!keepHistory) {
            clearMessage()
        }

        if (project.coderSetting.state.noChatHistory) {
            messages.clear()
        }

        var output = ""
        val completionRequest = prepareRequest(promptText, systemPrompt)

        return callbackFlow {
            withContext(Dispatchers.IO) {
                service.streamChatCompletion(completionRequest)
                    .doOnError { error ->
                        logger.error("Error in stream", error)
                        trySend(error.message ?: "Error occurs")
                    }
                    .blockingForEach { response ->
                        if (response.choices.isNotEmpty()) {
                            val completion = response.choices[0].message
                            if (completion != null && completion.content != null) {
                                output += completion.content
                                trySend(completion.content)
                            }
                        }
                    }

                recording.write(RecordingInstruction(promptText, output))
                close()
            }
        }
    }

    private fun prepareRequest(promptText: String, systemPrompt: String): ChatCompletionRequest? {
        if (messages.isEmpty()) {
            val systemMessage = ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt)
            messages.add(systemMessage)
        }

        val systemMessage = ChatMessage(ChatMessageRole.USER.value(), promptText)

        historyMessageLength += promptText.length
        if (historyMessageLength > maxTokenLength) {
            messages.clear()
        }

        messages.add(systemMessage)
        logger.info("messages length: ${messages.size}")

        return ChatCompletionRequest.builder()
            .model(openAiVersion)
            .temperature(0.0)
            .messages(messages)
            .build()
    }

    companion object {
        private val logger: Logger = logger<OpenAIProvider>()
    }
}
