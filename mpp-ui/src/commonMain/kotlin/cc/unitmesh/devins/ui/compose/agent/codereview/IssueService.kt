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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Serializable cache entry for persistent storage
 */
@Serializable
data class IssueCacheEntry(
    val issueId: String,
    val title: String,
    val description: String,
    val labels: List<String>,
    val status: String,
    val author: String?,
    val assignees: List<String>,
    val createdAt: String?,
    val updatedAt: String?,
    val cachedAtEpochMs: Long
) {
    fun toIssueInfo(): IssueInfo {
        return IssueInfo(
            id = issueId,
            title = title,
            description = description,
            labels = labels,
            status = status,
            author = author,
            assignees = assignees,
            createdAt = createdAt,
            updatedAt = updatedAt,
            cachedAt = Instant.fromEpochMilliseconds(cachedAtEpochMs)
        )
    }
    
    companion object {
        fun fromIssueInfo(issueInfo: IssueInfo): IssueCacheEntry {
            val cachedAt = issueInfo.cachedAt ?: Clock.System.now()
            return IssueCacheEntry(
                issueId = issueInfo.id,
                title = issueInfo.title,
                description = issueInfo.description,
                labels = issueInfo.labels,
                status = issueInfo.status,
                author = issueInfo.author,
                assignees = issueInfo.assignees,
                createdAt = issueInfo.createdAt,
                updatedAt = issueInfo.updatedAt,
                cachedAtEpochMs = cachedAt.toEpochMilliseconds()
            )
        }
    }
}

/**
 * Service for managing issue tracker integration and caching
 * 
 * Features:
 * - Create appropriate IssueTracker based on configuration
 * - Extract issue IDs from commit messages
 * - Cache issue information to avoid repeated API calls (with timestamps)
 * - Auto-detect repo from Git remote URL
 * - Support force refresh to bypass cache
 * - Show cache age for user transparency
 */
class IssueService(private val workspacePath: String) {
    
    // Cache for issue information (issue ID -> cached issue info with timestamp)
    private val issueCache = mutableMapOf<String, IssueInfo>()
    
    // Cache for extracted issue IDs (commit hash -> issue ID)
    private val issueIdCache = mutableMapOf<String, String?>()
    
    // Cache expiry time (default: 1 hour)
    private val cacheExpiryMinutes: Long = 60
    
