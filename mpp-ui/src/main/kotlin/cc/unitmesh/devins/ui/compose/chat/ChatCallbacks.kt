package cc.unitmesh.devins.ui.compose.chat

import cc.unitmesh.devins.compiler.DevInsCompilerFacade
import cc.unitmesh.devins.compiler.context.CompilerContext
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.devins.llm.ChatHistoryManager
import cc.unitmesh.devins.llm.KoogLLMService
import cc.unitmesh.devins.ui.compose.editor.model.EditorCallbacks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 创建聊天回调
 */
fun createChatCallbacks(
    fileSystem: ProjectFileSystem,
    llmService: KoogLLMService?,
    chatHistoryManager: ChatHistoryManager,
    scope: CoroutineScope,
    onCompilerOutput: (String) -> Unit,
    onLLMOutput: (String) -> Unit,
    onProcessingChange: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onConfigWarning: () -> Unit
): EditorCallbacks {
    return object : EditorCallbacks {
        override fun onSubmit(text: String) {
            if (llmService == null) {
                onConfigWarning()
                return
            }
            
            // 编译 DevIns
            compileDevIns(text, fileSystem, scope, onCompilerOutput)
            
            // 发送到 LLM
            sendToLLM(
                text = text,
                fileSystem = fileSystem,
                llmService = llmService,
                chatHistoryManager = chatHistoryManager,
                scope = scope,
                onLLMOutput = onLLMOutput,
                onProcessingChange = onProcessingChange,
                onError = onError
            )
        }
    }
}

/**
 * 编译 DevIns 代码
 */
private fun compileDevIns(
    text: String,
    fileSystem: ProjectFileSystem,
    scope: CoroutineScope,
    onResult: (String) -> Unit
) {
    scope.launch {
        try {
            val result = withContext(Dispatchers.IO) {
                val context = CompilerContext().apply {
                    this.fileSystem = fileSystem
                }
                DevInsCompilerFacade.compile(text, context)
            }
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess()) {
                    onResult(buildString {
                        appendLine("✅ 编译成功!")
                        appendLine()
                        appendLine("输出:")
                        appendLine(result.output)
                        appendLine()
                        appendLine("统计:")
                        appendLine("- 变量: ${result.statistics.variableCount}")
                        appendLine("- 命令: ${result.statistics.commandCount}")
                        appendLine("- Agent: ${result.statistics.agentCount}")
                    })
                } else {
                    onResult("❌ 编译失败: ${result.errorMessage}")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult("❌ 异常: ${e.message}\n${e.stackTraceToString()}")
            }
        }
    }
}

/**
 * 发送到 LLM
 */
private fun sendToLLM(
    text: String,
    fileSystem: ProjectFileSystem,
    llmService: KoogLLMService,
    chatHistoryManager: ChatHistoryManager,
    scope: CoroutineScope,
    onLLMOutput: (String) -> Unit,
    onProcessingChange: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    onProcessingChange(true)
    onLLMOutput("")
    
    // 添加用户消息到历史
    chatHistoryManager.addUserMessage(text)
    
    scope.launch {
        var currentOutput = ""
        try {
            // 获取历史消息（排除刚添加的当前用户消息）
            val historyMessages = chatHistoryManager.getMessages().dropLast(1)
            
            println("📝 [ChatCallbacks] 发送消息，历史消息数: ${historyMessages.size}")
            
            // 传递 fileSystem 和历史消息
            llmService.streamPrompt(text, fileSystem, historyMessages)
                .catch { e ->
                    val errorMsg = extractErrorMessage(e)
                    onError(errorMsg)
                    onProcessingChange(false)
                }
                .collect { chunk ->
                    currentOutput += chunk
                    onLLMOutput(currentOutput)
                }
            
            // AI 响应完成后，添加到历史
            if (currentOutput.isNotEmpty()) {
                chatHistoryManager.addAssistantMessage(currentOutput)
                println("💾 [ChatCallbacks] AI 响应已保存到历史")
            }
            
            onProcessingChange(false)
        } catch (e: Exception) {
            val errorMsg = extractErrorMessage(e)
            onError(errorMsg)
            onLLMOutput("")
            onProcessingChange(false)
        }
    }
}

/**
 * 提取错误信息
 */
private fun extractErrorMessage(e: Throwable): String {
    val message = e.message ?: "Unknown error"

    return when {
        message.contains("DeepSeekLLMClient API") -> {
            val parts = message.split("API: ")
            if (parts.size > 1) {
                "=== DeepSeek API 错误 ===\n\n" +
                "API 返回：\n${parts[1]}\n\n" +
                "完整错误信息：\n$message"
            } else {
                "=== DeepSeek API 错误 ===\n\n$message"
            }
        }
        
        message.contains("OpenAI") -> "=== OpenAI API 错误 ===\n\n$message"
        message.contains("Anthropic") -> "=== Anthropic API 错误 ===\n\n$message"
        message.contains("Connection") || message.contains("timeout") -> "=== 网络连接错误 ===\n\n$message"
        message.contains("401") || message.contains("Unauthorized") -> "=== 认证失败 (401 Unauthorized) ===\n\n$message"
        message.contains("400") || message.contains("Bad Request") -> "=== 请求错误 (400 Bad Request) ===\n\n$message"
        message.contains("429") || message.contains("rate limit") -> "=== 请求限流 (429 Too Many Requests) ===\n\n$message"
        message.contains("500") || message.contains("Internal Server Error") -> "=== 服务器错误 (500) ===\n\n$message"
        
        else -> {
            "=== 错误详情 ===\n\n" +
            "错误类型：${e::class.simpleName}\n\n" +
            "错误消息：\n$message"
        }
    }
}

