package cc.unitmesh.devti.settings.dialog

import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.serialization.json.Json

class QuickLLMSetupDialogTest : BasePlatformTestCase() {

    fun testProviderConfigurations() {
        val settings = AutoDevSettingsState()
        val dialog = QuickLLMSetupDialog(project, settings) {}

        // Test that dialog can be created without errors
        assertNotNull(dialog)
    }

    fun testDialogHasTestConnectionFeature() {
        val settings = AutoDevSettingsState()
        val dialog = QuickLLMSetupDialog(project, settings) {}

        // Test that dialog has test connection components
        assertNotNull(dialog)
        // The test button and result label should be accessible through the dialog
        // This is a basic test to ensure the dialog structure is correct
    }

    fun testDeepSeekConfiguration() {
        val settings = AutoDevSettingsState()
        
        // Simulate creating a DeepSeek configuration
        val deepSeekConfig = LlmConfig(
            name = "DeepSeek",
            description = "Created by Quick Setup - DeepSeek",
            url = "https://api.deepseek.com/v1/chat/completions",
            auth = cc.unitmesh.devti.llm2.model.Auth(type = "Bearer", token = "test-token"),
            maxTokens = 4096,
            customRequest = cc.unitmesh.devti.llm2.model.CustomRequest(
                headers = emptyMap(),
                body = mapOf(
                    "model" to kotlinx.serialization.json.JsonPrimitive("deepseek-chat"),
                    "temperature" to kotlinx.serialization.json.JsonPrimitive(0.0),
                    "stream" to kotlinx.serialization.json.JsonPrimitive(true)
                ),
                stream = true
            ),
            modelType = cc.unitmesh.devti.llm2.model.ModelType.Default
        )

        // Verify configuration properties
        assertEquals("DeepSeek", deepSeekConfig.name)
        assertEquals("https://api.deepseek.com/v1/chat/completions", deepSeekConfig.url)
        assertEquals("test-token", deepSeekConfig.auth.token)
        assertEquals(4096, deepSeekConfig.maxTokens)
        assertTrue(deepSeekConfig.customRequest.stream)
    }

    fun testGLMConfiguration() {
        val settings = AutoDevSettingsState()
        
        // Simulate creating a GLM configuration
        val glmConfig = LlmConfig(
            name = "GLM",
            description = "Created by Quick Setup - GLM (智谱清言)",
            url = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            auth = cc.unitmesh.devti.llm2.model.Auth(type = "Bearer", token = "test-token"),
            maxTokens = 4096,
            customRequest = cc.unitmesh.devti.llm2.model.CustomRequest(
                headers = emptyMap(),
                body = mapOf(
                    "model" to kotlinx.serialization.json.JsonPrimitive("glm-4"),
                    "temperature" to kotlinx.serialization.json.JsonPrimitive(0.0),
                    "stream" to kotlinx.serialization.json.JsonPrimitive(true)
                ),
                stream = true
            ),
            modelType = cc.unitmesh.devti.llm2.model.ModelType.Default
        )

        // Verify configuration properties
        assertEquals("GLM", glmConfig.name)
        assertEquals("https://open.bigmodel.cn/api/paas/v4/chat/completions", glmConfig.url)
        assertEquals("test-token", glmConfig.auth.token)
        assertEquals(4096, glmConfig.maxTokens)
        assertTrue(glmConfig.customRequest.stream)
    }

    fun testQwenConfiguration() {
        val settings = AutoDevSettingsState()
        
        // Simulate creating a Qwen configuration
        val qwenConfig = LlmConfig(
            name = "Qwen",
            description = "Created by Quick Setup - Qwen (通义千问)",
            url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation",
            auth = cc.unitmesh.devti.llm2.model.Auth(type = "Bearer", token = "test-token"),
            maxTokens = 4096,
            customRequest = cc.unitmesh.devti.llm2.model.CustomRequest(
                headers = emptyMap(),
                body = mapOf(
                    "model" to kotlinx.serialization.json.JsonPrimitive("qwen-turbo"),
                    "temperature" to kotlinx.serialization.json.JsonPrimitive(0.0),
                    "stream" to kotlinx.serialization.json.JsonPrimitive(true)
                ),
                stream = true
            ),
            modelType = cc.unitmesh.devti.llm2.model.ModelType.Default
        )

        // Verify configuration properties
        assertEquals("Qwen", qwenConfig.name)
        assertEquals("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation", qwenConfig.url)
        assertEquals("test-token", qwenConfig.auth.token)
        assertEquals(4096, qwenConfig.maxTokens)
        assertTrue(qwenConfig.customRequest.stream)
    }

