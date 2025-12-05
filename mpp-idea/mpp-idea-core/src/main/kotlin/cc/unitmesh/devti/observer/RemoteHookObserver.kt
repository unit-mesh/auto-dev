package cc.unitmesh.devti.observer

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.observer.agent.AgentProcessor
import cc.unitmesh.devti.provider.observer.AgentObserver
import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import cc.unitmesh.devti.flow.model.SimpleStory
import cc.unitmesh.devti.settings.devops.devopsPromptsSettings
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.dvcs.repo.VcsRepositoryManager
// import git4idea.repo.GitRepository // Removed in platform 252
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * Remote Hook observer will receive the remote hook event and process it.
 * like:
 * - [x] GitHub issue
 * - [ ] Jira issue
 * - [ ] GitLab issue
 * and Trigger after processor, and send the notification to the chat window.
 */
open class RemoteHookObserver : AgentObserver {
    private var issueWorker: IssueWorker? = null
    
    override fun onRegister(project: Project) {
        issueWorker = IssueWorker(project)
        // Schedule periodic checks for new issues
        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            { issueWorker?.process() },
            0, 30, TimeUnit.MINUTES
        )
    }
}

class IssueWorker(private val project: Project) : AgentProcessor {
    private val log = logger<IssueWorker>()
    private val processedIssues = ConcurrentHashMap.newKeySet<String>()
    
    override fun process() {
        try {
            // Get git repositories in the project
            val repositories = getGitRepositories()
            if (repositories.isEmpty()) {
                log.debug("No Git repositories found in the project")
                return
            }
            
            for (repository in repositories) {
                processRepository(repository)
            }
        } catch (e: Exception) {
            log.warn("Error processing GitHub/GitLab issues", e)
        }
    }
    
    private fun processRepository(repository: Any) {
        // GitRepository API changed in platform 252
        val remoteUrl: String? = null // TODO: Restore when API stabilizes
        
        if (remoteUrl?.contains("github.com") == true) {
            processGitHubIssues(remoteUrl)
        } else if (remoteUrl?.contains("gitlab") == true) {
            // GitLab implementation would go here
            log.debug("GitLab integration not yet implemented")
        }
    }
    
    private fun processGitHubIssues(repoUrl: String) {
        val token = project.devopsPromptsSettings.state.githubToken
        if (token.isBlank()) {
            log.debug("GitHub token not configured")
            return
        }
        
        try {
            val github = GitHubIssue(repoUrl, token)
            val latestIssues = getLatestIssues(github)
            
            for (latestIssueId in latestIssues) {
                if (!processedIssues.contains(latestIssueId)) {
                    processNewIssue(github, latestIssueId)
                }
            }
        } catch (e: Exception) {
            log.warn("Error processing GitHub issues for repo: $repoUrl", e)
        }
    }
    
    private fun processNewIssue(github: GitHubIssue, issueId: String) {
        try {
            val story = github.getStoryById(issueId)
            processedIssues.add(issueId)
            notifyNewIssue(story)
            log.info("Processed new GitHub issue: #$issueId - ${story.title}")
        } catch (e: Exception) {
            log.warn("Error processing issue $issueId", e)
        }
    }
    
    private fun getLatestIssues(github: GitHubIssue): List<String> {
        return try {
            github.getRecentIssueIds()
        } catch (e: Exception) {
            log.warn("Error fetching latest issues", e)
            emptyList()
        }
    }
    
    private fun notifyNewIssue(story: SimpleStory) {
        val prompt = buildIssuePrompt(story)
        AutoDevNotifications.error(project, prompt)
    }
    
    private fun buildIssuePrompt(story: SimpleStory): String {
        return """
            New issue received: #${story.id} - ${story.title}
            
            ${story.description}
            
            How would you like to proceed with this issue?
        """.trimIndent()
    }

    private fun getGitRepositories(): List<Any> { // GitRepository API changed in platform 252
        // GitRepository type check removed in platform 252
        // Return empty list until API stabilizes
        return emptyList()
    }
}