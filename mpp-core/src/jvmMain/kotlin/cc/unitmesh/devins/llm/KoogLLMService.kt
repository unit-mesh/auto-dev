package cc.unitmesh.devins.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.list
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.*
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import cc.unitmesh.devins.compiler.DevInsCompilerFacade
import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow

class KoogLLMService(private val config: ModelConfig) {
    /**
     * 流式发送提示，支持 DevIns 编译、SpecKit 命令和多轮对话
     * @param userPrompt 用户输入的提示文本（可以包含 DevIns 语法和命令）
     * @param fileSystem 项目文件系统，用于支持 SpecKit 等命令（可选）
     * @param historyMessages 历史消息列表，用于多轮对话（可选）
     */
    fun streamPrompt(
        userPrompt: String, 
        fileSystem: ProjectFileSystem = EmptyFileSystem(),
        historyMessages: List<Message> = emptyList()
    ): Flow<String> = flow {
        val executor = createExecutor()
        val model = getModelForProvider()

        // 创建带有文件系统的编译上下文
        val context = CompilerContext().apply {
            this.fileSystem = fileSystem
        }

        // 编译 DevIns 代码，支持 SpecKit 命令（只编译最新的用户输入）
        println("🔍 [KoogLLMService] 开始编译 DevIns 代码...")
        println("🔍 [KoogLLMService] 用户输入: $userPrompt")
        println("🔍 [KoogLLMService] 历史消息数: ${historyMessages.size}")
        println("🔍 [KoogLLMService] 文件系统: ${fileSystem.javaClass.simpleName}")
        println("🔍 [KoogLLMService] 项目路径: ${fileSystem.getProjectPath()}")

        val compiledResult = DevInsCompilerFacade.compile(userPrompt, context)
        val finalPrompt = compiledResult.output

        println("🔍 [KoogLLMService] 编译完成!")
        println("🔍 [KoogLLMService] 编译结果: ${if (compiledResult.isSuccess()) "成功" else "失败"}")
        println("🔍 [KoogLLMService] 命令数量: ${compiledResult.statistics.commandCount}")
        println("🔍 [KoogLLMService] 编译输出: $finalPrompt")
        if (compiledResult.hasError) {
            println("⚠️ [KoogLLMService] 编译错误: ${compiledResult.errorMessage}")
        }
        
        // 构建包含历史的 prompt
        val prompt = prompt(
            id = "chat",
            params = LLMParams(temperature = config.temperature, toolChoice = LLMParams.ToolChoice.None)
        ) {
            // 添加历史消息
            historyMessages.forEach { message ->
                when (message.role) {
                    MessageRole.USER -> user(message.content)
                    MessageRole.ASSISTANT -> assistant(message.content)
                    MessageRole.SYSTEM -> system(message.content)
                }
            }
            
            // 添加当前用户消息（编译后的）
            user(finalPrompt)
        }

        executor.executeStreaming(prompt, model)
            .cancellable()
            .collect { frame ->
                when (frame) {
                    is StreamFrame.Append -> {
                        emit(frame.text)
                    }
                    is StreamFrame.End -> {
                        // Stream ended successfully
                    }
                    is StreamFrame.ToolCall -> {
                        // Tool calls (可以后续扩展)
                    }
                }
            }
    }

    suspend fun sendPrompt(prompt: String): String {
        return try {
            val executor = createExecutor()
            
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = getModelForProvider()
            )
            
            agent.run(prompt)
        } catch (e: Exception) {
            "[Error: ${e.message}]"
        }
    }

    private fun getModelForProvider(): LLModel {
        return when (config.provider) {
            LLMProviderType.OPENAI -> {
                OpenAIModels.list()
                    .find { it.id == config.modelName }
                    ?: createDefaultModel(LLMProvider.OpenAI, 128000)
            }
            LLMProviderType.DEEPSEEK -> {
                DeepSeekModels.list()
                    .find { it.id == config.modelName }
                    ?: createDefaultModel(LLMProvider.DeepSeek, 64000)
            }
            LLMProviderType.ANTHROPIC -> {
                AnthropicModels.list()
                    .find { it.id == config.modelName }
                    ?: createDefaultModel(LLMProvider.Anthropic, 200000)
            }
            LLMProviderType.GOOGLE -> {
                GoogleModels.list()
                    .find { it.id == config.modelName }
                    ?: createDefaultModel(LLMProvider.Google, 128000)
            }
            LLMProviderType.OPENROUTER -> {
                OpenRouterModels.list()
                    .find { it.id == config.modelName }
                    ?: createDefaultModel(LLMProvider.OpenRouter, 128000)
            }
            LLMProviderType.OLLAMA -> {
                LLModel(
                    provider = LLMProvider.Ollama,
                    id = config.modelName,
                    capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
                    contextLength = 128000
                )
            }
            LLMProviderType.BEDROCK -> {
                createDefaultModel(LLMProvider.Bedrock, 128000)
            }
        }
    }

    private fun createDefaultModel(provider: LLMProvider, contextLength: Long): LLModel {
        return LLModel(
            provider = provider,
            id = config.modelName,
            capabilities = listOf(),
            contextLength = contextLength,
        )
    }


    /**
     * Create appropriate executor based on provider configuration
     */
    private fun createExecutor(): SingleLLMPromptExecutor {
        return when (config.provider) {
            LLMProviderType.OPENAI -> simpleOpenAIExecutor(config.apiKey)
            LLMProviderType.ANTHROPIC -> simpleAnthropicExecutor(config.apiKey)
            LLMProviderType.GOOGLE -> simpleGoogleAIExecutor(config.apiKey)
            LLMProviderType.DEEPSEEK -> {
                SingleLLMPromptExecutor(DeepSeekLLMClient(config.apiKey))
            }
            LLMProviderType.OLLAMA -> simpleOllamaAIExecutor(
                baseUrl = config.baseUrl.ifEmpty { "http://localhost:11434" }
            )
            LLMProviderType.OPENROUTER -> simpleOpenRouterExecutor(config.apiKey)
            LLMProviderType.BEDROCK -> {
                // Bedrock requires AWS credentials in format: accessKeyId:secretAccessKey
                val credentials = config.apiKey.split(":")
                if (credentials.size != 2) {
                    throw IllegalArgumentException("Bedrock requires API key in format: accessKeyId:secretAccessKey")
                }
                simpleBedrockExecutor(
                    awsAccessKeyId = credentials[0],
                    awsSecretAccessKey = credentials[1]
                )
            }
        }
    }

    suspend fun validateConfig(): Result<String> {
        return try {
            val response = sendPrompt("Say 'OK' if you can hear me.")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun create(config: ModelConfig): KoogLLMService {
            if (!config.isValid()) {
                throw IllegalArgumentException("Invalid model configuration: ${config.provider} requires ${if (config.provider == LLMProviderType.OLLAMA) "baseUrl and modelName" else "apiKey and modelName"}")
            }
            return KoogLLMService(config)
        }
    }
}
