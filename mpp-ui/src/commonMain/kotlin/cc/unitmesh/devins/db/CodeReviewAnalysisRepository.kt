package cc.unitmesh.devins.db

import cc.unitmesh.devins.ui.compose.agent.codereview.AIAnalysisProgress

/**
 * CodeReviewAnalysisRepository - Code Review 分析结果数据访问层
 * 使用 expect/actual 模式支持跨平台持久化
 * 
 * 使用 remoteUrl 作为主要标识符，projectPath 作为 fallback
 */
expect class CodeReviewAnalysisRepository {
    /**
     * 保存或更新分析结果
     * @param remoteUrl Git remote URL (如果可用)
     * @param projectPath 项目路径（fallback）
     * @param commitHash commit hash
     * @param progress 分析进度数据
     */
    fun saveAnalysisResult(remoteUrl: String?, projectPath: String, commitHash: String, progress: AIAnalysisProgress)
    
    /**
     * 根据 remote URL/项目路径和 commit hash 查询分析结果
     * 优先使用 remoteUrl 查询，如果为空则使用 projectPath
     * @param remoteUrl Git remote URL (如果可用)
     * @param projectPath 项目路径（fallback）
     * @param commitHash commit hash
     * @return 分析进度数据，如果不存在返回 null
     */
    fun getAnalysisResult(remoteUrl: String?, projectPath: String, commitHash: String): AIAnalysisProgress?
    
    /**
     * 根据 remote URL/项目路径查询所有分析结果
     * 优先使用 remoteUrl 查询，如果为空则使用 projectPath
     * @param remoteUrl Git remote URL (如果可用)
     * @param projectPath 项目路径（fallback）
     * @return 分析结果列表
     */
    fun getAnalysisResultsByProject(remoteUrl: String?, projectPath: String): List<Pair<String, AIAnalysisProgress>>
    
    /**
     * 删除指定项目和 commit 的分析结果
     * @param remoteUrl Git remote URL (如果可用)
     * @param projectPath 项目路径（fallback）
     * @param commitHash commit hash
     */
    fun deleteAnalysisResult(remoteUrl: String?, projectPath: String, commitHash: String)
    
    /**
     * 删除旧的分析结果，只保留最近的 N 条
     * @param keepCount 保留的数量
     */
    fun deleteOldAnalysis(keepCount: Long = 100)
    
    /**
     * 清空所有分析结果
     */
    fun deleteAll()
    
    companion object {
        fun getInstance(): CodeReviewAnalysisRepository
    }
}

