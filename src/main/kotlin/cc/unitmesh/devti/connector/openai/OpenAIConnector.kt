package cc.unitmesh.devti.connector.openai

import cc.unitmesh.devti.connector.CodeCopilot
import cc.unitmesh.devti.parser.parseCodeFromString
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.OPENAI_MODEL
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.service.OpenAiService.defaultClient
import com.theokanning.openai.service.OpenAiService.defaultObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import java.time.Duration


class OpenAIConnector : CodeCopilot {
    private val promptGenerator = PromptGenerator()
    private var service: OpenAiService

    private val timeout = Duration.ofSeconds(600)
    private val openAiVersion: String = AutoDevSettingsState.getInstance()?.openAiModel ?: OPENAI_MODEL[0]
    private val openAiKey: String = AutoDevSettingsState.getInstance()?.openAiKey ?: ""

    init {

        if (openAiKey.isEmpty()) {
            logger.error("openAiKey is empty")
            throw Exception("openAiKey is empty")
        }

        val openAiProxy = AutoDevSettingsState.getInstance()?.customOpenAiHost
        if (openAiProxy.isNullOrEmpty()) {
            service = OpenAiService(openAiKey, timeout)
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
            service = OpenAiService(api)
        }
    }

    private val messages: MutableList<ChatMessage> = ArrayList()
    private var historyMessageLength: Int = 0

    override fun prompt(promptText: String): String {
        val completionRequest = prepareRequest(promptText)

        val completion = service.createChatCompletion(completionRequest)
        val output = completion
            .choices[0].message.content

        return output
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun stream(promptText: String): Flow<String> {
        val completionRequest = prepareRequest(promptText)

        return callbackFlow {
            withContext(Dispatchers.IO) {
                service.streamChatCompletion(completionRequest)
                    .doOnError(Throwable::printStackTrace)
                    .blockingForEach { response ->
                        val completion = response.choices[0].message
                        if (completion != null && completion.content != null) {
                            trySend(completion.content)
                        }
                    }

                close()
            }
        }

    }

    private fun prepareRequest(promptText: String): ChatCompletionRequest? {
        val systemMessage = ChatMessage(ChatMessageRole.USER.value(), promptText)

        historyMessageLength += promptText.length
        if (historyMessageLength > 16384) {
            messages.clear()
        }

        messages.add(systemMessage)

        return ChatCompletionRequest.builder()
            .model(openAiVersion)
            .temperature(0.0)
            .messages(messages)
            .build()
    }

    override fun autoComment(text: @NlsSafe String): String {
        val promptText = promptGenerator.autoComment(text)
        logger.warn("autoComment prompt text: $promptText")
        return runBlocking {
            val prompt = prompt(promptText)
            return@runBlocking parseCodeFromString(prompt)[0]
        }
    }

    override fun findBug(text: String): String {
        val promptText = promptGenerator.findBug(text)
        logger.warn("findBug prompt text: $promptText")
        return runBlocking {
            return@runBlocking prompt(promptText)
        }
    }

    companion object {
        private val logger: Logger = logger<OpenAIConnector>()
    }
}