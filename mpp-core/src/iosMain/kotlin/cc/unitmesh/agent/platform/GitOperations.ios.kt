package cc.unitmesh.agent.platform

import cc.unitmesh.devins.workspace.GitCommitInfo
import cc.unitmesh.devins.workspace.GitDiffInfo

/**
 * iOS implementation of GitOperations
 * Git operations are not supported on iOS
 */
actual class GitOperations actual constructor(private val projectPath: String) {
    actual suspend fun performClone(repoUrl: String, targetDir: String?): Boolean {
        return false
    }

    actual suspend fun getModifiedFiles(): List<String> {
        println("Git operations not supported on iOS")
        return emptyList()
    }

    actual suspend fun getFileDiff(filePath: String): String? {
        return null
    }

    actual suspend fun getRecentCommits(count: Int): List<GitCommitInfo> {
        println("Git operations not supported on iOS")
        return emptyList()
    }

    actual suspend fun getTotalCommitCount(): Int? {
        return null
    }

    actual suspend fun getCommitDiff(commitHash: String): GitDiffInfo? {
        return null
    }

    actual suspend fun getDiff(base: String, target: String): GitDiffInfo? {
        return null
    }

    actual fun isSupported(): Boolean = false
}

