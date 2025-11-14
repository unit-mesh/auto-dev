package cc.unitmesh.agent.platform

import cc.unitmesh.devins.workspace.GitCommitInfo
import cc.unitmesh.devins.workspace.GitDiffInfo

/**
 * 跨平台 Git 操作抽象
 * 
 * 提供获取修改文件列表、文件差异、提交历史等能力
 * 不同平台有不同实现：
 * - JVM: 使用 ProcessBuilder 调用 git 命令
 * - Android: 空实现或抛出异常（Android 上通常没有 git）
 * - JS/Wasm: 空实现或抛出异常
 */
expect class GitOperations(projectPath: String) {
    /**
     * 获取 git 仓库中已修改的文件列表
     * @return 文件路径列表
     */
    suspend fun getModifiedFiles(): List<String>
    
    /**
     * 获取指定文件的 diff
     * @param filePath 文件路径
     * @return diff 内容，如果获取失败返回 null
     */
    suspend fun getFileDiff(filePath: String): String?
    
    /**
     * 获取最近的 commit 历史
     * @param count 获取的 commit 数量
     * @return commit 列表
     */
    suspend fun getRecentCommits(count: Int = 20): List<GitCommitInfo>
    
    /**
     * 获取指定 commit 的 diff
     * @param commitHash commit hash
     * @return diff 信息，如果获取失败返回 null
     */
    suspend fun getCommitDiff(commitHash: String): GitDiffInfo?
    
    /**
     * 获取两个 commit/branch 之间的 diff
     * @param base 基准 commit/branch
     * @param target 目标 commit/branch
     * @return diff 信息，如果获取失败返回 null
     */
    suspend fun getDiff(base: String, target: String): GitDiffInfo?
    
    /**
     * 检查当前平台是否支持 git 操作
     * @return true 表示支持，false 表示不支持
     */
    fun isSupported(): Boolean
}
