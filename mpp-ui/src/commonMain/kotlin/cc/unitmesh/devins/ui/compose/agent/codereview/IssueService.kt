package cc.unitmesh.devins.ui.compose.agent.codereview

import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.agent.platform.GitOperations
import cc.unitmesh.agent.tracker.GitHubIssueTracker
import cc.unitmesh.agent.tracker.IssueInfo
import cc.unitmesh.agent.tracker.IssueTracker
import cc.unitmesh.agent.tracker.NoOpIssueTracker
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.config.IssueTrackerConfig
import kotlinx.coroutines.*

/**
 * Service for managing issue tracker integration and caching
 * 
 * Features:
 * - Create appropriate IssueTracker based on configuration
 * - Extract issue IDs from commit messages
 * - Cache issue information to avoid repeated API calls
 * - Auto-detect repo from Git remote URL
 */
class IssueService(private val workspacePath: String) {
    
    // Cache for issue information (commit hash -> issue info)
    private val issueCache = mutableMapOf<String, IssueInfo?>()
    
    // Cache for extracted issue IDs (commit hash -> issue ID)
    private val issueIdCache = mutableMapOf<String, String?>()
    
    private var issueTracker: IssueTracker = NoOpIssueTracker()
    private var isInitialized = false
    
    /**
     * Initialize the issue service
     * Loads configuration and creates appropriate issue tracker
     * 
     * @param gitOps Optional GitOperations instance for auto-detecting repo
     */
    suspend fun initialize(gitOps: GitOperations? = null) {
        try {
            val config = ConfigManager.getIssueTracker()
            
            // If no config or disabled, try to auto-detect and create default config
            if (config == null || !config.enabled) {
                // Try to auto-detect from Git
                if (gitOps != null && gitOps.isSupported()) {
                    val autoDetectedConfig = autoDetectRepoFromGit(gitOps)
                    if (autoDetectedConfig != null) {
                        AutoDevLogger.info("IssueService") {
                            "Auto-detected GitHub repo: ${autoDetectedConfig.repoOwner}/${autoDetectedConfig.repoName}"
                        }
                        issueTracker = createIssueTracker(autoDetectedConfig)
                        isInitialized = true
                        return
                    }
                }
                
                AutoDevLogger.info("IssueService") {
                    "Issue tracker not configured or disabled, and auto-detection failed"
                }
                issueTracker = NoOpIssueTracker()
                isInitialized = true
                return
            }
            
            // Always try to auto-detect repo from Git remote (override config)
            val finalConfig = if (gitOps != null && gitOps.isSupported()) {
                autoDetectRepo(gitOps, config)
            } else {
                config
            }
            
            // Create appropriate issue tracker based on type
            issueTracker = createIssueTracker(finalConfig)
            
            isInitialized = true
            AutoDevLogger.info("IssueService") {
                "Issue tracker initialized: ${finalConfig.type} (${finalConfig.repoOwner}/${finalConfig.repoName})"
            }
        } catch (e: Exception) {
            AutoDevLogger.error("IssueService") {
                "Failed to initialize issue tracker: ${e.message}"
            }
            issueTracker = NoOpIssueTracker()
            isInitialized = true
        }
    }
    
    /**
     * Auto-detect repository from Git remote URL (without existing config)
     */
    private suspend fun autoDetectRepoFromGit(gitOps: GitOperations): IssueTrackerConfig? {
        return try {
            val remoteUrl = gitOps.getRemoteUrl("origin")
            if (remoteUrl != null) {
                val parsed = GitHubIssueTracker.parseRepoUrl(remoteUrl)
                if (parsed != null) {
                    // Create a default config with auto-detected repo info
                    // Token will be loaded from config if exists, otherwise empty (public repo)
                    return IssueTrackerConfig(
                        type = "github",
                        token = ConfigManager.getIssueTracker()?.token ?: "",
                        repoOwner = parsed.first,
                        repoName = parsed.second,
                        enabled = true
                    )
                }
            }
            null
        } catch (e: Exception) {
            AutoDevLogger.warn("IssueService") {
                "Failed to auto-detect repo from Git: ${e.message}"
            }
            null
        }
    }
    
    /**
     * Auto-detect repository owner and name from Git remote URL
     */
    private suspend fun autoDetectRepo(gitOps: GitOperations, config: IssueTrackerConfig): IssueTrackerConfig {
        return try {
            val remoteUrl = gitOps.getRemoteUrl("origin")
            if (remoteUrl != null) {
                when (config.type.lowercase()) {
                    "github" -> {
                        val parsed = GitHubIssueTracker.parseRepoUrl(remoteUrl)
                        if (parsed != null) {
                            AutoDevLogger.info("IssueService") {
                                "Auto-detected GitHub repo: ${parsed.first}/${parsed.second}"
                            }
                            return config.copy(
                                repoOwner = parsed.first,
                                repoName = parsed.second
                            )
                        }
                    }
                    // Add more issue tracker types here (GitLab, etc.)
                }
            }
            config
        } catch (e: Exception) {
            AutoDevLogger.warn("IssueService") {
                "Failed to auto-detect repo: ${e.message}"
            }
            config
        }
    }
    
