package cc.unitmesh.agent.subagent

import cc.unitmesh.agent.core.SubAgent
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.ModelConfig
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.platform.GitOperations
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.llm.KoogLLMService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 错误恢复 SubAgent
 * 
 * 分析命令失败原因并提供恢复方案
 * 从 TypeScript 版本移植
 * 
 * 作为 Tool，可以被任何 Agent 调用
 * 
 * 跨平台支持：
 * - JVM: 完整支持 git 操作
 * - Android/JS/Wasm: 不支持 git，仅分析错误消息
 */
class ErrorRecoveryAgent(
    private val projectPath: String,
    private val llmService: KoogLLMService
) : SubAgent<ErrorContext, ToolResult.AgentResult>(
    definition = createDefinition()
) {
    private val gitOps = GitOperations(projectPath)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun validateInput(input: Map<String, Any>): ErrorContext {
        return ErrorContext(
            command = input["command"] as? String ?: throw IllegalArgumentException("command is required"),
            errorMessage = input["errorMessage"] as? String ?: throw IllegalArgumentException("errorMessage is required"),
            exitCode = (input["exitCode"] as? Number)?.toInt(),
            stdout = input["stdout"] as? String,
            stderr = input["stderr"] as? String
        )
    }

    override suspend fun execute(
        input: ErrorContext,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        onProgress("🔧 Error Recovery SubAgent")
        onProgress("Command: ${input.command}")
        onProgress("Error: ${input.errorMessage.take(80)}...")

        // Step 1: Check for file modifications (only if supported)
        val modifiedFiles = if (gitOps.isSupported()) {
            onProgress("Checking for file modifications...")
            getModifiedFiles()
        } else {
            onProgress("⚠️  Git not available on this platform, skipping file analysis")
            emptyList()
        }

        // Step 2: Get diffs for modified files
        val fileDiffs = if (modifiedFiles.isNotEmpty()) {
            onProgress("Getting diffs for ${modifiedFiles.size} file(s)...")
            getFileDiffs(modifiedFiles)
        } else {
            emptyMap()
        }

        // Step 3: Build error context
        onProgress("Building error context...")
        val context = buildErrorContext(input, modifiedFiles, fileDiffs)

        // Step 4: Ask LLM for fix
        onProgress("🤖 Analyzing error with AI...")
        val recovery = askLLMForFix(context)

        onProgress("✓ Analysis complete")
        
        // Convert RecoveryResult to ToolResult.AgentResult
        return ToolResult.AgentResult(
            success = recovery.success,
            content = formatRecovery(recovery),
            metadata = mapOf(
                "shouldRetry" to recovery.shouldRetry.toString(),
                "shouldAbort" to recovery.shouldAbort.toString(),
                "suggestedActionsCount" to recovery.suggestedActions.size.toString(),
                "hasRecoveryCommands" to (recovery.recoveryCommands != null).toString()
            )
        )
    }

    override fun formatOutput(output: ToolResult.AgentResult): String {
        return output.content
    }
    
    /**
     * 格式化恢复结果
     */
    private fun formatRecovery(recovery: RecoveryResult): String {
        return buildString {
            appendLine("📋 Analysis:")
            appendLine("   ${recovery.analysis}")

            if (recovery.suggestedActions.isNotEmpty()) {
                appendLine()
                appendLine("💡 Suggested Actions:")
                recovery.suggestedActions.forEachIndexed { index, action ->
                    appendLine("   ${index + 1}. $action")
                }
            }

            if (recovery.recoveryCommands != null && recovery.recoveryCommands.isNotEmpty()) {
                appendLine()
                appendLine("🔧 Recovery Commands:")
                recovery.recoveryCommands.forEach { cmd ->
                    appendLine("   $ $cmd")
                }
            }
        }
    }

    /**
     * 获取修改的文件列表
     */
    private suspend fun getModifiedFiles(): List<String> {
        return gitOps.getModifiedFiles()
    }

    /**
     * 获取文件差异
     */
    private suspend fun getFileDiffs(files: List<String>): Map<String, String> {
        val diffs = mutableMapOf<String, String>()
        
        for (file in files) {
            val diff = gitOps.getFileDiff(file)
            if (diff != null && diff.isNotBlank()) {
                diffs[file] = diff
            }
        }
        
        if (diffs.isNotEmpty()) {
            println("   📄 Collected ${diffs.size} diff(s)")
        }
        
        return diffs
    }

    /**
     * 构建错误上下文
     */
    private fun buildErrorContext(
        errorContext: ErrorContext,
        modifiedFiles: List<String>,
        fileDiffs: Map<String, String>
    ): String {
        return buildString {
            appendLine("# Error Recovery Context")
            appendLine()

            // 1. Command that failed
            appendLine("## Failed Command")
            appendLine("```bash")
            appendLine(errorContext.command)
            appendLine("```")
            appendLine()

            // 2. Exit code
            if (errorContext.exitCode != null) {
                appendLine("**Exit Code:** ${errorContext.exitCode}")
                appendLine()
            }

            // 3. Error message
            appendLine("## Error Message")
            appendLine("```")
            appendLine(errorContext.errorMessage)
            appendLine("```")
            appendLine()

            // 4. Modified files with diffs
            if (modifiedFiles.isNotEmpty() && fileDiffs.isNotEmpty()) {
                appendLine("## ⚠️ Files Modified Before Error")
                appendLine("The following files were changed, which may have caused the error:")
                appendLine()

                for ((file, diff) in fileDiffs) {
                    appendLine("### $file")
                    appendLine("```diff")
                    appendLine(diff)
                    appendLine("```")
                    appendLine()
                }
            } else if (modifiedFiles.isNotEmpty()) {
                appendLine("## Modified Files")
                modifiedFiles.forEach { appendLine("- $it") }
                appendLine()
                appendLine("(No diff available)")
            } else {
                appendLine("## No Files Modified")
                appendLine("No changes detected in the repository.")
                appendLine()
            }

            // 5. Additional context
            if (errorContext.stdout?.isNotBlank() == true) {
                appendLine("## Command Output (stdout)")
                appendLine("```")
                appendLine(errorContext.stdout.take(1000))
                appendLine("```")
                appendLine()
            }
        }
    }

    /**
     * 询问 LLM 分析和修复建议
     */
    private suspend fun askLLMForFix(context: String): RecoveryResult {
        val systemPrompt = """
You are an Error Recovery Agent. Your job is to:
1. Analyze why a command failed
2. Identify the root cause (especially if files were corrupted)
3. Suggest specific fixes

Focus on:
- Build file corruption (build.gradle.kts, pom.xml, package.json, etc.)
- Syntax errors introduced by recent changes
- File permission or path issues

Respond in this JSON format:
{
  "analysis": "Brief explanation of what went wrong",
  "rootCause": "The specific cause (e.g., 'build.gradle.kts was corrupted')",
  "suggestedActions": [
    "Specific action 1",
    "Specific action 2"
  ],
  "recoveryCommands": [
    "git checkout build.gradle.kts",
    "./gradlew build"
  ],
  "shouldRetry": true,
  "shouldAbort": false
}
""".trimIndent()

        val userPrompt = """
$context

**Task:** Analyze this error and provide a recovery plan. If files were modified and caused the error, suggest restoring them from git.
""".trimIndent()

        return try {
            // 使用 sendPrompt（非流式）获取完整响应
            val response = llmService.sendPrompt(
                "$systemPrompt\n\n$userPrompt"
            )
            parseRecoveryResponse(response)
        } catch (e: Exception) {
            println("   ❌ LLM analysis failed: ${e.message}")
            RecoveryResult(
                success = false,
                analysis = "Failed to analyze error: ${e.message}",
                suggestedActions = listOf("Manual intervention required"),
                shouldRetry = false,
                shouldAbort = true
            )
        }
    }

    /**
     * 解析 LLM 响应
     */
    private fun parseRecoveryResponse(response: String): RecoveryResult {
        return try {
            // Try to extract JSON from response
            val jsonMatch = Regex("```json\\s*([\\s\\S]*?)\\s*```").find(response)?.groupValues?.get(1)
                ?: Regex("\\{[\\s\\S]*?\\}").find(response)?.value

            if (jsonMatch != null) {
                val parsed = json.decodeFromString<RecoveryResultJson>(jsonMatch)
                RecoveryResult(
                    success = true,
                    analysis = parsed.analysis ?: parsed.rootCause ?: "Unknown error",
                    suggestedActions = parsed.suggestedActions ?: emptyList(),
                    recoveryCommands = parsed.recoveryCommands,
                    shouldRetry = parsed.shouldRetry ?: true,
                    shouldAbort = parsed.shouldAbort ?: false
                )
            } else {
                // Fallback: use raw response as analysis
                RecoveryResult(
                    success = true,
                    analysis = response,
                    suggestedActions = extractActionsFromText(response),
                    shouldRetry = response.contains("retry", ignoreCase = true),
                    shouldAbort = response.contains("abort", ignoreCase = true)
                )
            }
        } catch (e: Exception) {
            println("   ⚠️  Could not parse JSON response, using raw text")
            RecoveryResult(
                success = true,
                analysis = response,
                suggestedActions = extractActionsFromText(response),
                shouldRetry = response.contains("retry", ignoreCase = true),
                shouldAbort = response.contains("abort", ignoreCase = true)
            )
        }
    }

    /**
     * 从文本中提取动作项
     */
    private fun extractActionsFromText(text: String): List<String> {
        val actions = mutableListOf<String>()
        val lines = text.split("\n")

        for (line in lines) {
            if (Regex("^(\\d+\\.|[-*]|\\s*-)\\s*").containsMatchIn(line)) {
                val action = line.replace(Regex("^(\\d+\\.|[-*]|\\s*-)\\s*"), "").trim()
                if (action.length in 1..200) {
                    actions.add(action)
                }
            }
        }

        return actions
    }

    companion object {
        private fun createDefinition() = AgentDefinition(
            name = "error_recovery",
            displayName = "Error Recovery SubAgent",
            description = "Analyzes command failures and provides recovery plans",
            promptConfig = PromptConfig(
                systemPrompt = "You are an Error Recovery Agent specialized in diagnosing and fixing command failures."
            ),
            modelConfig = ModelConfig(modelId = "gpt-4"),
            runConfig = RunConfig(maxTurns = 5, maxTimeMinutes = 2)
        )
    }
}

/**
 * 错误上下文
 */
@Serializable
data class ErrorContext(
    val command: String,
    val errorMessage: String,
    val exitCode: Int? = null,
    val stdout: String? = null,
    val stderr: String? = null
)

/**
 * 恢复结果 - 内部数据类
 */
@Serializable
data class RecoveryResult(
    val success: Boolean,
    val analysis: String,
    val suggestedActions: List<String>,
    val recoveryCommands: List<String>? = null,
    val shouldRetry: Boolean,
    val shouldAbort: Boolean
)

/**
 * JSON 解析用的数据类
 */
@Serializable
private data class RecoveryResultJson(
    val analysis: String? = null,
    val rootCause: String? = null,
    val suggestedActions: List<String>? = null,
    val recoveryCommands: List<String>? = null,
    val shouldRetry: Boolean? = null,
    val shouldAbort: Boolean? = null
)
