package cc.unitmesh.agent.platform

import cc.unitmesh.devins.workspace.GitCommitInfo
import cc.unitmesh.devins.workspace.GitDiffInfo
import cc.unitmesh.devins.workspace.GitDiffFile
import cc.unitmesh.devins.workspace.GitFileStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

/**
 * WebAssembly 平台的 Git 操作实现
 *
 * 使用 wasm-git (libgit2 编译为 WASM) 提供 Git 功能
 * 参考: https://github.com/petersalomonsen/wasm-git
 */
actual class GitOperations actual constructor(private val projectPath: String) {

    private var lg2Module: LibGit2Module? = null
    private var isInitialized = false
    private val commandOutputBuffer = mutableListOf<String>()

    actual suspend fun performClone(repoUrl: String, targetDir: String?): Boolean {
        return try {
            this.clone(repoUrl, targetDir)
        } catch (e: Throwable) {
            WasmConsole.error("Clone error: ${e.message ?: "Unknown error"}")
            false
        }
    }

    init {
        CoroutineScope(Dispatchers.Default).launch {
            initialize()
        }
    }

    /**
     * Initialize wasm-git module
     */
    private suspend fun initialize() {
        if (isInitialized) return

        try {
            WasmConsole.log("Initializing wasm-git...")

            val config = createModuleConfig(
                onPrint = { text ->
                    WasmConsole.log("[Git] $text")
                    commandOutputBuffer.add(text)
                },
                onPrintErr = { text ->
                    WasmConsole.error("[Git Error] $text")
                }
            )

            lg2Module = lg2(config).await()

            WasmConsole.log("wasm-git initialized successfully")
            isInitialized = true

            // Try to change to project directory if it exists
            try {
                lg2Module?.FS?.chdir(projectPath)
            } catch (e: Throwable) {
                WasmConsole.warn("Project directory not found: $projectPath")
            }
        } catch (e: Throwable) {
            WasmConsole.error("Failed to initialize wasm-git: ${e.message ?: "Unknown error"}")
            throw e
        }
    }

    actual fun isSupported(): Boolean = true

    /**
     * Retry a suspend operation with exponential backoff
     * @param maxAttempts Maximum number of retry attempts (default: 3)
     * @param initialDelayMs Initial delay in milliseconds (default: 1000)
     * @param maxDelayMs Maximum delay in milliseconds (default: 8000)
     * @param factor Multiplier for exponential backoff (default: 2.0)
     * @param operation The suspend operation to retry
     * @return Result of the operation
     * @throws Exception if all retry attempts fail
     */
    private suspend fun <T> retryWithExponentialBackoff(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 8000,
        factor: Double = 2.0,
        operation: suspend (attempt: Int) -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null

        repeat(maxAttempts) { attempt ->
            try {
                return operation(attempt + 1)
            } catch (e: Throwable) {
                lastException = e
                
                if (attempt < maxAttempts - 1) {
                    WasmConsole.warn("Attempt ${attempt + 1} failed: ${e.message}. Retrying in ${currentDelay}ms...")
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
                } else {
                    WasmConsole.error("All $maxAttempts attempts failed. Last error: ${e.message}")
                }
            }
        }

        throw lastException ?: Exception("Operation failed after $maxAttempts attempts")
    }

    /**
     * Clone a git repository (Wasm-specific functionality)
     * @param repoUrl Repository URL (e.g., "https://github.com/user/repo.git")
     * @param targetDir Target directory name (optional, will be derived from URL if not provided)
     * @return true if successful
     */
    suspend fun clone(repoUrl: String, targetDir: String? = null): Boolean {
        initialize()

        val module = lg2Module ?: return false

        return try {
            val dir = targetDir ?: repoUrl.substringAfterLast('/').removeSuffix(".git")
            WasmConsole.log("Cloning $repoUrl into $dir...")

            // Use retry mechanism for clone operation to handle transient network failures
            retryWithExponentialBackoff(
                maxAttempts = 3,
                initialDelayMs = 1000,
                maxDelayMs = 8000
            ) { attempt ->
                WasmConsole.log("Clone attempt $attempt...")
                
                // Use CORS proxy for cross-origin requests
                val proxiedUrl = "https://cors-anywhere.com/$repoUrl"
                
                commandOutputBuffer.clear()
                val exitCode = module.callMain(jsArrayOf("clone", proxiedUrl, dir)).await<JsNumber>().toInt()

                if (exitCode == 0) {
                    WasmConsole.log("Clone successful")
                    // Change to cloned directory
                    module.FS.chdir(dir)
                    true
                } else {
                    val errorMsg = "Clone failed with exit code: $exitCode"
                    WasmConsole.error(errorMsg)
                    throw Exception(errorMsg)
                }
            }
        } catch (e: Throwable) {
            WasmConsole.error("Clone error after all retries: ${e.message}")
            false
        }
    }


    actual suspend fun getModifiedFiles(): List<String> {
        initialize()

        val module = lg2Module ?: return emptyList()

        return try {
            commandOutputBuffer.clear()
            val exitCode = module.callMain(jsArrayOf("status", "--porcelain")).await<JsNumber>().toInt()

            if (exitCode != 0) {
                WasmConsole.warn("git status failed with exit code: $exitCode")
                return emptyList()
            }

            // Parse porcelain output: each line is "XY filename"
            commandOutputBuffer
                .filter { it.isNotBlank() }
                .map { line -> line.substring(3).trim() }
        } catch (e: Throwable) {
            WasmConsole.error("Failed to get modified files: ${e.message}")
            emptyList()
        }
    }

    actual suspend fun getFileDiff(filePath: String): String? {
        initialize()

        val module = lg2Module ?: return null

        return try {
            commandOutputBuffer.clear()
            val exitCode = module.callMain(jsArrayOf("diff", filePath)).await<JsNumber>().toInt()

            if (exitCode != 0) {
                WasmConsole.warn("git diff failed with exit code: $exitCode")
                return null
            }

            commandOutputBuffer.joinToString("\n")
        } catch (e: Throwable) {
            WasmConsole.error("Failed to get file diff: ${e.message}")
            null
        }
    }

    actual suspend fun getRecentCommits(count: Int): List<GitCommitInfo> {
        initialize()

        val module = lg2Module ?: return emptyList()
        debugObj(lg2Module!!)

        return try {
            commandOutputBuffer.clear()
            // Use classic git log format (wasm-git doesn't support --pretty=format)
            val exitCode = module.callMain(jsArrayOf("log")).await<JsNumber>().toInt()

            if (exitCode != 0) {
                WasmConsole.warn("git log failed with exit code: $exitCode")
                return emptyList()
            }

            // Parse classic git log output
            val logOutput = commandOutputBuffer.joinToString("\n")
            GitLogParser.parse(logOutput)
        } catch (e: Throwable) {
            WasmConsole.error("Failed to get recent commits: ${e.message}")
            emptyList()
        }
    }

    actual suspend fun getTotalCommitCount(): Int? {
        initialize()

        val module = lg2Module ?: return null

        return try {
            commandOutputBuffer.clear()
            val exitCode = module.callMain(jsArrayOf("rev-list", "--count", "HEAD")).await<JsNumber>().toInt()

            if (exitCode != 0) {
                WasmConsole.warn("git rev-list failed with exit code: $exitCode")
                return null
            }

            commandOutputBuffer.firstOrNull()?.trim()?.toIntOrNull()
        } catch (e: Throwable) {
            WasmConsole.error("Failed to get commit count: ${e.message}")
            null
        }
    }

    actual suspend fun getCommitDiff(commitHash: String): GitDiffInfo? {
        initialize()

        val module = lg2Module ?: return null

        return try {
            // Step 1: Get list of changed files using diff --name-status
            commandOutputBuffer.clear()
            var exitCode = module.callMain(jsArrayOf("diff", "--name-status", "$commitHash^", commitHash)).await<JsNumber>().toInt()

            if (exitCode != 0) {
                WasmConsole.warn("git diff --name-status failed with exit code: $exitCode")
                return null
            }

            val changedFiles = commandOutputBuffer
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    // Format: "M\tfilename" or "A\tfilename" or "D\tfilename"
                    val parts = line.split("\t", limit = 2)
                    if (parts.size == 2) {
                        Pair(parts[0], parts[1]) // (status, filename)
                    } else null
                }

            // Step 2: Get the actual diff for context
            commandOutputBuffer.clear()
            exitCode = module.callMain(jsArrayOf("diff", "$commitHash^", commitHash)).await<JsNumber>().toInt()
            val diffContent = if (exitCode == 0) {
                commandOutputBuffer.joinToString("\n")
            } else {
                ""
            }

            // Step 3: For each modified/added file, read its full content
            val filesWithContent = buildString {
                for ((status, filename) in changedFiles) {
                    appendLine("=" .repeat(80))
                    appendLine("File: $filename (Status: $status)")
                    appendLine("=" .repeat(80))
                    
                    when (status) {
                        "D" -> {
                            appendLine("[File Deleted]")
                        }
                        "A", "M" -> {
                            // Try to read the file content from current working directory
                            val content = try {
                                val fileData = module.FS.readFile(filename).toIntArray()
                                val bytes = fileData.map { it.toByte() }.toByteArray()
                                bytes.decodeToString()
                            } catch (e: Throwable) {
                                "[Unable to read file: ${e.message}]"
                            }
                            appendLine(content)
                        }
                    }
                    appendLine()
                }
                
                // Also include the diff for reference
                if (diffContent.isNotBlank()) {
                    appendLine("=" .repeat(80))
                    appendLine("Git Diff Output:")
                    appendLine("=" .repeat(80))
                    appendLine(diffContent)
                }
            }

            GitDiffInfo(
                files = changedFiles.map { (status, filename) ->
                    val fileStatus = when (status) {
                        "A" -> GitFileStatus.ADDED
                        "D" -> GitFileStatus.DELETED
                        "M" -> GitFileStatus.MODIFIED
                        "R" -> GitFileStatus.RENAMED
                        "C" -> GitFileStatus.COPIED
                        else -> GitFileStatus.MODIFIED
                    }
                    GitDiffFile(
                        path = filename,
                        oldPath = null,
                        status = fileStatus,
                        additions = 0,
                        deletions = 0,
                        diff = ""
                    )
                },
                totalAdditions = 0, // Could be parsed from diff output if needed
                totalDeletions = 0, // Could be parsed from diff output if needed
                originDiff = filesWithContent
            )
        } catch (e: Throwable) {
            WasmConsole.error("Failed to get commit diff: ${e.message}")
            null
        }
    }

    actual suspend fun getDiff(base: String, target: String): GitDiffInfo? {
        initialize()

        val module = lg2Module ?: return null

        return try {
            commandOutputBuffer.clear()
            val exitCode = module.callMain(jsArrayOf("diff", base, target)).await<JsNumber>().toInt()

            if (exitCode != 0) {
                WasmConsole.warn("git diff failed with exit code: $exitCode")
                return null
            }

            val diff = commandOutputBuffer.joinToString("\n")
            // TODO: Parse diff to extract file changes, additions, and deletions
            GitDiffInfo(
                files = emptyList(),
                totalAdditions = 0,
                totalDeletions = 0,
                originDiff = diff
            )
        } catch (e: Throwable) {
            WasmConsole.error("Failed to get diff: ${e.message}")
            null
        }
    }

    /**
     * Initialize a new git repository
     */
    suspend fun init(): Boolean {
        initialize()

        val module = lg2Module ?: return false

        return try {
            WasmConsole.log("Initializing git repository...")
            val exitCode = module.callMain(jsArrayOf("init")).await<JsNumber>().toInt()
            exitCode == 0
        } catch (e: Throwable) {
            WasmConsole.error("Failed to init repository: ${e.message}")
            false
        }
    }

    /**
     * Add files to staging area
     */
    suspend fun add(vararg files: String): Boolean {
        initialize()

        val module = lg2Module ?: return false

        return try {
            val args = jsArrayOf("add", *files)
            val exitCode = module.callMain(args).await<JsNumber>().toInt()
            exitCode == 0
        } catch (e: Throwable) {
            WasmConsole.error("Failed to add files: ${e.message}")
            false
        }
    }

    /**
     * Commit changes
     */
    suspend fun commit(message: String): Boolean {
        initialize()

        val module = lg2Module ?: return false

        return try {
            val exitCode = module.callMain(jsArrayOf("commit", "-m", message)).await<JsNumber>().toInt()
            exitCode == 0
        } catch (e: Throwable) {
            WasmConsole.error("Failed to commit: ${e.message}")
            false
        }
    }

    /**
     * Push changes to remote
     */
    suspend fun push(remote: String = "origin", branch: String = "master"): Boolean {
        initialize()

        val module = lg2Module ?: return false

        return try {
            val exitCode = module.callMain(jsArrayOf("push", remote, branch)).await<JsNumber>().toInt()
            exitCode == 0
        } catch (e: Throwable) {
            WasmConsole.error("Failed to push: ${e.message}")
            false
        }
    }

    /**
     * Pull changes from remote
     */
    suspend fun pull(remote: String = "origin", branch: String = "master"): Boolean {
        initialize()

        val module = lg2Module ?: return false

        return try {
            var exitCode = module.callMain(jsArrayOf("fetch", remote)).await<JsNumber>().toInt()
            if (exitCode != 0) return false

            exitCode = module.callMain(jsArrayOf("merge", "$remote/$branch")).await<JsNumber>().toInt()
            exitCode == 0
        } catch (e: Throwable) {
            WasmConsole.error("Failed to pull: ${e.message}")
            false
        }
    }

    /**
     * Read file content from repository
     */
    suspend fun readFile(filePath: String): String? {
        initialize()

        val module = lg2Module ?: return null

        return try {
            val fileData = module.FS.readFile(filePath).toIntArray()
            val bytes = fileData.map { it.toByte() }.toByteArray()
            bytes.decodeToString()
        } catch (e: Throwable) {
            WasmConsole.error("Failed to read file: ${e.message}")
            null
        }
    }

    /**
     * Write file to repository
     */
    suspend fun writeFile(filePath: String, content: String): Boolean {
        initialize()

        val module = lg2Module ?: return false

        return try {
            module.FS.writeFile(filePath, content)
            true
        } catch (e: Throwable) {
            WasmConsole.error("Failed to write file: ${e.message}")
            false
        }
    }
    
    actual suspend fun getRemoteUrl(remoteName: String): String? {
        initialize()
        
        val module = lg2Module ?: return null
        
        return try {
            commandOutputBuffer.clear()
            val exitCode = module.callMain(jsArrayOf("remote", "get-url", remoteName)).await<JsNumber>().toInt()
            
            if (exitCode != 0) {
                WasmConsole.warn("git remote get-url failed with exit code: $exitCode")
                return null
            }
            
            commandOutputBuffer.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
        } catch (e: Throwable) {
            WasmConsole.error("Failed to get remote URL: ${e.message}")
            null
        }
    }
}

fun debugObj(obj: LibGit2Module): Unit = js("console.log(obj)")