    /**
     * Create issue tracker based on configuration
     */
    private fun createIssueTracker(config: IssueTrackerConfig): IssueTracker {
        return when (config.type.lowercase()) {
            "github" -> {
                if (config.isConfigured()) {
                    GitHubIssueTracker(
                        repoOwner = config.repoOwner,
                        repoName = config.repoName,
                        token = config.token.takeIf { it.isNotBlank() }
                    )
                } else {
                    AutoDevLogger.warn("IssueService") {
                        "GitHub issue tracker not properly configured"
                    }
                    NoOpIssueTracker()
                }
            }
            // Add more issue tracker types here
            else -> {
                AutoDevLogger.warn("IssueService") {
                    "Unknown issue tracker type: ${config.type}"
                }
                NoOpIssueTracker()
            }
        }
    }
    
    /**
     * Extract issue ID from commit message
     * Supports common patterns like:
     * - #123
     * - GH-123
     * - fixes #123
     * - closes #123
     * 
     * @param commitMessage Commit message
     * @param commitHash Commit hash (for caching)
     * @return Issue ID or null if not found
     */
    fun extractIssueId(commitMessage: String, commitHash: String): String? {
        // Check cache first
        if (issueIdCache.containsKey(commitHash)) {
            return issueIdCache[commitHash]
        }
        
        // Common patterns for issue references
        val patterns = listOf(
            Regex("""#(\d+)"""),           // #123
            Regex("""GH-(\d+)""", RegexOption.IGNORE_CASE),  // GH-123
            Regex("""(?:fixes|closes|resolves)\s+#(\d+)""", RegexOption.IGNORE_CASE) // fixes #123
        )
        
        for (pattern in patterns) {
            val match = pattern.find(commitMessage)
            if (match != null) {
                val issueId = match.groupValues[1]
                issueIdCache[commitHash] = issueId
                return issueId
            }
        }
        
        // Cache null result to avoid repeated parsing
        issueIdCache[commitHash] = null
        return null
    }
    
    /**
     * Result of issue fetch attempt
     */
    data class IssueResult(
        val issueInfo: IssueInfo?,
        val error: String? = null,
        val needsToken: Boolean = false
    )
    
    /**
     * Get issue information asynchronously
     * Uses cache to avoid repeated API calls
     * 
     * @param commitHash Commit hash
     * @param commitMessage Commit message
     * @return Deferred<IssueResult> that resolves to issue result
     */
    fun getIssueAsync(commitHash: String, commitMessage: String): Deferred<IssueResult> {
        return CoroutineScope(Dispatchers.Default).async {
            // Check cache first
            if (issueCache.containsKey(commitHash)) {
                return@async IssueResult(issueInfo = issueCache[commitHash])
            }
            
            // Ensure initialized
            if (!isInitialized) {
                initialize()
            }
            
            // Extract issue ID
            val issueId = extractIssueId(commitMessage, commitHash)
            if (issueId == null) {
                issueCache[commitHash] = null
                return@async IssueResult(issueInfo = null)
            }
            
            // Fetch issue info
            try {
                val issueInfo = issueTracker.getIssue(issueId)
                issueCache[commitHash] = issueInfo
                
                if (issueInfo != null) {
                    AutoDevLogger.info("IssueService") {
                        "Fetched issue #$issueId: ${issueInfo.title}"
                    }
                } else {
                    AutoDevLogger.warn("IssueService") {
                        "Issue #$issueId not found"
                    }
                }
                
                IssueResult(issueInfo = issueInfo)
            } catch (e: Exception) {
                AutoDevLogger.error("IssueService") {
                    "Failed to fetch issue #$issueId: ${e.message}"
                }
                issueCache[commitHash] = null
                
                // Check if it's an authentication error
                val needsToken = e.message?.contains("401") == true || 
                                 e.message?.contains("403") == true ||
                                 e.message?.contains("authentication") == true
                
                IssueResult(
                    issueInfo = null,
                    error = if (needsToken) "Authentication required" else "Failed to fetch issue",
                    needsToken = needsToken
                )
            }
        }
    }
    
    /**
     * Get issue information synchronously (blocking)
     * Prefer getIssueAsync for better performance
     */
    suspend fun getIssue(commitHash: String, commitMessage: String): IssueInfo? {
        val result = getIssueAsync(commitHash, commitMessage).await()
        return result.issueInfo
    }
    
    /**
     * Check if issue tracker is configured and ready
     */
    fun isConfigured(): Boolean {
        return issueTracker.isConfigured()
    }
    
    /**
     * Clear cache (useful when configuration changes)
     */
    fun clearCache() {
        issueCache.clear()
        issueIdCache.clear()
        AutoDevLogger.info("IssueService") {
            "Issue cache cleared"
        }
    }
    
    /**
     * Reload configuration and reinitialize issue tracker
     */
    suspend fun reload(gitOps: GitOperations? = null) {
        clearCache()
        isInitialized = false
        initialize(gitOps)
    }
}