    private var issueTracker: IssueTracker = NoOpIssueTracker()
    private var isInitialized = false
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = false
    }
    
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
        val needsToken: Boolean = false,
        /**
         * Whether this result was served from cache
         */
        val fromCache: Boolean = false,
        /**
         * Human-readable cache age (e.g., "5m ago", "2h ago")
         */
        val cacheAgeDisplay: String? = null
    )
    
    /**
     * Get issue information asynchronously
     * Uses cache to avoid repeated API calls (especially for GitHub rate limiting)
     * 
     * @param commitHash Commit hash
     * @param commitMessage Commit message
     * @param forceRefresh If true, bypass cache and fetch fresh data
     * @return Deferred<IssueResult> that resolves to issue result
     */
    fun getIssueAsync(
        commitHash: String, 
        commitMessage: String,
        forceRefresh: Boolean = false
    ): Deferred<IssueResult> {
        return CoroutineScope(Dispatchers.Default).async {
            // Ensure initialized
            if (!isInitialized) {
                initialize()
            }
            
            // Extract issue ID
            val issueId = extractIssueId(commitMessage, commitHash)
            if (issueId == null) {
                return@async IssueResult(issueInfo = null)
            }
            
            // Check cache first (unless force refresh)
            if (!forceRefresh) {
                val cachedIssue = issueCache[issueId]
                if (cachedIssue != null) {
                    val cacheAgeMinutes = cachedIssue.getCacheAgeMinutes() ?: 0
                    
                    // Return cached if not expired
                    if (cacheAgeMinutes < cacheExpiryMinutes) {
                        AutoDevLogger.debug("IssueService") {
                            "Using cached issue #$issueId (cached ${cachedIssue.getCacheAgeDisplay()})"
                        }
                        return@async IssueResult(
                            issueInfo = cachedIssue,
                            fromCache = true,
                            cacheAgeDisplay = cachedIssue.getCacheAgeDisplay()
                        )
                    } else {
                        AutoDevLogger.info("IssueService") {
                            "Cache expired for issue #$issueId (${cacheAgeMinutes}m old), refreshing..."
                        }
                    }
                }
            } else {
                AutoDevLogger.info("IssueService") {
                    "Force refreshing issue #$issueId"
                }
            }
            
            // Fetch issue info from remote
            try {
                val issueInfo = issueTracker.getIssue(issueId)
                
                if (issueInfo != null) {
                    // Add cache timestamp and store
                    val cachedInfo = issueInfo.withCacheTimestamp()
                    issueCache[issueId] = cachedInfo
                    
                    AutoDevLogger.info("IssueService") {
                        "Fetched issue #$issueId: ${issueInfo.title}"
                    }
                    
                    IssueResult(
                        issueInfo = cachedInfo,
                        fromCache = false
                    )
                } else {
                    AutoDevLogger.warn("IssueService") {
                        "Issue #$issueId not found"
                    }
                    IssueResult(issueInfo = null)
                }
            } catch (e: Exception) {
                AutoDevLogger.error("IssueService") {
                    "Failed to fetch issue #$issueId: ${e.message}"
                }
                
                // Check if it's a rate limiting or authentication error
                val is403 = e.message?.contains("403") == true
                val is401 = e.message?.contains("401") == true
                val needsToken = is401 || e.message?.contains("authentication") == true
                
                // On 403 rate limit, try to return cached data if available
                if (is403) {
                    val cachedIssue = issueCache[issueId]
                    if (cachedIssue != null) {
                        AutoDevLogger.warn("IssueService") {
                            "Rate limited (403), using cached issue #$issueId (cached ${cachedIssue.getCacheAgeDisplay()})"
                        }
                        return@async IssueResult(
                            issueInfo = cachedIssue,
                            error = "Rate limited - showing cached data",
                            fromCache = true,
                            cacheAgeDisplay = cachedIssue.getCacheAgeDisplay()
                        )
                    }
                }
                
                IssueResult(
                    issueInfo = null,
                    error = when {
                        is403 -> "Rate limited (403) - try again later"
                        needsToken -> "Authentication required"
                        else -> "Failed to fetch issue: ${e.message}"
                    },
                    needsToken = needsToken
                )
            }
        }
    }
    
    /**
     * Force refresh issue for a specific commit (bypasses cache)
     */
    fun refreshIssueAsync(commitHash: String, commitMessage: String): Deferred<IssueResult> {
        return getIssueAsync(commitHash, commitMessage, forceRefresh = true)
    }
    
    /**
     * Get cached issue info for an issue ID (if available)
     */
    fun getCachedIssue(issueId: String): IssueInfo? {
        return issueCache[issueId]
    }
    
    /**
     * Check if an issue is cached and not expired
     */
    fun isCacheValid(issueId: String): Boolean {
        val cached = issueCache[issueId] ?: return false
        val ageMinutes = cached.getCacheAgeMinutes() ?: return false
        return ageMinutes < cacheExpiryMinutes
    }
    
    /**
     * Get issue information synchronously (blocking)
     * Prefer getIssueAsync for better performance
     */
    suspend fun getIssue(commitHash: String, commitMessage: String, forceRefresh: Boolean = false): IssueInfo? {
        val result = getIssueAsync(commitHash, commitMessage, forceRefresh).await()
        return result.issueInfo
    }
    
    /**
     * Check if issue tracker is configured and ready
     */
    fun isConfigured(): Boolean {
        return issueTracker.isConfigured()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        val validCount = issueCache.count { (id, _) -> isCacheValid(id) }
        val expiredCount = issueCache.size - validCount
        val oldestCache = issueCache.values.mapNotNull { it.cachedAt }.minOrNull()
        val newestCache = issueCache.values.mapNotNull { it.cachedAt }.maxOrNull()
        
        return CacheStats(
            totalCached = issueCache.size,
            validCached = validCount,
            expiredCached = expiredCount,
            oldestCacheTime = oldestCache,
            newestCacheTime = newestCache
        )
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
     * Clear cache for a specific issue ID
     */
    fun clearCacheForIssue(issueId: String) {
        issueCache.remove(issueId)
        AutoDevLogger.info("IssueService") {
            "Cache cleared for issue #$issueId"
        }
    }
    
    /**
     * Reload configuration and reinitialize issue tracker
     * Note: This does NOT clear the cache to preserve cached data
     */
    suspend fun reload(gitOps: GitOperations? = null) {
        isInitialized = false
        initialize(gitOps)
    }
    
    /**
     * Reload configuration and clear all cache
     */
    suspend fun reloadAndClearCache(gitOps: GitOperations? = null) {
        clearCache()
        reload(gitOps)
    }
}

/**
 * Cache statistics for monitoring
 */
data class CacheStats(
    val totalCached: Int,
    val validCached: Int,
    val expiredCached: Int,
    val oldestCacheTime: Instant?,
    val newestCacheTime: Instant?
) {
    fun getOldestAgeDisplay(): String? {
        return oldestCacheTime?.let {
            val minutes = (Clock.System.now() - it).inWholeMinutes
            when {
                minutes < 1 -> "just now"
                minutes < 60 -> "${minutes}m"
                minutes < 1440 -> "${minutes / 60}h"
                else -> "${minutes / 1440}d"
            }
        }
    }
}

