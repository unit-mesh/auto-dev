package cc.unitmesh.llm

import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.*

/**
 * Executor 工厂 - 负责根据配置创建合适的 LLM Executor
 * 职责：
 * 1. 根据 Provider 类型创建对应的 Executor
 * 2. 处理不同 Provider 的初始化逻辑
 * 3. 统一 Executor 创建接口
 */
object ExecutorFactory {
    
    /**
     * 根据模型配置创建 Executor
     */
    fun create(config: ModelConfig): SingleLLMPromptExecutor {
        return when (config.provider) {
            LLMProviderType.OPENAI -> createOpenAI(config)
            LLMProviderType.ANTHROPIC -> createAnthropic(config)
            LLMProviderType.GOOGLE -> createGoogle(config)
            LLMProviderType.DEEPSEEK -> createDeepSeek(config)
            LLMProviderType.OLLAMA -> createOllama(config)
            LLMProviderType.OPENROUTER -> createOpenRouter(config)
        }
    }

    private fun createOpenAI(config: ModelConfig): SingleLLMPromptExecutor {
        return simpleOpenAIExecutor(config.apiKey)
    }

    private fun createAnthropic(config: ModelConfig): SingleLLMPromptExecutor {
        return simpleAnthropicExecutor(config.apiKey)
    }

    private fun createGoogle(config: ModelConfig): SingleLLMPromptExecutor {
        return simpleGoogleAIExecutor(config.apiKey)
    }

    private fun createDeepSeek(config: ModelConfig): SingleLLMPromptExecutor {
        return SingleLLMPromptExecutor(DeepSeekLLMClient(config.apiKey))
    }

    private fun createOllama(config: ModelConfig): SingleLLMPromptExecutor {
        val baseUrl = config.baseUrl.ifEmpty { "http://localhost:11434" }
        return simpleOllamaAIExecutor(baseUrl = baseUrl)
    }

    private fun createOpenRouter(config: ModelConfig): SingleLLMPromptExecutor {
        return simpleOpenRouterExecutor(config.apiKey)
    }
}
