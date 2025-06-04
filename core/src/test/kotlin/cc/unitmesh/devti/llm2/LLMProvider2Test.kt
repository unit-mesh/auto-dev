package cc.unitmesh.devti.llm2

import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.settings.AutoDevSettingsState
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class LLMProvider2Test {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var llmProvider2: LLMProvider2

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val settings = AutoDevSettingsState().apply {
            customEngineServer = mockWebServer.url("/").toString()
            customModel = "test-model"
            customEngineResponseFormat = "\$.choices[0].delta.content"
            customEngineRequestFormat = "{}"
        }

        llmProvider2 = LLMProvider2(settings)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun shouldWorkWithJson() = runBlocking {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                """{"choices":[{"delta":{"role":"assistant","content":"Hello"}}]}"""
            )
        mockWebServer.enqueue(mockResponse)

        val response = llmProvider2.request(Message("User", "hi!"), false)
        response.collectLatest {
            println(it.chatMessage.content)
            assertEquals("Hello", it.chatMessage.content)
        }
    }

    @Ignore
    @Test(expected = IllegalStateException::class)
    fun shouldFailIfResponseOnlyWithIllegalJson() {
        runBlocking {
            val mockResponse = MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(
                    """{"choices":[{"delta":{"role":"assistant","__content":"Hello"}}]}
                    """.trimMargin()
                )
            mockWebServer.enqueue(mockResponse)

            val response = llmProvider2.request(Message("User", "hi!"), false)
            response.collectLatest { }
        }
    }

    @Ignore
    @Test(expected = IllegalStateException::class)
    fun shouldFailIfResponseOnlyWithIllegalJsonStream() {
        runBlocking {
            val mockResponse = MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """data: {"choices":[{"delta":{"role":"assistant","__content":"Hello"}}]}
                        |
                        |
                    """.trimMargin()
                )
            mockWebServer.enqueue(mockResponse)

            val response = llmProvider2.request(Message("User", "hi!"), true)
            response.collectLatest { }
        }
    }

    @Ignore
    @Test
    fun shouldIgnoreIllegalResponse() = runBlocking {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                """data: {"choices":[{"delta":{"role":"assistant","content":"Hello"}}]}
                    |
                    |data: {"id":"cmpl-ac26a17e","object":"chat.completion.chunk","created":1858403,"model":"yi-34b-chat","choices":[{"delta":{"role":"assistant"},"index":0}],"content":"","lastOne":false}
                    |
                    |data: [DONE]
                    |
                """.trimMargin()
            )
        mockWebServer.enqueue(mockResponse)

        val response = llmProvider2.request(Message("User", "hi!"), true)
        response.collectLatest {
            assertEquals("Hello", it.chatMessage.content)
        }
    }

    @Ignore
    @Test
    fun shouldEmitChanges() = runTest {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                """data: {"choices":[{"delta":{"role":"assistant","content":"Hello"}}]}
                    |
                    |data: {"choices":[{"delta":{"role":"assistant","content":" World"}}]}
                    |
                    |data: [DONE]
                    |
                """.trimMargin()
            )
        mockWebServer.enqueue(mockResponse)
        llmProvider2.request(Message("User", "hi!"), true).collectIndexed { index, value ->
            when (index) {
                0 -> assertEquals("Hello", value.chatMessage.content)
                1 -> assertEquals("Hello World", value.chatMessage.content)
            }
        }
    }

    @Ignore("This test is for local server")
    @Test(timeout = 10000)
    fun ollamaTestStream() = runTest {
        val result = LLMProvider2.Ollama("codellama:13b").request(Message("User", "hi!"), stream = true)
        result.collect {
            println(it.chatMessage.content)
        }
    }

    @Ignore("This test is for local server")
    @Test(timeout = 10000)
    fun ollamaTestWitoutStream() = runTest {
        val result = LLMProvider2.Ollama(
            "codellama:13b",
            responseResolver = "\$.choices[0].message.content"
        ).request(Message("User", "hi!"), stream = false)
        result.collect {
            println(it.chatMessage.content)
        }
    }

    @Ignore
    @Test
    fun githubCopilotStream() = runTest {
        val result = LLMProvider2.GithubCopilot(
            modelName = "o3-mini",
        ).request(
            Message("user", "你是谁"), stream = true
        )
        result.collect {
            println(it.chatMessage.content)
        }
    }

}