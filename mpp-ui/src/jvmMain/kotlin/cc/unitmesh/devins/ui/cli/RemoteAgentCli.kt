package cc.unitmesh.devins.ui.cli

import cc.unitmesh.devins.ui.remote.*
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * Remote Agent CLI - Kotlin equivalent of TypeScript server command
 *
 * Usage:
 * ```bash
 * ./gradlew :mpp-ui:runRemoteAgentCli --args="--server http://localhost:8080 --project-id autocrud --task 'Write tests for BlogService'"
 * ```
 */
object RemoteAgentCli {
    private const val ANSI_RESET = "\u001B[0m"
    private const val ANSI_GREEN = "\u001B[32m"
    private const val ANSI_RED = "\u001B[31m"
    private const val ANSI_YELLOW = "\u001B[33m"
    private const val ANSI_BLUE = "\u001B[34m"
    private const val ANSI_CYAN = "\u001B[36m"
    private const val ANSI_GRAY = "\u001B[90m"
    private const val ANSI_BOLD = "\u001B[1m"

    @JvmStatic
    fun main(args: Array<String>) {
        val options = parseArgs(args)

        if (options.help) {
            printHelp()
            exitProcess(0)
        }

        if (options.serverUrl.isEmpty() || options.projectId.isEmpty() || options.task.isEmpty()) {
            println("${ANSI_RED}❌ Error: Missing required arguments${ANSI_RESET}")
            printHelp()
            exitProcess(1)
        }

        runBlocking {
            try {
                runRemoteAgent(options)
            } catch (e: Exception) {
                println("\n${ANSI_RED}❌ Fatal error: ${e.message}${ANSI_RESET}")
                e.printStackTrace()
                exitProcess(1)
            }
        }
    }

    private suspend fun runRemoteAgent(options: CliOptions) {
        val client = RemoteAgentClient(options.serverUrl)

        // Health check
        println("${ANSI_CYAN}🔍 Connecting to server: ${options.serverUrl}${ANSI_RESET}")
        try {
            val health = client.healthCheck()
            println("${ANSI_GREEN}✅ Server is ${health.status}${ANSI_RESET}")
        } catch (e: Exception) {
            println("${ANSI_RED}❌ Server health check failed: ${e.message}${ANSI_RESET}")
            exitProcess(1)
        }

        // Prepare request
        val llmConfig = if (!options.useServerConfig) {
            // Load from local config (simplified - you can integrate with ConfigManager)
            LLMConfig(
                provider = options.provider,
                modelName = options.model,
                apiKey = options.apiKey,
                baseUrl = options.baseUrl
            )
        } else {
            null
        }

        // Auto-detect Git URL (like TypeScript version)
        val isGitUrl = options.projectId.startsWith("http://") ||
                       options.projectId.startsWith("https://") ||
                       options.projectId.startsWith("git@")

        val actualProjectId: String
        val actualGitUrl: String?

        if (isGitUrl && options.gitUrl.isNullOrBlank()) {
            // projectId is a Git URL - extract repo name and use URL as gitUrl
            actualProjectId = options.projectId.split('/').lastOrNull()?.removeSuffix(".git") ?: "temp-project"
            actualGitUrl = options.projectId
        } else {
            // Normal project ID or explicit gitUrl provided
            actualProjectId = options.projectId
            actualGitUrl = options.gitUrl
        }

        // Print banner with actual values
        printBanner(options, actualProjectId, actualGitUrl)

        val request = RemoteAgentRequest(
            projectId = actualProjectId,
            task = options.task,
            llmConfig = llmConfig,
            gitUrl = actualGitUrl,
            branch = options.branch,
            username = options.username,
            password = options.password
        )

        // Create renderer
        val renderer = CliRenderer()

        // Execute with streaming
        try {
            client.executeStream(request).collect { event ->
                renderer.renderEvent(event)

                if (event is RemoteAgentEvent.Complete) {
                    client.close()
                    exitProcess(if (event.success) 0 else 1)
                }
            }
        } catch (e: Exception) {
            println("${ANSI_RED}❌ Streaming error: ${e.message}${ANSI_RESET}")
            e.printStackTrace()
            client.close()
            exitProcess(1)
        }
    }

