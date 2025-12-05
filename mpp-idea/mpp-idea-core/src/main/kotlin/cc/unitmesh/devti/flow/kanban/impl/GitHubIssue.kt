package cc.unitmesh.devti.flow.kanban.impl

import cc.unitmesh.devti.flow.kanban.Kanban
import cc.unitmesh.devti.flow.model.SimpleStory
import cc.unitmesh.devti.settings.devops.devopsPromptsSettings
import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.openapi.project.Project
// import git4idea.repo.GitRepository // Removed in platform 252
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHRepository
import java.time.Instant
import kotlin.text.contains

class GitHubIssue(var repoUrl: String, val token: String) : Kanban {
    private val gitHub: GitHub

    init {
        if (repoUrl.startsWith("https") || repoUrl.startsWith("git")) {
            repoUrl = formatUrl(repoUrl)
        }

        try {
            gitHub = GitHubBuilder()
                .withOAuthToken(token)
                .build()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize GitHub client: ${e.message}", e)
        }
    }

    fun formatUrl(repoUrl: String): String {
        var url = repoUrl.split("/").takeLast(2).joinToString("/")
        url = if (url.endsWith(".git")) url.substring(0, url.length - 4) else url
        return url
    }

    override fun getStoryById(storyId: String): SimpleStory {
        val issue = gitHub.getRepository(repoUrl).getIssue(Integer.parseInt(storyId))
        if (issue.comments.size == 0) {
            return SimpleStory(issue.number.toString(), issue.title, issue.body ?: "")
        }

        // get all comments and filter body contains "用户故事"
        val comments = issue.comments
        val comment = comments.find { it.body.contains("用户故事") }
        if (comment != null) {
            return SimpleStory(issue.number.toString(), issue.title, comment.body)
        }

        return SimpleStory(issue.number.toString(), issue.title, issue.body ?: "")
    }

    /**
     * Get recent issue IDs (within last 24 hours)
     */
    fun getRecentIssueIds(): List<String> {
        return try {
            val repository = gitHub.getRepository(repoUrl)
            val yesterday = Instant.now().minusSeconds(24 * 60 * 60)

            repository.getIssues(GHIssueState.ALL)
                .filter { it.createdAt.toInstant().isAfter(yesterday) }
                .map { it.number.toString() }
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch recent issues: ${e.message}", e)
        }
    }

    companion object {
        fun getGitHubRepository(project: Project, remoteUrl: String): GHRepository? {
            val github = createGitHubConnection(project)
            return try {
                extractRepositoryPath(remoteUrl)?.let { repoPath ->
                    github.getRepository(repoPath)
                }
            } catch (e: Exception) {
                null
            }
        }

        fun createGitHubConnection(project: Project): GitHub {
            val token = project.devopsPromptsSettings.githubToken
            return if (token.isEmpty()) {
                GitHub.connectAnonymously()
            } else {
                GitHub.connectUsingOAuth(token)
            }
        }

        private fun extractRepositoryPath(remoteUrl: String): String? {
            val httpsPattern = Regex("https://github\\.com/([^/]+/[^/]+)(?:\\.git)?/?")
            val sshPattern = Regex("git@github\\.com:([^/]+/[^/]+)(?:\\.git)?/?")

            return httpsPattern.find(remoteUrl)?.groupValues?.get(1)
                ?: sshPattern.find(remoteUrl)?.groupValues?.get(1)
        }

        fun parseGitHubRemoteUrl(repository: Any): String? {
            // GitRepository API changed in platform 252
            return null // TODO: Restore when API stabilizes
        }

        fun parseGitHubRepository(project: Project): GHRepository? {
            val repositoryManager: VcsRepositoryManager = VcsRepositoryManager.getInstance(project)
            val repository = repositoryManager.getRepositoryForFile(project.baseDir)

            if (repository == null) {
                return null
            }

            // GitRepository type check removed in platform 252
            // if (repository !is GitRepository) {
            //     return null
            // }

            val remoteUrl = parseGitHubRemoteUrl(repository) ?: return null
            return getGitHubRepository(project, remoteUrl)
        }

        fun isGitHubRepository(project: Project): Boolean {
            val repositoryManager: VcsRepositoryManager = VcsRepositoryManager.getInstance(project)
            val repository = repositoryManager.getRepositoryForFile(project.baseDir)

            if (repository == null) {
                return false
            }

            // GitRepository type check removed in platform 252
            // if (repository !is GitRepository) {
            //     return false
            // }

            val remoteUrl = parseGitHubRemoteUrl(repository) ?: return false
            if (remoteUrl.contains("github.com")) {
                return true
            }

            return false
        }
    }
}