package cc.unitmesh.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.devins.compiler.DevInsCompilerFacade
import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.llm.compression.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock

class KoogLLMService(
    private val config: ModelConfig,
    private val compressionConfig: CompressionConfig = CompressionConfig()
) {
    private val logger = getLogger("KoogLLMService")

    private val executor: SingleLLMPromptExecutor by lazy {
        ExecutorFactory.create(config)
    }
    
    private val model: LLModel by lazy {
        ModelRegistry.createModel(config.provider, config.modelName)
            ?: ModelRegistry.createGenericModel(config.provider, config.modelName)
    }
    
    private val compressionService: ChatCompressionService by lazy {
        ChatCompressionService(executor, model, compressionConfig)
    }
    
    // Token 追踪
    private var lastTokenInfo: TokenInfo = TokenInfo()
    private var messagesSinceLastCompression = 0
    private var hasFailedCompressionAttempt = false

    fun streamPrompt(
        userPrompt: String, 
        fileSystem: ProjectFileSystem = EmptyFileSystem(),
        historyMessages: List<Message> = emptyList(),
        compileDevIns: Boolean = true,
        onTokenUpdate: ((TokenInfo) -> Unit)? = null,
        onCompressionNeeded: ((Int, Int) -> Unit)? = null
    ): Flow<String> = flow {
        val finalPrompt = if (compileDevIns) {
            compilePrompt(userPrompt, fileSystem)
        } else {
            userPrompt
        }
        
        val prompt = buildPrompt(finalPrompt, historyMessages)
        executor.executeStreaming(prompt, model)
            .cancellable()
            .collect { frame ->
                when (frame) {
                    is StreamFrame.Append -> emit(frame.text)
                    is StreamFrame.End -> {
                        logger.debug { "StreamFrame.End -> finishReason=${frame.finishReason}, metaInfo=${frame.metaInfo}" }
                        frame.metaInfo?.let { metaInfo ->
                            lastTokenInfo = TokenInfo(
                                totalTokens = metaInfo.totalTokensCount ?: 0,
                                inputTokens = metaInfo.inputTokensCount ?: 0,
                                outputTokens = metaInfo.outputTokensCount ?: 0,
                                timestamp = Clock.System.now().toEpochMilliseconds()
                            )
                            
                            onTokenUpdate?.invoke(lastTokenInfo)
                            if (compressionConfig.autoCompressionEnabled) {
                                val maxTokens = getMaxTokens()
                                if (lastTokenInfo.needsCompression(maxTokens, compressionConfig.contextPercentageThreshold)) {
                                    onCompressionNeeded?.invoke(lastTokenInfo.inputTokens, maxTokens)
                                }
                            }
                        }
                        
                        messagesSinceLastCompression++
                    }
                    is StreamFrame.ToolCall -> { /* Tool calls (可以后续扩展) */ }
                }
            }
    }

    suspend fun sendPrompt(prompt: String): String {
        return try {
            val agent = AIAgent(promptExecutor = executor, llmModel = model)
            agent.run(prompt)
        } catch (e: Exception) {
            "[Error: ${e.message}]"
        }
    }

    private suspend fun compilePrompt(userPrompt: String, fileSystem: ProjectFileSystem): String {
        val context = CompilerContext().apply {
            this.fileSystem = fileSystem
        }

        val compiledResult = DevInsCompilerFacade.compile(userPrompt, context)

        if (compiledResult.hasError) {
            logger.warn { "⚠️ [KoogLLMService] 编译错误: ${compiledResult.errorMessage}" }
        }

        return compiledResult.output
    }

    private fun buildPrompt(finalPrompt: String, historyMessages: List<Message>) = prompt(
        id = "chat",
        params = LLMParams(
            temperature = config.temperature,
            toolChoice = LLMParams.ToolChoice.None
        )
    ) {
        historyMessages.forEach { message ->
            when (message.role) {
                MessageRole.USER -> user(message.content)
                MessageRole.ASSISTANT -> assistant(message.content)
                MessageRole.SYSTEM -> system(message.content)
            }
        }

        user(finalPrompt)
    }

    suspend fun validateConfig(): Result<String> {
        return try {
            val response = sendPrompt("Say 'OK' if you can hear me.")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 尝试压缩历史消息
     * 
     * @param historyMessages 完整的对话历史
     * @param force 是否强制压缩（忽略阈值和失败记录）
     * @return 压缩结果
     */
    suspend fun tryCompressHistory(
        historyMessages: List<Message>,
        force: Boolean = false
    ): CompressionResult {
        // 如果之前压缩失败且消息数量不足，跳过
        if (!force && hasFailedCompressionAttempt && 
            messagesSinceLastCompression < compressionConfig.retryAfterMessages) {
            return CompressionResult(
                newMessages = null,
                info = ChatCompressionInfo(
                    originalTokenCount = lastTokenInfo.inputTokens,
                    newTokenCount = lastTokenInfo.inputTokens,
                    compressionStatus = CompressionStatus.NOOP,
                    errorMessage = "等待更多消息后再重试压缩"
                )
            )
        }
        
        val maxTokens = getMaxTokens()
        val result = compressionService.compress(
            messages = historyMessages,
            currentTokenCount = lastTokenInfo.inputTokens,
            maxTokens = maxTokens,
            force = force
        )
        
        // 更新状态
        when (result.info.compressionStatus) {
            CompressionStatus.COMPRESSED -> {
                hasFailedCompressionAttempt = false
                messagesSinceLastCompression = 0
                // 更新 token 信息
                lastTokenInfo = lastTokenInfo.copy(
                    inputTokens = result.info.newTokenCount
                )
            }
            CompressionStatus.COMPRESSION_FAILED_INFLATED_TOKEN_COUNT,
            CompressionStatus.COMPRESSION_FAILED_TOKEN_COUNT_ERROR,
            CompressionStatus.COMPRESSION_FAILED_ERROR -> {
                hasFailedCompressionAttempt = !force
                messagesSinceLastCompression = 0
            }
            CompressionStatus.NOOP -> {
                // 无操作
            }
        }
        
        return result
    }
    
    /**
     * 获取最后的 token 信息
     */
    fun getLastTokenInfo(): TokenInfo = lastTokenInfo
    
    /**
     * 获取模型的最大 token 数
     */
    fun getMaxTokens(): Int {
        // 优先使用模型自带的 maxTokens
        return (model.maxOutputTokens as? Int) ?: config.maxTokens
    }
    
    /**
     * 重置压缩状态
     */
    fun resetCompressionState() {
        hasFailedCompressionAttempt = false
        messagesSinceLastCompression = 0
    }

    companion object {
        fun create(
            config: ModelConfig, 
            compressionConfig: CompressionConfig = CompressionConfig()
        ): KoogLLMService {
            require(config.isValid()) {
                val requirement = if (config.provider == LLMProviderType.OLLAMA) {
                    "baseUrl and modelName"
                } else {
                    "apiKey and modelName"
                }
                "Invalid model configuration: ${config.provider} requires $requirement"
            }
            return KoogLLMService(config, compressionConfig)
        }
    }
}
