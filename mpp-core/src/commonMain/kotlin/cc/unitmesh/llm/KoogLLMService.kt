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

/**
 * Koog LLM 服务 - 负责处理 LLM 请求和响应
 * 职责：
 * 1. 管理 LLM 流式和非流式调用
 * 2. 集成 DevIns 编译器
 * 3. 处理多轮对话历史
 * 4. 提供配置验证功能
 */
class KoogLLMService(private val config: ModelConfig) {
    private val executor: SingleLLMPromptExecutor by lazy { 
        ExecutorFactory.create(config)
    }
    
    private val model: LLModel by lazy {
        ModelRegistry.createModel(config.provider, config.modelName)
            ?: ModelRegistry.createGenericModel(config.provider, config.modelName)
    }
    /**
     * 流式发送提示，支持 DevIns 编译、SpecKit 命令和多轮对话
     * @param userPrompt 用户输入的提示文本（可以包含 DevIns 语法和命令）
     * @param fileSystem 项目文件系统，用于支持 SpecKit 等命令（可选）
     * @param historyMessages 历史消息列表，用于多轮对话（可选）
     */
    /**
     * 流式发送提示，支持 DevIns 编译、SpecKit 命令和多轮对话
     * @param userPrompt 用户输入的提示文本（可以包含 DevIns 语法和命令）
     * @param fileSystem 项目文件系统，用于支持 SpecKit 等命令（可选）
     * @param historyMessages 历史消息列表，用于多轮对话（可选）
     * @param compileDevIns 是否编译 DevIns 代码（默认 true，Agent 调用时应设为 false）
     */
    fun streamPrompt(
        userPrompt: String, 
        fileSystem: ProjectFileSystem = EmptyFileSystem(),
        historyMessages: List<Message> = emptyList(),
        compileDevIns: Boolean = true
    ): Flow<String> = flow {
        // 只在需要时编译 DevIns 脚本
        val finalPrompt = if (compileDevIns) {
            compilePrompt(userPrompt, fileSystem)
        } else {
            userPrompt
        }
        
        // 构建包含历史的 prompt
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

    /**
     * 发送非流式提示
     */
    suspend fun sendPrompt(prompt: String): String {
        return try {
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = model
            )
            agent.run(prompt)
        } catch (e: Exception) {
            "[Error: ${e.message}]"
        }
    }

    /**
     * 编译 DevIns 提示文本
     */
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

    /**
     * 构建包含历史消息的 Prompt
     */
    private fun buildPrompt(finalPrompt: String, historyMessages: List<Message>) = prompt(
        id = "chat",
        params = LLMParams(
            temperature = config.temperature,
            toolChoice = LLMParams.ToolChoice.None
        )
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
