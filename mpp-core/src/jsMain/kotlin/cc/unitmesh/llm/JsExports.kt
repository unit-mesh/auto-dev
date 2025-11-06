@file:JsExport

package cc.unitmesh.llm

import cc.unitmesh.agent.CodingAgentContext
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.impl.GrepParams
import cc.unitmesh.agent.tool.impl.ShellParams
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.shell.JsShellExecutor
import cc.unitmesh.devins.compiler.DevInsCompilerFacade
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.llm.Message
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.completion.CompletionManager
import cc.unitmesh.devins.completion.CompletionContext
import cc.unitmesh.devins.completion.CompletionTriggerType
import cc.unitmesh.devins.completion.CompletionItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.Promise

/**
 * JavaScript-friendly wrapper for KoogLLMService
 * This class is exported to JavaScript and provides a simpler API
 */
@JsExport
class JsKoogLLMService(config: JsModelConfig, compressionConfig: JsCompressionConfig? = null) {
    private val kotlinConfig: ModelConfig
    internal val service: KoogLLMService  // 改为 internal 以便在同一模块访问

    init {
        // Convert string provider to LLMProviderType
        val provider = when (config.providerName.uppercase()) {
            "OPENAI" -> LLMProviderType.OPENAI
            "ANTHROPIC" -> LLMProviderType.ANTHROPIC
            "GOOGLE" -> LLMProviderType.GOOGLE
            "DEEPSEEK" -> LLMProviderType.DEEPSEEK
            "OLLAMA" -> LLMProviderType.OLLAMA
            "OPENROUTER" -> LLMProviderType.OPENROUTER
            else -> throw IllegalArgumentException("Unknown provider: ${config.providerName}")
        }

        kotlinConfig = ModelConfig(
            provider = provider,
            modelName = config.modelName,
            apiKey = config.apiKey,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            baseUrl = config.baseUrl
        )

        val kotlinCompressionConfig = compressionConfig?.toKotlin()
            ?: cc.unitmesh.llm.compression.CompressionConfig()

        service = KoogLLMService(kotlinConfig, kotlinCompressionConfig)
    }
    
