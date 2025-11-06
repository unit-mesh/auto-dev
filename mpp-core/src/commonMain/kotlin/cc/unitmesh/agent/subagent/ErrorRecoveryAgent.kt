package cc.unitmesh.agent.subagent

import cc.unitmesh.agent.core.SubAgent
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.platform.GitOperations
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ErrorRecoverySchema : DeclarativeToolSchema(
    description = "Analyze and recover from errors in code or execution",
    properties = mapOf(
        "errorMessage" to string(
            description = "The error message or stack trace to analyze",
            required = true
        ),
        "context" to string(
            description = "Additional context about when/where the error occurred",
            required = false
        ),
        "codeSnippet" to string(
            description = "The code snippet that caused the error (optional)",
            required = false
        ),
        "errorType" to string(
            description = "Type of error (compilation, runtime, test, etc.)",
            required = false,
            enum = listOf("compilation", "runtime", "test", "build", "dependency", "configuration")
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return "/$toolName errorMessage=\"Compilation failed: cannot find symbol\" context=\"building project\" errorType=\"compilation\""
    }
}

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
    private val logger = getLogger("ErrorRecoveryAgent")
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

    override fun getParameterClass(): String = ErrorContext::class.simpleName ?: "ErrorContext"

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
            // Header with status indicator
            if (recovery.success) {
                appendLine("🔍 ERROR ANALYSIS COMPLETE")
            } else {
                appendLine("⚠️  ERROR ANALYSIS FAILED")
            }
            appendLine("=" .repeat(50))

            // Analysis section
            appendLine("📋 **Analysis:**")
            appendLine("   ${recovery.analysis}")
            appendLine()

            // Suggested actions with better formatting
            if (recovery.suggestedActions.isNotEmpty()) {
                appendLine("💡 **Recommended Actions:**")
                recovery.suggestedActions.forEachIndexed { index, action ->
                    appendLine("   ${index + 1}. $action")
                }
                appendLine()
            }

            // Recovery commands with execution hints
            if (recovery.recoveryCommands != null && recovery.recoveryCommands.isNotEmpty()) {
                appendLine("🔧 **Recovery Commands:**")
                appendLine("   Run these commands in order:")
                recovery.recoveryCommands.forEachIndexed { index, cmd ->
                    appendLine("   ${index + 1}. $ $cmd")
                }
                appendLine()
            }

            // Next steps guidance
            if (recovery.shouldRetry) {
                appendLine("🔄 **Next Steps:** The agent will retry the failed operation automatically.")
            } else if (recovery.shouldAbort) {
                appendLine("🛑 **Next Steps:** Manual intervention required. Please review the error and fix manually.")
            } else {
                appendLine("⏸️  **Next Steps:** Consider the suggested actions before proceeding.")
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
            logger.info { "   📄 Collected ${diffs.size} diff(s)" }
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
You are an Error Recovery Agent specialized in diagnosing and fixing development tool failures.

## Your Responsibilities:
1. **Analyze** the error message and context thoroughly
2. **Identify** the root cause with high precision
3. **Suggest** specific, actionable recovery steps
4. **Provide** exact commands when possible

## Common Error Categories to Focus On:
- **Build System Issues**: Corrupted build files (build.gradle.kts, pom.xml, package.json, etc.)
- **Dependency Problems**: Missing dependencies, version conflicts, repository issues
- **Syntax Errors**: Recent code changes that broke compilation
- **File System Issues**: Permission denied, file not found, path problems
- **Environment Issues**: Missing tools, wrong versions, configuration problems
- **Git Issues**: Merge conflicts, corrupted repository state

## Analysis Guidelines:
- Look for specific error patterns and keywords
- Consider recent file modifications as potential causes
- Distinguish between temporary and permanent failures
- Assess whether the error is recoverable or requires manual intervention

## Response Format:
Always respond with valid JSON in this exact format:
```json
{
  "analysis": "Clear, concise explanation of what went wrong and why",
  "rootCause": "The specific technical cause (e.g., 'Syntax error in build.gradle.kts line 15')",
  "suggestedActions": [
    "Step-by-step action that user should take",
    "Another specific action with clear instructions"
  ],
  "recoveryCommands": [
    "exact command to run",
    "another command if needed"
  ],
  "shouldRetry": true,
  "shouldAbort": false
}
```

## Important Notes:
- Keep analysis concise but informative
- Make suggested actions specific and actionable
- Only include recovery commands that are safe to run
- Set shouldRetry=true if the error is likely fixable
- Set shouldAbort=true only for unrecoverable errors
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
            logger.error(e) { "   ❌ LLM analysis failed: ${e.message}" }
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
            logger.warn(e) { "   ⚠️  Could not parse JSON response, using raw text" }
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
            name = ToolType.ErrorAgent.name,
            displayName = "Error Recovery SubAgent",
            description = "Analyzes command failures and provides recovery plans",
            promptConfig = PromptConfig(
                systemPrompt = "You are an Error Recovery Agent specialized in diagnosing and fixing command failures."
            ),
            modelConfig = ModelConfig.default(),
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