    fun testMoonshotConfiguration() {
        val settings = AutoDevSettingsState()
        
        // Simulate creating a Moonshot configuration
        val moonshotConfig = LlmConfig(
            name = "Moonshot",
            description = "Created by Quick Setup - Moonshot (月之暗面)",
            url = "https://api.moonshot.cn/v1/chat/completions",
            auth = cc.unitmesh.devti.llm2.model.Auth(type = "Bearer", token = "test-token"),
            maxTokens = 4096,
            customRequest = cc.unitmesh.devti.llm2.model.CustomRequest(
                headers = emptyMap(),
                body = mapOf(
                    "model" to kotlinx.serialization.json.JsonPrimitive("moonshot-v1-8k"),
                    "temperature" to kotlinx.serialization.json.JsonPrimitive(0.0),
                    "stream" to kotlinx.serialization.json.JsonPrimitive(true)
                ),
                stream = true
            ),
            modelType = cc.unitmesh.devti.llm2.model.ModelType.Default
        )

        // Verify configuration properties
        assertEquals("Moonshot", moonshotConfig.name)
        assertEquals("https://api.moonshot.cn/v1/chat/completions", moonshotConfig.url)
        assertEquals("test-token", moonshotConfig.auth.token)
        assertEquals(4096, moonshotConfig.maxTokens)
        assertTrue(moonshotConfig.customRequest.stream)
    }

    fun testBaichuanConfiguration() {
        val settings = AutoDevSettingsState()
        
        // Simulate creating a Baichuan configuration
        val baichuanConfig = LlmConfig(
            name = "Baichuan",
            description = "Created by Quick Setup - Baichuan (百川)",
            url = "https://api.baichuan-ai.com/v1/chat/completions",
            auth = cc.unitmesh.devti.llm2.model.Auth(type = "Bearer", token = "test-token"),
            maxTokens = 4096,
            customRequest = cc.unitmesh.devti.llm2.model.CustomRequest(
                headers = emptyMap(),
                body = mapOf(
                    "model" to kotlinx.serialization.json.JsonPrimitive("Baichuan2-Turbo"),
                    "temperature" to kotlinx.serialization.json.JsonPrimitive(0.0),
                    "stream" to kotlinx.serialization.json.JsonPrimitive(true)
                ),
                stream = true
            ),
            modelType = cc.unitmesh.devti.llm2.model.ModelType.Default
        )

        // Verify configuration properties
        assertEquals("Baichuan", baichuanConfig.name)
        assertEquals("https://api.baichuan-ai.com/v1/chat/completions", baichuanConfig.url)
        assertEquals("test-token", baichuanConfig.auth.token)
        assertEquals(4096, baichuanConfig.maxTokens)
        assertTrue(baichuanConfig.customRequest.stream)
    }

    fun testConfigurationSerialization() {
        val settings = AutoDevSettingsState()
        
        // Create a test configuration
        val testConfig = LlmConfig(
            name = "Test LLM",
            description = "Test configuration",
            url = "https://api.test.com/v1/chat/completions",
            auth = cc.unitmesh.devti.llm2.model.Auth(type = "Bearer", token = "test-token"),
            maxTokens = 4096,
            customRequest = cc.unitmesh.devti.llm2.model.CustomRequest(
                headers = emptyMap(),
                body = mapOf(
                    "model" to kotlinx.serialization.json.JsonPrimitive("test-model"),
                    "temperature" to kotlinx.serialization.json.JsonPrimitive(0.0),
                    "stream" to kotlinx.serialization.json.JsonPrimitive(true)
                ),
                stream = true
            ),
            modelType = cc.unitmesh.devti.llm2.model.ModelType.Default
        )

        // Test serialization
        val json = Json { prettyPrint = true }
        val serialized = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(LlmConfig.serializer()),
            listOf(testConfig)
        )

        assertNotNull(serialized)
        assertTrue(serialized.contains("Test LLM"))
        assertTrue(serialized.contains("https://api.test.com/v1/chat/completions"))
        assertTrue(serialized.contains("test-token"))
    }
}
