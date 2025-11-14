package cc.unitmesh.agent.platform

import cc.unitmesh.devins.workspace.GitCommitInfo
import cc.unitmesh.devins.workspace.GitDiffInfo

/**
 * WebAssembly 平台的 Git 操作实现 (Stub)
 * 
 * WASM 环境中无法直接调用 git 命令，提供空实现
 */
actual class GitOperations actual constructor(private val projectPath: String) {
    
    actual fun isSupported(): Boolean {
        // WASM environment doesn't have access to git
        return false
    }
    
    actual suspend fun getModifiedFiles(): List<String> {
        // WASM environment doesn't have access to git
        return emptyList()
    }
    
    actual suspend fun getFileDiff(filePath: String): String? {
        // WASM environment doesn't have access to git
        return null
    }

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
