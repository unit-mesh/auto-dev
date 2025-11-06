package cc.unitmesh.llm

import cc.unitmesh.llm.clients.CustomOpenAILLMClient
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.params.LLMParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 测试 CustomOpenAILLMClient 的基本功能
 */
class CustomOpenAILLMClientTest {

    @Test
    fun `should create CustomOpenAILLMClient with correct provider`() {
        val client = CustomOpenAILLMClient(
            apiKey = "test-api-key",
            baseUrl = "https://api.example.com/v1"
        )
        
        assertEquals(LLMProvider.OpenAI, client.llmProvider())
    }

    @Test
    fun `should use custom chat completions path`() {
        val client = CustomOpenAILLMClient(
            apiKey = "test-api-key",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            chatCompletionsPath = "chat/completions"
        )
        
        assertNotNull(client)
    }

    @Test
    fun `ExecutorFactory should create CustomOpenAI executor`() {
        val config = ModelConfig(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            apiKey = "test-key",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4"
        )
        
        val executor = ExecutorFactory.create(config)
        assertNotNull(executor)
    }

    @Test
    fun `ModelRegistry should create generic model for custom OpenAI`() {
        val model = ModelRegistry.createGenericModel(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            contextLength = 128000L
        )
        
        assertNotNull(model)
        assertEquals("glm-4-plus", model.id)
        assertEquals(LLMProvider.OpenAI, model.provider)
        assertEquals(128000L, model.contextLength)
    }

    @Test
    fun `ModelConfig should validate custom OpenAI config`() {
        // 有效配置
        val validConfig = ModelConfig(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            apiKey = "test-key",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4"
        )
        assertTrue(validConfig.isValid())

        // 缺少 baseUrl
        val invalidConfig1 = ModelConfig(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            apiKey = "test-key",
            baseUrl = ""
        )
        assertTrue(!invalidConfig1.isValid())

        // 缺少 apiKey
        val invalidConfig2 = ModelConfig(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            apiKey = "",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4"
        )
        assertTrue(!invalidConfig2.isValid())
    }

    @Test
    fun `KoogLLMService should validate config before creation`() {
        val invalidConfig = ModelConfig(
            provider = LLMProviderType.CUSTOM_OPENAI_BASE,
            modelName = "glm-4-plus",
            apiKey = "",
            baseUrl = ""
        )

        try {
            KoogLLMService.create(invalidConfig)
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid model configuration") == true)
        }
    }
}