    private fun printBanner(options: CliOptions, actualProjectId: String, gitUrl: String?) {
        println()
        println("${ANSI_BOLD}${ANSI_CYAN}🚀 AutoDev Remote Coding Agent${ANSI_RESET}")
        println("${ANSI_CYAN}🌐 Server: ${options.serverUrl}${ANSI_RESET}")
        println("${ANSI_CYAN}📦 Project: ${options.projectId}${ANSI_RESET}")

        if (!options.useServerConfig) {
            println("${ANSI_CYAN}📦 Provider: ${options.provider} (from client)${ANSI_RESET}")
            println("${ANSI_CYAN}🤖 Model: ${options.model}${ANSI_RESET}")
        } else {
            println("${ANSI_CYAN}📦 Using server configuration${ANSI_RESET}")
        }

        println()
        println("${ANSI_GRAY}${"━".repeat(80)}${ANSI_RESET}")

        // Show clone info if Git URL is detected
        if (!gitUrl.isNullOrBlank()) {
            println()
            println("${ANSI_CYAN}📦 Cloning repository...${ANSI_RESET}")
            println()
        }
    }

    private fun printHelp() {
        println("""
            ${ANSI_BOLD}AutoDev Remote Agent CLI${ANSI_RESET}

            ${ANSI_BOLD}USAGE:${ANSI_RESET}
                RemoteAgentCli [OPTIONS]

            ${ANSI_BOLD}REQUIRED OPTIONS:${ANSI_RESET}
                -s, --server <URL>          Server URL (default: http://localhost:8080)
                -p, --project-id <ID>       Project ID on the server
                -t, --task <TASK>           Development task to complete

            ${ANSI_BOLD}OPTIONAL:${ANSI_RESET}
                --use-server-config         Use server's LLM configuration
                --provider <PROVIDER>       LLM provider (default: deepseek)
                --model <MODEL>             Model name (default: deepseek-chat)
                --api-key <KEY>             API key for LLM
                --base-url <URL>            Base URL for LLM API
                --git-url <URL>             Git repository URL (for auto-clone)
                --branch <BRANCH>           Git branch (default: main)
                --username <USER>           Git username for private repos
                --password <PASS>           Git password or token
                -h, --help                  Show this help message

            ${ANSI_BOLD}EXAMPLES:${ANSI_RESET}
                # Use existing project on server
                RemoteAgentCli --server http://localhost:8080 \\
                    --project-id autocrud \\
                    --task "Write tests for BlogService"

                # Clone from Git and execute
                RemoteAgentCli --server http://localhost:8080 \\
                    --project-id https://github.com/unit-mesh/untitled \\
                    --task "Add error handling" \\
                    --provider deepseek \\
                    --model deepseek-chat \\
                    --api-key sk-xxx
        """.trimIndent())
    }

    private fun parseArgs(args: Array<String>): CliOptions {
        val options = CliOptions()
        var i = 0

        while (i < args.size) {
            when (args[i]) {
                "-s", "--server" -> options.serverUrl = args.getOrNull(++i) ?: ""
                "-p", "--project-id" -> options.projectId = args.getOrNull(++i) ?: ""
                "-t", "--task" -> options.task = args.getOrNull(++i) ?: ""
                "--use-server-config" -> options.useServerConfig = true
                "--provider" -> options.provider = args.getOrNull(++i) ?: "deepseek"
                "--model" -> options.model = args.getOrNull(++i) ?: "deepseek-chat"
                "--api-key" -> options.apiKey = args.getOrNull(++i) ?: ""
                "--base-url" -> options.baseUrl = args.getOrNull(++i) ?: ""
                "--git-url" -> options.gitUrl = args.getOrNull(++i)
                "--branch" -> options.branch = args.getOrNull(++i) ?: "main"
                "--username" -> options.username = args.getOrNull(++i)
                "--password" -> options.password = args.getOrNull(++i)
                "-h", "--help" -> options.help = true
                else -> {
                    println("${ANSI_YELLOW}⚠️  Unknown option: ${args[i]}${ANSI_RESET}")
                }
            }
            i++
        }

        return options
    }

