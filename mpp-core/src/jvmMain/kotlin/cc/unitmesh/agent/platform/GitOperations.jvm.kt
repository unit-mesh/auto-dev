package cc.unitmesh.agent.platform

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.devins.workspace.GitCommitInfo
import cc.unitmesh.devins.workspace.GitDiffInfo
import cc.unitmesh.devins.workspace.GitDiffFile
import cc.unitmesh.devins.workspace.GitFileStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * JVM 平台的 Git 操作实现
 * 
 * 使用 ProcessBuilder 调用系统 git 命令
 */
actual class GitOperations actual constructor(private val projectPath: String) {

    private val logger = getLogger("GitOperations")

    actual suspend fun getModifiedFiles(): List<String> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("git", "diff", "--name-only")
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            val files = output.trim().split("\n").filter { it.isNotBlank() }

            if (files.isNotEmpty()) {
                logger.info { "Modified: ${files.joinToString(", ") { it.split("/").last() }}" }
            } else {
                logger.debug { "No modifications detected" }
            }

            files
        } catch (e: Exception) {
            logger.warn(e) { "Git check failed: ${e.message}" }
            emptyList()
        }
    }


    actual suspend fun performClone(repoUrl: String, targetDir: String?): Boolean {
        return try {
            val process = ProcessBuilder("git", "clone", repoUrl, targetDir)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (output.isNotBlank()) {
                logger.info { "Clone output: $output" }
            }

            true
        } catch (e: Exception) {
            logger.warn(e) { "Clone failed: ${e.message}" }
            false
        }
    }
    
    actual suspend fun getFileDiff(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("git", "diff", "--", filePath)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            if (output.isNotBlank()) {
                output
            } else {
                null
            }
        } catch (e: Exception) {
            // Silently skip
            null
        }
    }
    
    actual suspend fun getRecentCommits(count: Int): List<GitCommitInfo> = withContext(Dispatchers.IO) {
        try {
            // Use special delimiters to handle multi-line commit messages
            // %x1f = Unit Separator (ASCII 31), %x00 = Null (ASCII 0)
            val command = listOf(
                "git",
                "log",
                "-n", count.toString(),
                "--pretty=format:%H%x1f%an%x1f%ae%x1f%ct%x1f%B%x00"
            )
            
            val process = ProcessBuilder(command)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                logger.warn { "Git log failed with exit code $exitCode: $output" }
                return@withContext emptyList()
            }
            
            // Split by null character (record separator)
            output.split("\u0000")
                .filter { it.isNotBlank() }
                .mapNotNull { record ->
                    parseCommitLine(record)
                }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to get git commits: ${e.message}" }
            emptyList()
        }
    }
    
    actual suspend fun getTotalCommitCount(): Int? = withContext(Dispatchers.IO) {
        try {
            val command = listOf("git", "rev-list", "--count", "HEAD")
            
            val process = ProcessBuilder(command)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                logger.warn { "Git rev-list failed with exit code $exitCode: $output" }
                return@withContext null
            }
            
            output.toIntOrNull()
        } catch (e: Exception) {
            logger.warn(e) { "Failed to get total commit count: ${e.message}" }
            null
        }
    }
    
    actual suspend fun getCommitDiff(commitHash: String): GitDiffInfo? = withContext(Dispatchers.IO) {
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
            val statsExitCode = statsProcess.waitFor()
            
            if (statsExitCode != 0) {
                logger.warn { "Git show --numstat failed with exit code $statsExitCode. Output: ${statsOutput.take(500)}" }
            }
            
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
                logger.warn { "Git show failed with exit code $exitCode. Command: ${diffCommand.joinToString(" ")}. Output: ${diffOutput.take(500)}" }
                return@withContext null
            }
            
            parseDiff(statsOutput, diffOutput)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to get commit diff: ${e.message}" }
            null
        }
    }
    
    actual suspend fun getDiff(base: String, target: String): GitDiffInfo? = withContext(Dispatchers.IO) {
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
                logger.warn { "Git diff --numstat failed with exit code $exitCode. Command: ${command.joinToString(" ")}. Output: ${output.take(500)}" }
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
            logger.warn(e) { "Failed to get diff: ${e.message}" }
            null
        }
    }
    
    actual fun isSupported(): Boolean = true
    
    actual suspend fun getRemoteUrl(remoteName: String): String? = withContext(Dispatchers.IO) {
        try {
            val command = listOf("git", "remote", "get-url", remoteName)
            
            val process = ProcessBuilder(command)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            
            if (exitCode != 0 || output.isBlank()) {
                logger.debug { "No remote URL found for '$remoteName'" }
                return@withContext null
            }
            
            logger.info { "Remote '$remoteName' URL: $output" }
            output
        } catch (e: Exception) {
            logger.warn(e) { "Failed to get remote URL: ${e.message}" }
            null
        }
    }
    
    actual suspend fun hasParent(commitHash: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Try to get the parent commit hash
            val command = listOf("git", "rev-parse", "$commitHash^")
            
            val process = ProcessBuilder(command)
                .directory(File(projectPath))
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            
            // If exit code is 0, parent exists; if 128, it's a root commit
            exitCode == 0
        } catch (e: Exception) {
            logger.debug { "Failed to check parent for commit $commitHash: ${e.message}" }
            false
        }
    }
    
    // Private helper methods
    
    private fun parseCommitLine(line: String): GitCommitInfo? {
        return try {
            // Split by Unit Separator (ASCII 31)
            val parts = line.split("\u001f")
            if (parts.size < 5) return null
            
            val hash = parts[0].trim() // Trim to remove any leading/trailing whitespace or newlines
            
            GitCommitInfo(
                hash = hash,
                author = parts[1].trim(),
                email = parts[2].trim(),
                date = parts[3].toLongOrNull() ?: 0L,
                message = parts[4].trim(), // %B includes full commit message (subject + body)
                shortHash = hash.take(7)
            )
        } catch (e: Exception) {
            logger.warn { "Failed to parse commit line: $line" }
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
                    var path = parts.subList(2, parts.size).joinToString(" ")
                    var oldPath: String? = null
                    
                    // Handle git rename/move patterns like "old/{path => newpath}/file.kt"
                    // or "dir/{ => subdir}/file.kt" (move into subdirectory)
                    if (path.contains("{ => ") || path.contains(" => }")) {
                        val renamePattern = Regex("""(.*)\{(.*) => (.*)\}(.*)""")
                        val match = renamePattern.find(path)
                        if (match != null) {
                            val prefix = match.groupValues[1]
                            val oldPart = match.groupValues[2].trim()
                            val newPart = match.groupValues[3].trim()
                            val suffix = match.groupValues[4]
                            
                            // Construct the new path (target after move/rename)
                            path = prefix + newPart + suffix
                            // Construct the old path (source before move/rename)
                            oldPath = prefix + oldPart + suffix
                        }
                    }
                    
                    totalAdditions += additions
                    totalDeletions += deletions
                    
                    // Extract diff for this file
                    val fileDiff = extractFileDiff(diffOutput, path, oldPath)
                    
                    files.add(
                        GitDiffFile(
                            path = path,
                            oldPath = oldPath,
                            status = determineFileStatus(diffOutput, path, oldPath),
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
            totalDeletions = totalDeletions,
            originDiff = diffOutput
        )
    }
    
    private fun extractFileDiff(diffOutput: String, filePath: String, oldPath: String? = null): String {
        val lines = diffOutput.lines()
        val result = StringBuilder()
        var inFile = false
        var headerFound = false
        
        for (line in lines) {
            // Check for file header (check both new path and old path for renamed files)
            if (line.startsWith("diff --git") && 
                (line.contains(filePath) || (oldPath != null && line.contains(oldPath)))) {
                inFile = true
                headerFound = true
                result.appendLine(line)
                continue
            }
            
            // If we found the header, collect all lines until next diff
            if (inFile) {
                if (line.startsWith("diff --git") && 
                    !line.contains(filePath) && 
                    (oldPath == null || !line.contains(oldPath))) {
                    // Next file started
                    break
                }
                result.appendLine(line)
            }
        }
        
        return result.toString()
    }
    
    private fun determineFileStatus(diffOutput: String, filePath: String, oldPath: String? = null): GitFileStatus {
        val lines = diffOutput.lines()
        
        for (line in lines) {
            if (line.startsWith("new file") && diffOutput.contains("b/$filePath")) {
                return GitFileStatus.ADDED
            }
            if (line.startsWith("deleted file") && 
                (diffOutput.contains("a/$filePath") || (oldPath != null && diffOutput.contains("a/$oldPath")))) {
                return GitFileStatus.DELETED
            }
            if (line.startsWith("rename from") || oldPath != null) {
                return GitFileStatus.RENAMED
            }
        }
        
        return GitFileStatus.MODIFIED
    }
}
