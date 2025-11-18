package cc.unitmesh.agent.platform

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.devins.workspace.GitCommitInfo
import cc.unitmesh.devins.workspace.GitDiffInfo

/**
 * Android 平台的 Git 操作实现
 *
 * Android 上通常没有 git 命令行工具，因此返回空结果
 * ErrorRecoveryAgent 在 Android 上会自动跳过 git 相关分析
 */
actual class GitOperations actual constructor(private val projectPath: String) {

    private val logger = getLogger("GitOperations")

    actual suspend fun performClone(repoUrl: String, targetDir: String?): Boolean {
        logger.warn { "Git clone not supported on Android" }
        return false
    }

    actual suspend fun getModifiedFiles(): List<String> {
        logger.warn { "Git operations not supported on Android" }
        return emptyList()
    }
    
    actual suspend fun getFileDiff(filePath: String): String? {
        return null
    }
    
    actual fun isSupported(): Boolean = false

    actual suspend fun getRecentCommits(count: Int): List<GitCommitInfo> {
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
}
