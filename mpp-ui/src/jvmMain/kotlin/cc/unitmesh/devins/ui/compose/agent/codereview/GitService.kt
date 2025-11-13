package cc.unitmesh.devins.ui.compose.agent.codereview

import cc.unitmesh.devins.workspace.GitCommitInfo
import cc.unitmesh.devins.workspace.GitDiffInfo
import cc.unitmesh.devins.workspace.GitDiffFile
import cc.unitmesh.devins.workspace.GitFileStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Git service for JVM platform
 * Reads git history and diff using git commands
 */
class GitService(private val projectPath: String) {
    
    /**
     * Get recent commits
     */
    suspend fun getRecentCommits(count: Int = 20): List<GitCommitInfo> = withContext(Dispatchers.IO) {
        try {
            val command = listOf(
                "git",
                "log",
                "-n", count.toString(),
                "--pretty=format:%H|%an|%ae|%ct|%s"
            )
            
            val process = ProcessBuilder(command)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                println("⚠️  Git log failed with exit code $exitCode: $output")
                return@withContext emptyList()
            }
            
            output.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    parseCommitLine(line)
                }
        } catch (e: Exception) {
            println("❌ Failed to get git commits: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Get diff for a specific commit
     */
    suspend fun getCommitDiff(commitHash: String): GitDiffInfo? = withContext(Dispatchers.IO) {
        try {
            // Get changed files with stats
            val statsCommand = listOf(
                "git",
                "show",
                "--numstat",
                "--pretty=format:",
                commitHash
            )
            
            val statsProcess = ProcessBuilder(statsCommand)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()
            
            val statsOutput = statsProcess.inputStream.bufferedReader().readText()
            statsProcess.waitFor()
            
            // Get actual diff
            val diffCommand = listOf(
                "git",
                "show",
                "--no-color",
                commitHash
            )
            
            val diffProcess = ProcessBuilder(diffCommand)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()
            
            val diffOutput = diffProcess.inputStream.bufferedReader().readText()
            val exitCode = diffProcess.waitFor()
            
            if (exitCode != 0) {
                println("⚠️  Git show failed with exit code $exitCode")
                return@withContext null
            }
            
            parseDiff(statsOutput, diffOutput)
        } catch (e: Exception) {
            println("❌ Failed to get commit diff: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get diff between two commits or branches
     */
    suspend fun getDiff(base: String, target: String): GitDiffInfo? = withContext(Dispatchers.IO) {
        try {
            val command = listOf(
                "git",
                "diff",
                "--numstat",
                base,
                target
            )
            
            val process = ProcessBuilder(command)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                println("⚠️  Git diff failed with exit code $exitCode")
                return@withContext null
            }
            
            // Get actual diff content
            val diffCommand = listOf(
                "git",
                "diff",
                "--no-color",
                base,
                target
            )
            
            val diffProcess = ProcessBuilder(diffCommand)
                .directory(File(projectPath))
                .start()
            
            val diffOutput = diffProcess.inputStream.bufferedReader().readText()
            diffProcess.waitFor()
            
            parseDiff(output, diffOutput)
        } catch (e: Exception) {
            println("❌ Failed to get diff: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
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
        } catch (e: Exception) {
            println("⚠️  Failed to parse commit line: $line")
            null
        }
    }
    
    private fun parseDiff(statsOutput: String, diffOutput: String): GitDiffInfo {
        val files = mutableListOf<GitDiffFile>()
        var totalAdditions = 0
        var totalDeletions = 0
        
        // Parse numstat output
        statsOutput.lines()
            .filter { it.isNotBlank() }
            .forEach { line ->
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 3) {
                    val additions = parts[0].toIntOrNull() ?: 0
                    val deletions = parts[1].toIntOrNull() ?: 0
                    val path = parts.subList(2, parts.size).joinToString(" ")
                    
                    totalAdditions += additions
                    totalDeletions += deletions
                    
                    // Extract diff for this file
                    val fileDiff = extractFileDiff(diffOutput, path)
                    
                    files.add(
                        GitDiffFile(
                            path = path,
                            oldPath = null,
                            status = determineFileStatus(diffOutput, path),
                            additions = additions,
                            deletions = deletions,
                            diff = fileDiff
                        )
                    )
                }
            }
        
        return GitDiffInfo(
            files = files,
            totalAdditions = totalAdditions,
            totalDeletions = totalDeletions
        )
    }
    
    private fun extractFileDiff(diffOutput: String, filePath: String): String {
        val lines = diffOutput.lines()
        val result = StringBuilder()
        var inFile = false
        var headerFound = false
        
        for (line in lines) {
            // Check for file header
            if (line.startsWith("diff --git") && line.contains(filePath)) {
                inFile = true
                headerFound = true
                result.appendLine(line)
                continue
            }
            
            // If we found the header, collect all lines until next diff
            if (inFile) {
                if (line.startsWith("diff --git") && !line.contains(filePath)) {
                    // Next file started
                    break
                }
                result.appendLine(line)
            }
        }
        
        return result.toString()
    }
    
    private fun determineFileStatus(diffOutput: String, filePath: String): GitFileStatus {
        val lines = diffOutput.lines()
        
        for (line in lines) {
            if (line.startsWith("new file") && diffOutput.contains("b/$filePath")) {
                return GitFileStatus.ADDED
            }
            if (line.startsWith("deleted file") && diffOutput.contains("a/$filePath")) {
                return GitFileStatus.DELETED
            }
            if (line.startsWith("rename from")) {
                return GitFileStatus.RENAMED
            }
        }
        
        return GitFileStatus.MODIFIED
    }
}
