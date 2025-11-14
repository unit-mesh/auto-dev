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
            val output = execGitCommand("git log -n $count --pretty=format:%H|%an|%ae|%ct|%s")
            output.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { parseCommitLine(it) }
        } catch (e: Throwable) {
            logger.warn(e) { "Failed to get git commits: ${e.message}" }
            emptyList()
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
    
    // Private helper methods
    
    private fun parseCommitLine(line: String): GitCommitInfo? {
        return try {
            val parts = line.split("|")
            if (parts.size < 5) return null
            
            GitCommitInfo(
                hash = parts[0],
                author = parts[1],
                email = parts[2],
                date = parts[3].toLongOrNull() ?: 0L,
                message = parts[4],
                shortHash = parts[0].take(7)
            )
        } catch (e: Throwable) {
            logger.warn { "Failed to parse commit line: $line" }
            null
        }
    }
    
    private fun parseDiff(statsOutput: String, diffOutput: String): GitDiffInfo {
        // Simplified parsing - just return empty for now
        // Full implementation would mirror the JVM version
        return GitDiffInfo(
            files = emptyList(),
            totalAdditions = 0,
            totalDeletions = 0
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