    private data class CliOptions(
        var serverUrl: String = "http://localhost:8080",
        var projectId: String = "",
        var task: String = "",
        var useServerConfig: Boolean = false,
        var provider: String = "deepseek",
        var model: String = "deepseek-chat",
        var apiKey: String = "",
        var baseUrl: String = "",
        var gitUrl: String? = null,
        var branch: String = "main",
        var username: String? = null,
        var password: String? = null,
        var help: Boolean = false
    )

    /**
     * CLI Renderer - Kotlin equivalent of ServerRenderer.ts
     */
    private class CliRenderer {
        private var isCloning = false
        private var lastCloneProgress = 0
        private var hasStartedLLMOutput = false
        private val reasoningBuffer = StringBuilder()
        private var lastOutputLength = 0

        fun renderEvent(event: RemoteAgentEvent) {
            when (event) {
                is RemoteAgentEvent.CloneProgress -> renderCloneProgress(event.stage, event.progress)
                is RemoteAgentEvent.CloneLog -> renderCloneLog(event.message, event.isError)
                is RemoteAgentEvent.Iteration -> renderIteration(event.current, event.max)
                is RemoteAgentEvent.LLMChunk -> renderLLMChunk(event.chunk)
                is RemoteAgentEvent.ToolCall -> renderToolCall(event.toolName, event.params)
                is RemoteAgentEvent.ToolResult -> renderToolResult(event.toolName, event.success, event.output)
                is RemoteAgentEvent.Error -> renderError(event.message)
                is RemoteAgentEvent.Complete -> renderComplete(event)
            }
        }

        private fun renderCloneProgress(stage: String, progress: Int?) {
            if (!isCloning) {
                println()
                println("${ANSI_GRAY}${"━".repeat(80)}${ANSI_RESET}")
                println("${ANSI_BLUE}📦 Cloning repository...${ANSI_RESET}")
                println()
                isCloning = true
            }

            if (progress != null && progress != lastCloneProgress) {
                val barLength = 30
                val filledLength = (progress * barLength) / 100
                val bar = "█".repeat(filledLength) + "░".repeat(barLength - filledLength)

                print("\r${ANSI_CYAN}[$bar]${ANSI_RESET} $progress% - $stage")

                if (progress == 100) {
                    println()
                    println("${ANSI_GREEN}✓ Clone completed${ANSI_RESET}")
                    println("${ANSI_GRAY}${"━".repeat(80)}${ANSI_RESET}")
                    println()
                }

                lastCloneProgress = progress
            }
        }

        private fun renderCloneLog(message: String, isError: Boolean) {
            // Filter out noisy git messages
            val noisyPatterns = listOf(
                Regex("^Executing:"),
                Regex("^remote:"),
                Regex("^Receiving objects:"),
                Regex("^Resolving deltas:"),
                Regex("^Unpacking objects:")
            )

            if (noisyPatterns.any { it.containsMatchIn(message) }) {
                return
            }

            // Only show important messages
            if (message.contains("✓") || message.contains("Repository ready") || isError) {
                if (isError) {
                    println("${ANSI_RED}  ✗ $message${ANSI_RESET}")
                } else {
                    println("${ANSI_GRAY}  $message${ANSI_RESET}")
                }
            }
        }

        private fun renderIteration(current: Int, max: Int) {
            // Flush any buffered LLM output
            if (reasoningBuffer.isNotEmpty()) {
                println()
                reasoningBuffer.clear()
            }

            hasStartedLLMOutput = false
            lastOutputLength = 0
        }

        private fun renderLLMChunk(chunk: String) {
            if (!hasStartedLLMOutput) {
                print("${ANSI_GRAY}💭 ${ANSI_RESET}")
                hasStartedLLMOutput = true
            }

            reasoningBuffer.append(chunk)
            val processedContent = filterDevinBlocks(reasoningBuffer.toString())

            if (processedContent.length > lastOutputLength) {
                val newContent = processedContent.substring(lastOutputLength)
                print(newContent)
                lastOutputLength = processedContent.length
            }
        }

