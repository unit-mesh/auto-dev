package cc.unitmesh.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import cc.unitmesh.devins.compiler.DevInsCompilerFacade
import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow

class KoogLLMService(private val config: ModelConfig) {
    private val executor: SingleLLMPromptExecutor by lazy { 
        ExecutorFactory.create(config)
    }
    
    private val model: LLModel by lazy {
        ModelRegistry.createModel(config.provider, config.modelName)
            ?: ModelRegistry.createGenericModel(config.provider, config.modelName)
    }

    fun streamPrompt(
        userPrompt: String, 
        fileSystem: ProjectFileSystem = EmptyFileSystem(),
        historyMessages: List<Message> = emptyList(),
        compileDevIns: Boolean = true
    ): Flow<String> = flow {
        val finalPrompt = if (compileDevIns) {
            compilePrompt(userPrompt, fileSystem)
        } else {
            userPrompt
        }
        
        val prompt = buildPrompt(finalPrompt, historyMessages)

        // 执行流式调用
        executor.executeStreaming(prompt, model)
            .cancellable()
            .collect { frame ->
                when (frame) {
                    is StreamFrame.Append -> emit(frame.text)
                    is StreamFrame.End -> { /* Stream ended successfully */ }
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
            println("⚠️ [KoogLLMService] 编译错误: ${compiledResult.errorMessage}")
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

    /**
     * 验证配置是否可用
     */
    suspend fun validateConfig(): Result<String> {
        return try {
            val response = sendPrompt("Say 'OK' if you can hear me.")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        /**
         * 创建 KoogLLMService 实例（带配置验证）
         */
        fun create(config: ModelConfig): KoogLLMService {
            require(config.isValid()) {
                val requirement = if (config.provider == LLMProviderType.OLLAMA) {
                    "baseUrl and modelName"
                } else {
                    "apiKey and modelName"
                }
                "Invalid model configuration: ${config.provider} requires $requirement"
            }
            return KoogLLMService(config)
        }
    }
}