    /**
     * Stream a prompt and return a Promise that resolves when streaming completes
     * @param userPrompt The user's prompt
     * @param historyMessages Previous conversation messages
     * @param onChunk Callback for each chunk of text received
     * @param onError Callback for errors
     * @param onComplete Callback when streaming completes
     * @param compileDevIns Whether to compile DevIns code (default true, should be false for agent calls)
     */
    @JsName("streamPrompt")
    fun streamPrompt(
        userPrompt: String,
        historyMessages: Array<JsMessage> = emptyArray(),
        onChunk: (String) -> Unit,
        onError: ((Throwable) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        compileDevIns: Boolean = true
    ): Promise<Unit> {
        return Promise { resolve, reject ->
            GlobalScope.launch {
                try {
                    val messages = historyMessages.map { it.toKotlinMessage() }
                    service.streamPrompt(userPrompt, EmptyFileSystem(), messages, compileDevIns)
                        .catch { error ->
                            onError?.invoke(error)
                            reject(error)
                        }
                        .collect { chunk ->
                            onChunk(chunk)
                        }
                    onComplete?.invoke()
                    resolve(Unit)
                } catch (e: Throwable) {
                    onError?.invoke(e)
                    reject(e)
                }
            }
        }
    }
    
    /**
     * Send a prompt and get complete response (non-streaming)
     * Simple wrapper for SubAgents that only need basic prompt->response
     * @param prompt The complete prompt (includes system + user prompt)
     * @return Promise that resolves to the complete response text
     */
    @JsName("sendPrompt")
    fun sendPrompt(prompt: String): Promise<String> {
        return GlobalScope.promise {
            try {
                service.sendPrompt(prompt)
            } catch (e: Throwable) {
                "[Error: ${e.message}]"
            }
        }
    }

    /**
     * Get the maximum tokens for this model
     */
    @JsName("getMaxTokens")
    fun getMaxTokens(): Int {
        return service.getMaxTokens()
    }

    /**
     * Get the last token information
     */
    @JsName("getLastTokenInfo")
    fun getLastTokenInfo(): JsTokenInfo {
        return JsTokenInfo.fromKotlin(service.getLastTokenInfo())
    }

    /**
     * Reset compression state
     */
    @JsName("resetCompressionState")
    fun resetCompressionState() {
        service.resetCompressionState()
    }

    // Note: suspend functions cannot be exported to JS directly
    // They need to be called from Kotlin coroutines
    // JavaScript code should use streamPrompt() instead

    companion object {
        /**
         * Create a service with validation
         */
        @JsName("create")
        fun create(config: JsModelConfig, compressionConfig: JsCompressionConfig? = null): JsKoogLLMService {
            return JsKoogLLMService(config, compressionConfig)
        }
    }
}

/**
 * JavaScript-friendly model configuration
 * Uses string for provider to avoid enum export issues
 */
@JsExport
data class JsModelConfig(
    val providerName: String,  // "OPENAI", "ANTHROPIC", "GOOGLE", "DEEPSEEK", "OLLAMA", "OPENROUTER"
    val modelName: String,
    val apiKey: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 8192,
    val baseUrl: String = ""
) {
    fun toKotlinConfig(): ModelConfig {
        val provider = when (providerName.uppercase()) {
            "OPENAI" -> LLMProviderType.OPENAI
            "ANTHROPIC" -> LLMProviderType.ANTHROPIC
            "GOOGLE" -> LLMProviderType.GOOGLE
            "DEEPSEEK" -> LLMProviderType.DEEPSEEK
            "OLLAMA" -> LLMProviderType.OLLAMA
            "OPENROUTER" -> LLMProviderType.OPENROUTER
            else -> throw IllegalArgumentException("Unknown provider: $providerName")
        }
        
        return ModelConfig(
            provider = provider,
            modelName = modelName,
            apiKey = apiKey,
            temperature = temperature,
            maxTokens = maxTokens,
            baseUrl = baseUrl
        )
    }
}

/**
 * JavaScript-friendly message
 */
@JsExport
data class JsMessage(
    val role: String,  // "user", "assistant", or "system"
    val content: String
) {
    fun toKotlinMessage(): Message {
        val messageRole = when (role.lowercase()) {
            "user" -> MessageRole.USER
            "assistant" -> MessageRole.ASSISTANT
            "system" -> MessageRole.SYSTEM
            else -> MessageRole.USER
        }
        return Message(messageRole, content)
    }
}

/**
 * JavaScript-friendly NamedModelConfig
 */
@JsExport
data class JsNamedModelConfig(
    val name: String,
    val providerName: String,
    val modelName: String,
    val apiKey: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 8192,
    val baseUrl: String = ""
) {
    fun toKotlin(): NamedModelConfig {
        val provider = when (providerName.uppercase()) {
            "OPENAI" -> LLMProviderType.OPENAI
            "ANTHROPIC" -> LLMProviderType.ANTHROPIC
            "GOOGLE" -> LLMProviderType.GOOGLE
            "DEEPSEEK" -> LLMProviderType.DEEPSEEK
            "OLLAMA" -> LLMProviderType.OLLAMA
            "OPENROUTER" -> LLMProviderType.OPENROUTER
            else -> throw IllegalArgumentException("Unknown provider: $providerName")
        }
        
        return NamedModelConfig(
            name = name,
            provider = provider,
            modelName = modelName,
            apiKey = apiKey,
            temperature = temperature,
            maxTokens = maxTokens,
            baseUrl = baseUrl
        )
    }
    
    companion object {
        @JsName("fromKotlin")
        fun fromKotlin(config: NamedModelConfig): JsNamedModelConfig {
            return JsNamedModelConfig(
                name = config.name,
                providerName = config.provider.name,
                modelName = config.modelName,
                apiKey = config.apiKey,
                temperature = config.temperature,
                maxTokens = config.maxTokens,
                baseUrl = config.baseUrl
            )
        }
    }
}

/**
 * JavaScript-friendly ConfigFile
 */
@JsExport
data class JsConfigFile(
    val active: String,
    val configs: Array<JsNamedModelConfig>
) {
    fun toKotlin(): ConfigFile {
        return ConfigFile(
            active = active,
            configs = configs.map { it.toKotlin() }
        )
    }
    
    companion object {
        @JsName("fromKotlin")
        fun fromKotlin(configFile: ConfigFile): JsConfigFile {
            return JsConfigFile(
                active = configFile.active,
                configs = configFile.configs.map { JsNamedModelConfig.fromKotlin(it) }.toTypedArray()
            )
        }
        
        @JsName("empty")
        fun empty(): JsConfigFile {
            return JsConfigFile(active = "", configs = emptyArray())
        }
    }
}

/**
 * JavaScript-friendly result wrapper
 */
@JsExport
data class JsResult(
    val success: Boolean,
    val value: String,
    val error: String?
)

/**
 * Helper object to get available models for a provider
 */
@JsExport
object JsModelRegistry {
    @JsName("getAvailableModels")
    fun getAvailableModels(providerName: String): Array<String> {
        val provider = when (providerName.uppercase()) {
            "OPENAI" -> LLMProviderType.OPENAI
            "ANTHROPIC" -> LLMProviderType.ANTHROPIC
            "GOOGLE" -> LLMProviderType.GOOGLE
            "DEEPSEEK" -> LLMProviderType.DEEPSEEK
            "OLLAMA" -> LLMProviderType.OLLAMA
            "OPENROUTER" -> LLMProviderType.OPENROUTER
            else -> throw IllegalArgumentException("Unknown provider: $providerName")
        }
        return ModelRegistry.getAvailableModels(provider).toTypedArray()
    }
    