        private fun renderToolCall(toolName: String, params: String) {
            // Flush LLM output
            if (reasoningBuffer.isNotEmpty()) {
                println()
                reasoningBuffer.clear()
                hasStartedLLMOutput = false
                lastOutputLength = 0
            }

            val toolInfo = formatToolCall(toolName, params)
            println("${ANSI_BOLD}● ${toolInfo.name}${ANSI_RESET}${ANSI_GRAY} - ${toolInfo.description}${ANSI_RESET}")
            if (toolInfo.details != null) {
                println("${ANSI_GRAY}  ⎿ ${toolInfo.details}${ANSI_RESET}")
            }
        }

        private fun renderToolResult(toolName: String, success: Boolean, output: String?) {
            if (success && output != null) {
                val summary = generateToolSummary(toolName, output)
                println("${ANSI_GREEN}  ⎿ $summary${ANSI_RESET}")
            } else if (!success && output != null) {
                println("${ANSI_RED}  ⎿ Error: ${output.take(200)}${ANSI_RESET}")
            }
        }

        private fun renderError(message: String) {
            println()
            println("${ANSI_RED}❌ Error: $message${ANSI_RESET}")
            println()
        }

        private fun renderComplete(event: RemoteAgentEvent.Complete) {
            // Flush any buffered output
            if (reasoningBuffer.isNotEmpty()) {
                println()
            }

            println()
            if (event.success) {
                println("${ANSI_GREEN}✅ Task completed successfully${ANSI_RESET}")
            } else {
                println("${ANSI_RED}❌ Task failed${ANSI_RESET}")
            }

            if (event.message.isNotEmpty()) {
                println("${ANSI_GRAY}${event.message}${ANSI_RESET}")
            }

            println("${ANSI_GRAY}Task completed after ${event.iterations} iterations${ANSI_RESET}")

            if (event.edits.isNotEmpty()) {
                println()
                println("${ANSI_CYAN}📝 File Changes:${ANSI_RESET}")
                event.edits.forEach { edit ->
                    val icon = when (edit.operation) {
                        "CREATE" -> "➕"
                        "DELETE" -> "➖"
                        else -> "✏️"
                    }
                    println("${ANSI_BLUE}  $icon ${edit.file}${ANSI_RESET}")
                }
            }

            println()
        }

        private fun filterDevinBlocks(content: String): String {
            // Simple implementation - filter out <devin_block> tags
            return content
                .replace(Regex("<devin_block>.*?</devin_block>", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("<devin_block>.*", RegexOption.DOT_MATCHES_ALL), "") // Incomplete blocks
        }

        private fun formatToolCall(toolName: String, params: String): ToolInfo {
            // Simple parsing - you can enhance this
            return when (toolName) {
                "read-file" -> ToolInfo("$toolName", "file reader", "Reading file")
                "write-file", "edit-file" -> ToolInfo("$toolName", "file editor", "Editing file")
                "shell" -> ToolInfo("Shell command", "command executor", "Running command")
                "glob" -> ToolInfo("File search", "pattern matcher", "Searching files")
                "grep" -> ToolInfo("Text search", "content finder", "Searching text")
                else -> ToolInfo(toolName, "tool", null)
            }
        }

        private fun generateToolSummary(toolName: String, output: String): String {
            return when (toolName) {
                "glob" -> {
                    val files = output.lines().filter { it.isNotBlank() }
                    "Found ${files.size} files"
                }
                "read-file" -> {
                    val lines = output.lines().size
                    "Read $lines lines"
                }
                "write-file", "edit-file" -> "File operation completed"
                "shell" -> "Command executed successfully"
                "grep" -> {
                    val matches = output.lines().filter { it.isNotBlank() }.size
                    "Found $matches matches"
                }
                else -> "Operation completed"
            }
        }

        private data class ToolInfo(
            val name: String,
            val description: String,
            val details: String?
        )
    }
}

