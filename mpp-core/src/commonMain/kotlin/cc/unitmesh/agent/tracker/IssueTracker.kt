package cc.unitmesh.agent.tracker

/**
 * Issue information from issue tracker
 */
data class IssueInfo(
    val id: String,
    val title: String,
    val description: String,
    val labels: List<String> = emptyList(),
    val status: String = "unknown",
    val author: String? = null,
    val assignees: List<String> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Abstract interface for issue tracking systems
 * Supports GitHub, GitLab, Jira, etc.
 */
interface IssueTracker {
    /**
     * Get issue information by ID
     * 
     * @param issueId Issue ID (e.g., "123" for #123)
     * @return Issue information or null if not found
     */
    suspend fun getIssue(issueId: String): IssueInfo?
    
    /**
     * Get multiple issues by IDs
     * 
     * @param issueIds List of issue IDs
     * @return Map of issue ID to issue information
     */
    suspend fun getIssues(issueIds: List<String>): Map<String, IssueInfo> {
        val result = mutableMapOf<String, IssueInfo>()
        issueIds.forEach { issueId ->
            getIssue(issueId)?.let { issue ->
                result[issueId] = issue
            }
        }
        return result
    }
    
    /**
     * Check if the issue tracker is properly configured
     * 
     * @return true if configured and ready to use
     */
    fun isConfigured(): Boolean
    
    /**
     * Get the tracker type (e.g., "github", "gitlab", "jira")
     */
    fun getType(): String
}

/**
 * No-op issue tracker for cases where issue tracking is not configured
 */
class NoOpIssueTracker : IssueTracker {
    override suspend fun getIssue(issueId: String): IssueInfo? = null
    
    override suspend fun getIssues(issueIds: List<String>): Map<String, IssueInfo> = emptyMap()
    
    override fun isConfigured(): Boolean = false
    
    override fun getType(): String = "none"
}

