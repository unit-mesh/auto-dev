package cc.unitmesh.server.cli

import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.LLMProviderType
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.compression.TokenInfo
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * JVM CLI for testing CodingAgent with autonomous coding tasks
 *
 * Usage:
 * ```bash
 * ./gradlew :mpp-ui:runCodingCli -PcodingProjectPath=/path/to/project -PcodingTask="Add a hello world function"
 * ```
 */
object CodingCli {

    @JvmStatic
    fun main(args: Array<String>) {
        println("=".repeat(80))
        println("AutoDev Coding Agent CLI (JVM)")
        println("=".repeat(80))

        // Parse arguments
        val projectPath = System.getProperty("projectPath") ?: args.getOrNull(0) ?: run {
            System.err.println("Usage: -PcodingProjectPath=<path> -PcodingTask=<task> [-PmaxIterations=100]")
            return
        }

        val task = System.getProperty("task") ?: args.getOrNull(1) ?: run {
            System.err.println("Usage: -PcodingProjectPath=<path> -PcodingTask=<task> [-PmaxIterations=100]")
            return
        }

        val maxIterations = System.getProperty("maxIterations")?.toIntOrNull() ?: 100

        println("📂 Project Path: $projectPath")
        println("📝 Task: $task")
        println("🔄 Max Iterations: $maxIterations")
        println()

        runBlocking {
            try {
                val projectDir = File(projectPath).absoluteFile
                if (!projectDir.exists()) {
                    System.err.println("❌ Project path does not exist: $projectPath")
                    return@runBlocking
                }

                val startTime = System.currentTimeMillis()

                // Load configuration from ~/.autodev/config.yaml
                val configFile = File(System.getProperty("user.home"), ".autodev/config.yaml")
                if (!configFile.exists()) {
                    System.err.println("❌ Configuration file not found: ${configFile.absolutePath}")
                    System.err.println("   Please create ~/.autodev/config.yaml with your LLM configuration")
                    return@runBlocking
                }

                val yamlContent = configFile.readText()
                val yaml = Yaml(configuration = com.charleskorn.kaml.YamlConfiguration(strictMode = false))
                val config = yaml.decodeFromString(AutoDevConfig.serializer(), yamlContent)

                val activeName = config.active
                val activeConfig = config.configs.find { it.name == activeName }

                if (activeConfig == null) {
                    System.err.println("❌ Active configuration '$activeName' not found in config.yaml")
                    System.err.println("   Available configs: ${config.configs.map { it.name }.joinToString(", ")}")
                    return@runBlocking
                }

                println("📝 Using config: ${activeConfig.name} (${activeConfig.provider}/${activeConfig.model})")

                // Convert provider string to LLMProviderType
                val providerType = when (activeConfig.provider.lowercase()) {
                    "openai" -> LLMProviderType.OPENAI
                    "anthropic" -> LLMProviderType.ANTHROPIC
                    "google" -> LLMProviderType.GOOGLE
                    "deepseek" -> LLMProviderType.DEEPSEEK
                    "ollama" -> LLMProviderType.OLLAMA
                    "openrouter" -> LLMProviderType.OPENROUTER
                    "glm" -> LLMProviderType.GLM
                    "qwen" -> LLMProviderType.QWEN
                    "kimi" -> LLMProviderType.KIMI
                    else -> LLMProviderType.CUSTOM_OPENAI_BASE
                }

                val llmService = KoogLLMService(
                    ModelConfig(
                        provider = providerType,
                        modelName = activeConfig.model,
                        apiKey = activeConfig.apiKey,
                        temperature = activeConfig.temperature ?: 0.7,
                        maxTokens = activeConfig.maxTokens ?: 4096,
                        baseUrl = activeConfig.baseUrl ?: ""
                    )
                )

                val renderer = CodingCliRenderer()
                val mcpConfigService = McpToolConfigService(ToolConfigFile())

                println("🧠 Creating CodingAgent...")
                val agent = CodingAgent(
                    projectPath = projectDir.absolutePath,
                    llmService = llmService,
                    maxIterations = maxIterations,
                    renderer = renderer,
                    fileSystem = DefaultToolFileSystem(projectDir.absolutePath),
                    mcpToolConfigService = mcpConfigService,
                    enableLLMStreaming = true
                )

                println("✅ Agent created")
                println()
                println("🚀 Executing task...")
                println()

                val result = agent.execute(
                    AgentTask(requirement = task, projectPath = projectDir.absolutePath),
                    onProgress = { progress -> println("   $progress") }
                )

                val totalTime = System.currentTimeMillis() - startTime

                println()
                println("=".repeat(80))
                println("📊 Result:")
                println("=".repeat(80))
                println(result.content)
                println()

                if (result.success) {
                    println("✅ Task completed successfully")
                } else {
                    println("❌ Task failed")
                }
                println("⏱️  Total time: ${totalTime}ms")
                println("📈 Steps: ${result.metadata["steps"] ?: "N/A"}")
                println("✏️  Edits: ${result.metadata["edits"] ?: "N/A"}")

            } catch (e: Exception) {
                System.err.println("❌ Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

/**
 * Console renderer for CodingCli output
 */
class CodingCliRenderer : CodingAgentRenderer {
    // Track active sessions for awaitSessionResult
    private val activeSessions = mutableMapOf<String, SessionInfo>()

    data class SessionInfo(
        val sessionId: String,
        val command: String,
        val process: Process?,
        val startTime: Long
    )

    override fun renderIterationHeader(current: Int, max: Int) {
        println("\n━━━ Iteration $current/$max ━━━")
    }

    override fun renderLLMResponseStart() {
        println("💭 ")
    }

    override fun renderLLMResponseChunk(chunk: String) {
        print(chunk)
        System.out.flush()
    }

    override fun renderLLMResponseEnd() {
        println("\n")
    }

    override fun renderToolCall(toolName: String, paramsStr: String) {
        println("● $toolName")
        if (paramsStr.isNotEmpty()) {
            val formatted = formatCliParameters(paramsStr)
            formatted.lines().forEach { line ->
                println("  ⎿ $line")
            }
        }
    }

    override fun addLiveTerminal(
        sessionId: String,
        command: String,
        workingDirectory: String?,
        ptyHandle: Any?
    ) {
        val process = ptyHandle as? Process
        activeSessions[sessionId] = SessionInfo(
            sessionId = sessionId,
            command = command,
            process = process,
            startTime = System.currentTimeMillis()
        )
        println("  ⏳ Running: $command")
    }

    override fun updateLiveTerminalStatus(
        sessionId: String,
        exitCode: Int,
        executionTimeMs: Long,
        output: String?,
        cancelledByUser: Boolean
    ) {
        activeSessions.remove(sessionId)
        val statusSymbol = when {
            cancelledByUser -> "⚠"
            exitCode == 0 -> "✓"
            else -> "✗"
        }
        val statusMessage = if (cancelledByUser) "Cancelled by user" else "Exit code: $exitCode"
        val preview = (output ?: "").lines().take(3).joinToString(" ").take(100)
        println("  $statusSymbol $statusMessage (${executionTimeMs}ms)")
        if (preview.isNotEmpty()) {
            println("  $preview${if (preview.length < (output ?: "").length) "..." else ""}")
        }
    }

    override suspend fun awaitSessionResult(sessionId: String, timeoutMs: Long): cc.unitmesh.agent.tool.ToolResult {
        val session = activeSessions[sessionId]
        if (session == null) {
            // Session not found - check ShellSessionManager
            val managedSession = cc.unitmesh.agent.tool.shell.ShellSessionManager.getSession(sessionId)
            if (managedSession != null) {
                return awaitManagedSession(managedSession, timeoutMs)
            }
            return cc.unitmesh.agent.tool.ToolResult.Error("Session not found: $sessionId")
        }

        val process = session.process
        if (process == null) {
            return cc.unitmesh.agent.tool.ToolResult.Error("No process handle for session: $sessionId")
        }

        return awaitProcess(process, session, timeoutMs)
    }

    private suspend fun awaitManagedSession(
        session: cc.unitmesh.agent.tool.shell.ManagedSession,
        timeoutMs: Long
    ): cc.unitmesh.agent.tool.ToolResult {
        val process = session.processHandle as? Process
        if (process == null) {
            return cc.unitmesh.agent.tool.ToolResult.Error("No process handle for session: ${session.sessionId}")
        }

        val startWait = System.currentTimeMillis()
        val checkIntervalMs = 100L

        while (process.isAlive) {
            val elapsed = System.currentTimeMillis() - startWait
            if (elapsed >= timeoutMs) {
                // Timeout - process still running
                val output = session.getOutput()
                return cc.unitmesh.agent.tool.ToolResult.Pending(
                    sessionId = session.sessionId,
                    toolName = "shell",
                    command = session.command,
                    message = "Process still running after ${elapsed}ms",
                    metadata = mapOf(
                        "partial_output" to output.take(1000),
                        "elapsed_ms" to elapsed.toString()
                    )
                )
            }
            kotlinx.coroutines.delay(checkIntervalMs)
        }

        // Process completed
        val exitCode = process.exitValue()
        val output = session.getOutput()
        val wasCancelledByUser = session.cancelledByUser
        val executionTimeMs = System.currentTimeMillis() - startWait
        session.markCompleted(exitCode)

        return when {
            wasCancelledByUser -> {
                // User cancelled the command - return a special result with output
                cc.unitmesh.agent.tool.ToolResult.Error(
                    message = buildCancelledMessage(session.command, exitCode, output),
                    errorType = "CANCELLED_BY_USER",
                    metadata = mapOf(
                        "exit_code" to exitCode.toString(),
                        "execution_time_ms" to executionTimeMs.toString(),
                        "session_id" to session.sessionId,
                        "cancelled" to "true",
                        "output" to output
                    )
                )
            }
            exitCode == 0 -> {
                cc.unitmesh.agent.tool.ToolResult.Success(
                    content = output.ifEmpty { "(no output)" },
                    metadata = mapOf(
                        "exit_code" to exitCode.toString(),
                        "execution_time_ms" to executionTimeMs.toString(),
                        "session_id" to session.sessionId
                    )
                )
            }
            else -> {
                cc.unitmesh.agent.tool.ToolResult.Error(
                    message = "Command failed with exit code $exitCode:\n$output",
                    errorType = cc.unitmesh.agent.tool.ToolErrorType.COMMAND_FAILED.code,
                    metadata = mapOf(
                        "exit_code" to exitCode.toString(),
                        "execution_time_ms" to executionTimeMs.toString(),
                        "session_id" to session.sessionId
                    )
                )
            }
        }
    }

    /**
     * Build a consistent cancelled message for user-cancelled commands.
     */
    private fun buildCancelledMessage(command: String, exitCode: Int, output: String): String = buildString {
        appendLine("⚠️ Command cancelled by user")
        appendLine()
        appendLine("Command: $command")
        appendLine("Exit code: $exitCode (SIGKILL)")
        appendLine()
        if (output.isNotEmpty()) {
            appendLine("Output before cancellation:")
            appendLine(output)
        } else {
            appendLine("(no output captured before cancellation)")
        }
    }

    private suspend fun awaitProcess(
        process: Process,
        session: SessionInfo,
        timeoutMs: Long
    ): cc.unitmesh.agent.tool.ToolResult {
        val startWait = System.currentTimeMillis()
        val checkIntervalMs = 100L
        val outputBuilder = StringBuilder()

        // Read output in background
        val stdoutReader = process.inputStream.bufferedReader()
        val stderrReader = process.errorStream.bufferedReader()

        while (process.isAlive) {
            // Read available output
            while (stdoutReader.ready()) {
                val line = stdoutReader.readLine() ?: break
                outputBuilder.appendLine(line)
                println("  │ $line")
            }
            while (stderrReader.ready()) {
                val line = stderrReader.readLine() ?: break
                outputBuilder.appendLine("[stderr] $line")
                println("  │ [stderr] $line")
            }

            val elapsed = System.currentTimeMillis() - startWait
            if (elapsed >= timeoutMs) {
                // Timeout - process still running
                return cc.unitmesh.agent.tool.ToolResult.Pending(
                    sessionId = session.sessionId,
                    toolName = "shell",
                    command = session.command,
                    message = "Process still running after ${elapsed}ms",
                    metadata = mapOf(
                        "partial_output" to outputBuilder.toString().take(1000),
                        "elapsed_ms" to elapsed.toString()
                    )
                )
            }
            kotlinx.coroutines.delay(checkIntervalMs)
        }

        // Read remaining output
        stdoutReader.forEachLine { line ->
            outputBuilder.appendLine(line)
            println("  │ $line")
        }
        stderrReader.forEachLine { line ->
            outputBuilder.appendLine("[stderr] $line")
            println("  │ [stderr] $line")
        }

        // Process completed
        val exitCode = process.exitValue()
        val output = outputBuilder.toString()
        activeSessions.remove(session.sessionId)

        // Check if cancelled by user from ShellSessionManager
        val managedSession = cc.unitmesh.agent.tool.shell.ShellSessionManager.getSession(session.sessionId)
        val wasCancelledByUser = managedSession?.cancelledByUser == true

        val executionTimeMs = System.currentTimeMillis() - session.startTime
        val statusSymbol = when {
            wasCancelledByUser -> "⚠"
            exitCode == 0 -> "✓"
            else -> "✗"
        }
        println("  $statusSymbol Exit code: $exitCode (${executionTimeMs}ms)${if (wasCancelledByUser) " [Cancelled by user]" else ""}")

        return when {
            wasCancelledByUser -> {
                cc.unitmesh.agent.tool.ToolResult.Error(
                    message = buildCancelledMessage(session.command, exitCode, output),
                    errorType = "CANCELLED_BY_USER",
                    metadata = mapOf(
                        "exit_code" to exitCode.toString(),
                        "session_id" to session.sessionId,
                        "execution_time_ms" to executionTimeMs.toString(),
                        "cancelled" to "true",
                        "output" to output
                    )
                )
            }
            exitCode == 0 -> {
                cc.unitmesh.agent.tool.ToolResult.Success(
                    content = output.ifEmpty { "(no output)" },
                    metadata = mapOf(
                        "exit_code" to exitCode.toString(),
                        "session_id" to session.sessionId,
                        "execution_time_ms" to executionTimeMs.toString()
                    )
                )
            }
            else -> {
                cc.unitmesh.agent.tool.ToolResult.Error(
                    message = "Command failed with exit code $exitCode:\n$output",
                    errorType = cc.unitmesh.agent.tool.ToolErrorType.COMMAND_FAILED.code,
                    metadata = mapOf(
                        "exit_code" to exitCode.toString(),
                        "session_id" to session.sessionId,
                        "execution_time_ms" to executionTimeMs.toString()
                    )
                )
            }
        }
    }

    private fun formatCliParameters(params: String): String {
        val trimmed = params.trim()

        // Handle JSON format
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val lines = mutableListOf<String>()
            val jsonPattern = Regex(""""(\w+)"\s*:\s*("([^"]*)"|(\d+)|true|false|null)""")
            jsonPattern.findAll(trimmed).forEach { match ->
                val key = match.groups[1]?.value ?: ""
                val value = match.groups[3]?.value
                    ?: match.groups[4]?.value
                    ?: match.groups[2]?.value?.removeSurrounding("\"")
                    ?: ""
                lines.add("$key = $value")
            }
            return if (lines.isNotEmpty()) lines.joinToString(", ") else params
        }

        return params
    }

    override fun renderToolResult(
        toolName: String,
        success: Boolean,
        output: String?,
        fullOutput: String?,
        metadata: Map<String, String>
    ) {
        val statusSymbol = if (success) "✓" else "✗"
        val preview = (output ?: fullOutput ?: "").lines().take(3).joinToString(" ").take(100)
        println("  $statusSymbol ${if (preview.length < (output ?: fullOutput ?: "").length) "$preview..." else preview}")
    }

    override fun renderTaskComplete() {
        println("\n✓ Task marked as complete")
    }

    override fun renderFinalResult(success: Boolean, message: String, iterations: Int) {
        val symbol = if (success) "✅" else "❌"
        println("\n$symbol Final result after $iterations iterations:")
        println(message)
    }

    override fun renderError(message: String) {
        System.err.println("❌ Error: $message")
    }

    override fun renderRepeatWarning(toolName: String, count: Int) {
        println("⚠️  Warning: Tool '$toolName' called $count times")
    }

    override fun renderRecoveryAdvice(recoveryAdvice: String) {
        println("💡 Recovery advice: $recoveryAdvice")
    }

    override fun updateTokenInfo(tokenInfo: TokenInfo) {
        // Display token info in CLI
        println("📊 Tokens: ${tokenInfo.inputTokens} in / ${tokenInfo.outputTokens} out")
    }

    override fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>) {
        println("❓ Confirmation required for: $toolName")
        println("   Params: $params")
    }
}
