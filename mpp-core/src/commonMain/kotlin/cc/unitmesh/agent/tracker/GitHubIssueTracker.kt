package cc.unitmesh.agent.tracker

import cc.unitmesh.agent.logging.getLogger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * GitHub issue tracker implementation using GitHub REST API
 * 
 * @param repoOwner Repository owner (e.g., "unitymesh")
 * @param repoName Repository name (e.g., "auto-dev")
 * @param token GitHub personal access token (optional for public repos)
 * @param apiUrl GitHub API base URL (default: https://api.github.com)
 */
class GitHubIssueTracker(
    private val repoOwner: String,
    private val repoName: String,
    private val token: String? = null,
    private val apiUrl: String = "https://api.github.com"
) : IssueTracker {
    
    private val logger = getLogger("GitHubIssueTracker")
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    override suspend fun getIssue(issueId: String): IssueInfo? {
        if (!isConfigured()) {
            logger.warn { "GitHub issue tracker not properly configured" }
            return null
        }
        
        return try {
            val url = "$apiUrl/repos/$repoOwner/$repoName/issues/$issueId"
            logger.info { "Fetching GitHub issue: $url" }
            
            val response = client.get(url) {
                if (token != null) {
                    header("Authorization", "Bearer $token")
                }
                header("Accept", "application/vnd.github+json")
                header("X-GitHub-Api-Version", "2022-11-28")
            }
            
            if (response.status.isSuccess()) {
                val githubIssue: GitHubIssue = response.body()
                githubIssue.toIssueInfo()
            } else {
                logger.warn { "Failed to fetch issue #$issueId: ${response.status}" }
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Error fetching GitHub issue #$issueId: ${e.message}" }
            null
        }
    }
    
    override fun isConfigured(): Boolean {
        return repoOwner.isNotBlank() && repoName.isNotBlank()
    }
    
    override fun getType(): String = "github"
    
    /**
     * Parse repository URL to extract owner and repo name
     * Supports formats:
     * - https://github.com/owner/repo
     * - git@github.com:owner/repo.git
     * 
     * @param repoUrl Repository URL
     * @return Pair of (owner, repo) or null if parsing fails
     */
    companion object {
        fun parseRepoUrl(repoUrl: String): Pair<String, String>? {
            val httpsPattern = Regex("https://github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$")
            val sshPattern = Regex("git@github\\.com:([^/]+)/([^/]+?)(?:\\.git)?/?$")
            
            val httpsMatch = httpsPattern.find(repoUrl)
            if (httpsMatch != null) {
                val owner = httpsMatch.groupValues[1]
                val repo = httpsMatch.groupValues[2]
                return Pair(owner, repo)
            }
            
            val sshMatch = sshPattern.find(repoUrl)
            if (sshMatch != null) {
                val owner = sshMatch.groupValues[1]
                val repo = sshMatch.groupValues[2]
                return Pair(owner, repo)
            }
            
            return null
        }
        
        /**
         * Create GitHubIssueTracker from repository URL
         * 
         * @param repoUrl Repository URL
         * @param token GitHub token (optional)
         * @return GitHubIssueTracker or null if URL parsing fails
         */
        fun fromRepoUrl(repoUrl: String, token: String? = null): GitHubIssueTracker? {
            val (owner, repo) = parseRepoUrl(repoUrl) ?: return null
            return GitHubIssueTracker(owner, repo, token)
        }
    }
}

/**
 * GitHub API response models
 */
@Serializable
private data class GitHubIssue(
    val number: Int,
    val title: String,
    val body: String? = null,
    val state: String,
    val labels: List<GitHubLabel> = emptyList(),
    val user: GitHubUser? = null,
    val assignees: List<GitHubUser> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toIssueInfo(): IssueInfo {
        return IssueInfo(
            id = number.toString(),
            title = title,
            description = body ?: "",
            labels = labels.map { it.name },
            status = state,
            author = user?.login,
            assignees = assignees.map { it.login },
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

@Serializable
private data class GitHubLabel(
    val name: String,
    val color: String? = null,
    val description: String? = null
)

@Serializable
private data class GitHubUser(
    val login: String,
    val id: Int
)

