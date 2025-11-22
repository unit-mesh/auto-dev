package cc.unitmesh.agent.platform

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.devins.workspace.GitCommitInfo
import cc.unitmesh.devins.workspace.GitDiffInfo
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * JS 平台的 Git 操作实现 (Node.js)
 * 
 * 使用 Node.js 的 child_process 模块调用 git 命令
 * 移植自 TypeScript 版本的 ErrorRecoveryAgent
 */
actual class GitOperations actual constructor(private val projectPath: String) {

    private val logger = getLogger("GitOperations")

    private val isNodeJs: Boolean by lazy {
        try {
            js("typeof process !== 'undefined' && process.versions && process.versions.node") as Boolean
        } catch (e: Throwable) {
            false
        }
    }

    actual suspend fun performClone(repoUrl: String, targetDir: String?): Boolean {
        if (!isNodeJs) {
            logger.warn { "Git operations require Node.js environment" }
            return false
        }

        return try {
            val dir = targetDir ?: repoUrl.substringAfterLast('/').removeSuffix(".git")
            logger.info { "Cloning $repoUrl into $dir..." }

            val output = execGitCommand("git clone $repoUrl $dir")
            logger.info { "Clone output: $output" }
            true
        } catch (e: Throwable) {
            logger.warn(e) { "Clone failed: ${e.message}" }
            false
        }
    }
    
    actual suspend fun getModifiedFiles(): List<String> {
        if (!isNodeJs) {
            logger.warn { "Git operations require Node.js environment" }
            return emptyList()
        }

        return try {
            val output = execGitCommand("git diff --name-only")
            val files = output.trim().split("\n").filter { it.isNotBlank() }

            if (files.isNotEmpty()) {
                val fileNames = files.map { it.split("/").last() }.joinToString(", ")
                logger.info { "Modified: $fileNames" }
            } else {
                logger.debug { "No modifications detected" }
            }

            files
        } catch (e: Throwable) {
            logger.warn(e) { "Git check failed: ${e.message}" }
            emptyList()
        }
    }
    
    actual suspend fun getFileDiff(filePath: String): String? {
        if (!isNodeJs) {
            return null
        }
        
        return try {
            val output = execGitCommand("git diff -- \"$filePath\"")
            if (output.trim().isNotBlank()) {
                output
            } else {
                null
            }
        } catch (e: Throwable) {
            // Silently skip
            null
        }
    }
    
    actual suspend fun getRecentCommits(count: Int): List<GitCommitInfo> {
        if (!isNodeJs) {
            logger.warn { "Git operations require Node.js environment" }
            return emptyList()
        }

        return try {
            // Use special delimiters to handle multi-line commit messages
            // %x1f = Unit Separator (ASCII 31), %x00 = Null (ASCII 0)
            val output = execGitCommand("git log -n $count --pretty=format:%H%x1f%an%x1f%ae%x1f%ct%x1f%B%x00")
            // Split by null character (record separator)
            output.split("\u0000")
                .filter { it.isNotBlank() }
                .mapNotNull { parseCommitLine(it) }
        } catch (e: Throwable) {
            logger.warn(e) { "Failed to get git commits: ${e.message}" }
            emptyList()
        }
    }

    actual suspend fun getTotalCommitCount(): Int? {
        if (!isNodeJs) {
            return null
        }

        return try {
            val output = execGitCommand("git rev-list --count HEAD")
            output.trim().toIntOrNull()
        } catch (e: Throwable) {
            logger.warn(e) { "Failed to get total commit count: ${e.message}" }
            null
        }
    }

    actual suspend fun getCommitDiff(commitHash: String): GitDiffInfo? {
        if (!isNodeJs) {
            return null
        }
        
        return try {
            // Get changed files with stats
            val statsOutput = execGitCommand("git show --numstat --pretty=format: $commitHash")
            
            // Get actual diff
            val diffOutput = execGitCommand("git show --no-color $commitHash")
            
            parseDiff(statsOutput, diffOutput)
        } catch (e: Throwable) {
            logger.warn(e) { "Failed to get commit diff: ${e.message}" }
            null
        }
    }
    
    actual suspend fun getDiff(base: String, target: String): GitDiffInfo? {
        if (!isNodeJs) {
            return null
        }
        
        return try {
            val statsOutput = execGitCommand("git diff --numstat $base $target")
            val diffOutput = execGitCommand("git diff --no-color $base $target")
            
            parseDiff(statsOutput, diffOutput)
        } catch (e: Throwable) {
            logger.warn(e) { "Failed to get diff: ${e.message}" }
            null
        }
    }
    
    actual fun isSupported(): Boolean = isNodeJs
    
    actual suspend fun getRemoteUrl(remoteName: String): String? {
        if (!isNodeJs) {
            return null
        }
        
        return try {
            val output = execGitCommand("git remote get-url $remoteName")
            val url = output.trim()
            if (url.isNotBlank()) {
                logger.info { "Remote '$remoteName' URL: $url" }
                url
            } else {
                null
            }
        } catch (e: Throwable) {
            logger.warn(e) { "Failed to get remote URL: ${e.message}" }
            null
        }
    }
    
    // Private helper methods
    
    private fun parseCommitLine(line: String): GitCommitInfo? {
        return try {
            // Split by Unit Separator (ASCII 31)
            val parts = line.split("\u001f")
            if (parts.size < 5) return null
            
            GitCommitInfo(
                hash = parts[0],
                author = parts[1],
                email = parts[2],
                date = parts[3].toLongOrNull() ?: 0L,
                message = parts[4].trim(), // %B includes full commit message (subject + body)
                shortHash = parts[0].take(7)
            )
        } catch (e: Throwable) {
            logger.warn { "Failed to parse commit line: $line" }
            null
        }
    }
    
    private fun parseDiff(statsOutput: String, diffOutput: String): GitDiffInfo {
        return GitDiffInfo(
            files = emptyList(),
            totalAdditions = 0,
            totalDeletions = 0,
            originDiff = diffOutput
        )
    }
    
    /**
     * 执行 git 命令
     * 使用 Node.js 的 child_process.exec
     */
    private suspend fun execGitCommand(command: String): String {
        return execAsync(command, projectPath).await()
    }
}

/**
 * 封装 Node.js 的 child_process.exec 为 Promise
 */
private fun execAsync(command: String, cwd: String): Promise<String> {
    val exec: dynamic = js("require('child_process').exec")
    
    return Promise { resolve, reject ->
        exec(command, js("{ cwd: cwd }")) { error: dynamic, stdout: dynamic, stderr: dynamic ->
            if (error != null) {
                reject(js("new Error(stderr || error.message)"))
            } else {
                resolve(stdout as String)
            }
        }
    }
}