    @JsName("getAllProviders")
    fun getAllProviders(): Array<String> {
        return arrayOf("OPENAI", "ANTHROPIC", "GOOGLE", "DEEPSEEK", "OLLAMA", "OPENROUTER")
    }
}

/**
 * JavaScript-friendly completion manager
 * Provides auto-completion for @agent, /command, $variable, etc.
 */
@JsExport
class JsCompletionManager {
    private val manager = CompletionManager()
    
    // Cache completions for insert operation
    private var cachedCompletions: List<CompletionItem> = emptyList()
    private var cachedContext: CompletionContext? = null
    
    /**
     * Initialize workspace for file path completion
     * @param workspacePath The root path of the workspace
     * @return Promise that resolves when workspace is initialized
     */
    @JsName("initWorkspace")
    fun initWorkspace(workspacePath: String): Promise<Boolean> {
        return GlobalScope.promise {
            try {
                cc.unitmesh.devins.workspace.WorkspaceManager.openWorkspace(
                    name = "CLI Workspace",
                    rootPath = workspacePath
                )
                true
            } catch (e: Exception) {
                console.error("Failed to initialize workspace: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Get completion suggestions based on text and cursor position
     * @param text Full input text
     * @param cursorPosition Current cursor position (0-indexed)
     * @return Array of completion items
     */
    @JsName("getCompletions")
    fun getCompletions(text: String, cursorPosition: Int): Array<JsCompletionItem> {
        // Look for the most recent trigger character before the cursor
        var triggerOffset = -1
        var triggerType: CompletionTriggerType? = null
        
        // Search backwards from cursor for a trigger character
        // Stop at whitespace or newline
        for (i in (cursorPosition - 1) downTo 0) {
            val char = text[i]
            when (char) {
                ' ', '\n' -> {
                    // Stop if we hit whitespace - no trigger found
                    break
                }
                '@' -> {
                    triggerOffset = i
                    triggerType = CompletionTriggerType.AGENT
                    break
                }
                '/' -> {
                    triggerOffset = i
                    triggerType = CompletionTriggerType.COMMAND
                    break
                }
                '$' -> {
                    triggerOffset = i
                    triggerType = CompletionTriggerType.VARIABLE
                    break
                }
                ':' -> {
                    // ':' is COMMAND_VALUE trigger, but only if not preceded by space
                    // Check if this is after a command (like "/read-file:")
                    triggerOffset = i
                    triggerType = CompletionTriggerType.COMMAND_VALUE
                    break
                }
            }
        }
        // No trigger found
        if (triggerOffset < 0 || triggerType == null) return emptyArray()
        
        // Extract query text (text after trigger up to cursor)
        val queryText = text.substring(triggerOffset + 1, cursorPosition)
        
        // Check if query is valid (no whitespace or newlines in the middle)
        // Exception: COMMAND_VALUE can have empty query (e.g., "/read-file:")
        if (triggerType != CompletionTriggerType.COMMAND_VALUE) {
            if (queryText.contains('\n') || queryText.contains(' ')) {
                return emptyArray()
            }
        }
        
        val context = CompletionContext(
            fullText = text,
            cursorPosition = cursorPosition,
            triggerType = triggerType,
            triggerOffset = triggerOffset,
            queryText = queryText
        )
        
        val items = manager.getFilteredCompletions(context)
        
        // Cache for later use in applyCompletion
        cachedCompletions = items
        cachedContext = context
        
        return items.mapIndexed { index, item -> item.toJsItem(triggerType, index) }.toTypedArray()
    }
    
    /**
     * Apply a completion by index (from the last getCompletions call)
     * This properly handles insert handlers and triggers next completion if needed
     * 
     * @param text Current full text
     * @param cursorPosition Current cursor position
     * @param completionIndex Index of the completion item to apply
     * @return Insert result with new text, cursor position, and trigger flag
     */
    @JsName("applyCompletion")
    fun applyCompletion(text: String, cursorPosition: Int, completionIndex: Int): JsInsertResult? {
        val context = cachedContext ?: return null
        if (completionIndex < 0 || completionIndex >= cachedCompletions.size) return null
        
        val item = cachedCompletions[completionIndex]
        val result = item.defaultInsert(text, cursorPosition)
        
        return JsInsertResult(
            newText = result.newText,
            newCursorPosition = result.newCursorPosition,
            shouldTriggerNextCompletion = result.shouldTriggerNextCompletion
        )
    }
    
    /**
     * Check if a character should trigger completion
     */
    @JsName("shouldTrigger")
    fun shouldTrigger(char: String): Boolean {
        if (char.isEmpty()) return false
        val c = char[0]
        return c in setOf('@', '/', '$', ':')
    }
    
    /**
     * Get supported trigger types
     */
    @JsName("getSupportedTriggers")
    fun getSupportedTriggers(): Array<String> {
        return arrayOf("@", "/", "$", ":")
    }
}

/**
 * JavaScript-friendly completion item
 */
@JsExport
data class JsCompletionItem(
    val text: String,
    val displayText: String,
    val description: String?,
    val icon: String?,
    val triggerType: String,  // "AGENT", "COMMAND", "VARIABLE", "COMMAND_VALUE"
    val index: Int  // Index for applyCompletion
)

/**
 * JavaScript-friendly insert result
 */
@JsExport
data class JsInsertResult(
    val newText: String,
    val newCursorPosition: Int,
    val shouldTriggerNextCompletion: Boolean
)

/**
 * JavaScript-friendly DevIns compiler wrapper
 * Compiles DevIns code (e.g., "/read-file:path") and returns the result
 */
@JsExport
class JsDevInsCompiler {
    
    /**
     * Compile DevIns source code and return the result
     * @param source DevIns source code (e.g., "解释代码 /read-file:build.gradle.kts")
     * @param variables Optional variables map
     * @return Promise with compilation result
     */
    @JsName("compile")
    fun compile(source: String, variables: dynamic = null): Promise<JsDevInsResult> {
        return GlobalScope.promise {
            try {
                val varsMap = if (variables != null && jsTypeOf(variables) == "object") {
                    val map = mutableMapOf<String, Any>()
                    js("for (var key in variables) { map[key] = variables[key]; }")
                    map
                } else {
                    emptyMap()
                }
                
                val result = DevInsCompilerFacade.compile(source, varsMap)
                
                JsDevInsResult(
                    success = result.isSuccess(),
                    output = result.output,
                    errorMessage = result.errorMessage,
                    hasCommand = result.statistics.commandCount > 0
                )
            } catch (e: Exception) {
                JsDevInsResult(
                    success = false,
                    output = "",
                    errorMessage = e.message ?: "Unknown error",
                    hasCommand = false
                )
            }
        }
    }
    
    /**
     * Compile DevIns source and return just the output string
     * @param source DevIns source code
     * @return Promise with output string
     */
    @JsName("compileToString")
    fun compileToString(source: String): Promise<String> {
        return GlobalScope.promise {
            try {
                DevInsCompilerFacade.compileToString(source)
            } catch (e: Exception) {
                throw e
            }
        }
    }
}

/**
 * JavaScript-friendly DevIns compilation result
 */
@JsExport
data class JsDevInsResult(
    val success: Boolean,
    val output: String,
    val errorMessage: String?,
    val hasCommand: Boolean
)

/**
 * Extension to convert CompletionItem to JsCompletionItem
 */
private fun CompletionItem.toJsItem(triggerType: CompletionTriggerType, index: Int): JsCompletionItem {
    val triggerTypeStr = when (triggerType) {
        CompletionTriggerType.AGENT -> "AGENT"
        CompletionTriggerType.COMMAND -> "COMMAND"
        CompletionTriggerType.VARIABLE -> "VARIABLE"
        CompletionTriggerType.COMMAND_VALUE -> "COMMAND_VALUE"
        CompletionTriggerType.CODE_FENCE -> "CODE_FENCE"
        CompletionTriggerType.NONE -> "NONE"
    }

    return JsCompletionItem(
        text = this.text,
        displayText = this.displayText,
        description = this.description,
        icon = this.icon,
        triggerType = triggerTypeStr,
        index = index
    )
}

/**
 * JavaScript-friendly Tool Registry wrapper
 * Provides access to built-in tools like read-file, write-file, glob, grep, shell
 */
@JsExport
class JsToolRegistry(projectPath: String) {
    private val registry: ToolRegistry

    init {
        val fileSystem = DefaultToolFileSystem(projectPath)
        val shellExecutor = JsShellExecutor()
        registry = ToolRegistry(fileSystem, shellExecutor)
    }

    /**
     * Execute read-file tool
     */
    @JsName("readFile")
    fun readFile(path: String, startLine: Int? = null, endLine: Int? = null): Promise<JsToolResult> {
        return GlobalScope.promise {
            try {
                val params = cc.unitmesh.agent.tool.impl.ReadFileParams(
                    path = path,
                    startLine = startLine,
                    endLine = endLine
                )
                val result = registry.executeTool(ToolType.ReadFile.name, params)
                result.toJsToolResult()
            } catch (e: Exception) {
                JsToolResult(
                    success = false,
                    output = "",
                    errorMessage = e.message ?: "Unknown error",
                    metadata = emptyMap()
                )
            }
        }
    }

    /**
     * Execute write-file tool
     */
    @JsName("writeFile")
    fun writeFile(path: String, content: String, createDirectories: Boolean = true): Promise<JsToolResult> {
        return GlobalScope.promise {
            try {
                val params = cc.unitmesh.agent.tool.impl.WriteFileParams(
                    path = path,
                    content = content,
                    createDirectories = createDirectories
                )
                val result = registry.executeTool(ToolType.WriteFile.name, params)
                result.toJsToolResult()
            } catch (e: Exception) {
                JsToolResult(
                    success = false,
                    output = "",
                    errorMessage = e.message ?: "Unknown error",
                    metadata = emptyMap()
                )
            }
        }
    }

    /**
     * Execute glob tool
     */
    @JsName("glob")
    fun glob(pattern: String, path: String = ".", includeFileInfo: Boolean = false): Promise<JsToolResult> {
        return GlobalScope.promise {
            try {
                val params = cc.unitmesh.agent.tool.impl.GlobParams(
                    pattern = pattern,
                    path = path,
                    includeFileInfo = includeFileInfo
                )
                val result = registry.executeTool(ToolType.Glob.name, params)
                result.toJsToolResult()
            } catch (e: Exception) {
                JsToolResult(
                    success = false,
                    output = "",
                    errorMessage = e.message ?: "Unknown error",
                    metadata = emptyMap()
                )
            }
        }
    }

    /**
     * Execute grep tool
     */
    @JsName("grep")
    fun grep(
        pattern: String,
        path: String = ".",
        include: String? = null,
        exclude: String? = null,
        recursive: Boolean = true,
        caseSensitive: Boolean = true
    ): Promise<JsToolResult> {
        return GlobalScope.promise {
            try {
                val params = GrepParams(
                    pattern = pattern,
                    path = path,
                    include = include,
                    exclude = exclude,
                    recursive = recursive,
                    caseSensitive = caseSensitive
                )
                val result = registry.executeTool(ToolType.Grep.name, params)
                result.toJsToolResult()
            } catch (e: Exception) {
                JsToolResult(
                    success = false,
                    output = "",
                    errorMessage = e.message ?: "Unknown error",
                    metadata = emptyMap()
                )
            }
        }
    }

    /**
     * Execute shell tool
     */
    @JsName("shell")
    fun shell(
        command: String,
        workingDirectory: String? = null,
        timeoutMs: Double = 30000.0
    ): Promise<JsToolResult> {
        return GlobalScope.promise {
            try {
                val params = ShellParams(
                    command = command,
                    workingDirectory = workingDirectory,
                    timeoutMs = timeoutMs.toLong()
                )
                val result = registry.executeTool(ToolType.Shell.name, params)
                result.toJsToolResult()
            } catch (e: Exception) {
                JsToolResult(
                    success = false,
                    output = "",
                    errorMessage = e.message ?: "Unknown error",
                    metadata = emptyMap()
                )
            }
        }
    }

    /**
     * Get list of available tools
     */
    @JsName("getAvailableTools")
    fun getAvailableTools(): Array<String> {
        return registry.getToolNames().toTypedArray()
    }

    /**
     * Get all tools as agent tools
     */
    @JsName("getAgentTools")
    fun getAgentTools(): Array<JsAgentTool> {
        return registry.getAgentTools().map { tool ->
            JsAgentTool(
                name = tool.name,
                description = tool.description,
                example = tool.example
            )
        }.toTypedArray()
    }

    /**
     * Format tool list for AI consumption (similar to CodingAgentContext.formatToolListForAI)
     */
    @JsName("formatToolListForAI")
    fun formatToolListForAI(): String {
        val tools = registry.getAllTools().values.toList()
        return CodingAgentContext.formatToolListForAI(tools)
    }
}

/**
 * JavaScript-friendly tool result
 */
@JsExport
data class JsToolResult(
    val success: Boolean,
    val output: String,
    val errorMessage: String?,
    val metadata: Map<String, String>
)

/**
 * JavaScript-friendly agent tool
 */
@JsExport
data class JsAgentTool(
    val name: String,
    val description: String,
    val example: String
)

/**
 * Extension to convert ToolResult to JsToolResult
 */
private fun cc.unitmesh.agent.tool.ToolResult.toJsToolResult(): JsToolResult {
    return when (this) {
        is cc.unitmesh.agent.tool.ToolResult.Success -> JsToolResult(
            success = true,
            output = this.content,
            errorMessage = null,
            metadata = this.metadata
        )
        is cc.unitmesh.agent.tool.ToolResult.Error -> JsToolResult(
            success = false,
            output = "",
            errorMessage = this.message,
            metadata = emptyMap()
        )
        is cc.unitmesh.agent.tool.ToolResult.AgentResult -> JsToolResult(
            success = this.success,
            output = this.content,
            errorMessage = if (!this.success) "Agent execution failed" else null,
            metadata = this.metadata
        )
    }
}

// ============================================================================
// 压缩功能相关的 JS 导出
// ============================================================================

/**
 * JavaScript-friendly wrapper for CompressionConfig
 */
@JsExport
class JsCompressionConfig(
    val contextPercentageThreshold: Double = 0.7,
    val preserveRecentRatio: Double = 0.3,
    val autoCompressionEnabled: Boolean = true,
    val retryAfterMessages: Int = 5
) {
    internal fun toKotlin(): cc.unitmesh.llm.compression.CompressionConfig {
        return cc.unitmesh.llm.compression.CompressionConfig(
            contextPercentageThreshold = contextPercentageThreshold,
            preserveRecentRatio = preserveRecentRatio,
            autoCompressionEnabled = autoCompressionEnabled,
            retryAfterMessages = retryAfterMessages
        )
    }
}

/**
 * JavaScript-friendly wrapper for TokenInfo
 */
@JsExport
class JsTokenInfo(
    val totalTokens: Int = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val timestamp: Double = 0.0
) {
    @JsName("getUsagePercentage")
    fun getUsagePercentage(maxTokens: Int): Double {
        if (maxTokens <= 0) return 0.0
        return (inputTokens.toDouble() / maxTokens.toDouble()) * 100.0
    }

    @JsName("needsCompression")
    fun needsCompression(maxTokens: Int, threshold: Double): Boolean {
        if (maxTokens <= 0) return false
        val usage = inputTokens.toDouble() / maxTokens.toDouble()
        return usage >= threshold
    }

    internal fun toKotlin(): cc.unitmesh.llm.compression.TokenInfo {
        return cc.unitmesh.llm.compression.TokenInfo(
            totalTokens = totalTokens,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            timestamp = timestamp.toLong()
        )
    }

    companion object {
        internal fun fromKotlin(tokenInfo: cc.unitmesh.llm.compression.TokenInfo): JsTokenInfo {
            return JsTokenInfo(
                totalTokens = tokenInfo.totalTokens,
                inputTokens = tokenInfo.inputTokens,
                outputTokens = tokenInfo.outputTokens,
                timestamp = tokenInfo.timestamp.toDouble()
            )
        }
    }
}

/**
 * JavaScript-friendly wrapper for ChatCompressionInfo
 */
@JsExport
class JsChatCompressionInfo(
    val originalTokenCount: Int,
    val newTokenCount: Int,
    val compressionStatus: String,
    val errorMessage: String? = null
) {
    @JsName("getCompressionRatio")
    fun getCompressionRatio(): Double {
        return if (originalTokenCount > 0) {
            1.0 - (newTokenCount.toDouble() / originalTokenCount.toDouble())
        } else {
            0.0
        }
    }

    @JsName("getTokensSaved")
    fun getTokensSaved(): Int {
        return originalTokenCount - newTokenCount
    }

    companion object {
        internal fun fromKotlin(info: cc.unitmesh.llm.compression.ChatCompressionInfo): JsChatCompressionInfo {
            return JsChatCompressionInfo(
                originalTokenCount = info.originalTokenCount,
                newTokenCount = info.newTokenCount,
                compressionStatus = info.compressionStatus.name,
                errorMessage = info.errorMessage
            )
        }
    }
}

// ============================================================================
// Domain Dictionary Generation JS Exports
// ============================================================================

/**
 * JavaScript-friendly wrapper for DomainDictGenerator
 */
@JsExport
class JsDomainDictGenerator(
    private val projectPath: String,
    private val modelConfig: JsModelConfig,
    private val maxTokenLength: Int = 8192
) {
    private val fileSystem = cc.unitmesh.devins.filesystem.DefaultProjectFileSystem(projectPath)
    private val generator = cc.unitmesh.indexer.DomainDictGenerator(
        fileSystem = fileSystem,
        modelConfig = ModelConfig(
            provider = when (this.modelConfig.providerName.lowercase()) {
                "deepseek" -> LLMProviderType.DEEPSEEK
                "openai" -> LLMProviderType.OPENAI
                "anthropic" -> LLMProviderType.ANTHROPIC
                "ollama" -> LLMProviderType.OLLAMA
                else -> LLMProviderType.DEEPSEEK
            },
            modelName = this.modelConfig.modelName,
            apiKey = this.modelConfig.apiKey,
            temperature = this.modelConfig.temperature,
            maxTokens = this.modelConfig.maxTokens,
            baseUrl = this.modelConfig.baseUrl
        ),
        maxTokenLength = maxTokenLength
    )

    /**
     * Generate domain dictionary and return complete result
     */
    @JsName("generate")
    fun generate(): Promise<String> {
        return GlobalScope.promise {
            try {
                generator.generate()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Generate and save domain dictionary to file
     */
    @JsName("generateAndSave")
    fun generateAndSave(): Promise<JsDomainDictResult> {
        return GlobalScope.promise {
            try {
                val result = generator.generateAndSave()
                when (result) {
                    is cc.unitmesh.indexer.GenerationResult.Success ->
                        JsDomainDictResult(true, result.content, null)
                    is cc.unitmesh.indexer.GenerationResult.Error ->
                        JsDomainDictResult(false, "", result.message)
                }
            } catch (e: Exception) {
                JsDomainDictResult(false, "", e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Check if domain dictionary file exists
     */
    @JsName("exists")
    fun exists(): Promise<Boolean> {
        return GlobalScope.promise {
            try {
                fileSystem.exists("prompts/domain.csv")
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Load existing domain dictionary content
     */
    @JsName("loadContent")
    fun loadContent(): Promise<String?> {
        return GlobalScope.promise {
            try {
                val service = cc.unitmesh.indexer.DomainDictService(fileSystem)
                service.loadContent()
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * JavaScript-friendly result for domain dictionary generation
 */
@JsExport
data class JsDomainDictResult(
    val success: Boolean,
    val content: String,
    val errorMessage: String?
)

