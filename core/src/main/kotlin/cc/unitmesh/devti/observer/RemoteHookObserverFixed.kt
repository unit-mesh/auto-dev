package cc.unitmesh.devti.observer

import cc.unitmesh.devti.observer.agent.AgentProcessor
import cc.unitmesh.devti.provider.observer.AgentObserver
import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import cc.unitmesh.devti.flow.model.SimpleStory
import cc.unitmesh.devti.settings.devops.devopsPromptsSettings
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.dvcs.repo.VcsRepositoryManager
import git4idea.repo.GitRepository
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

            repositories.forEach { repo ->
                processRepository(repo)
            }
        } catch (e: Exception) {
            log.error("Error processing issues", e)
        }
    }

    private fun processRepository(repo: GitRepository) {
        try {
            val remoteUrl = repo.remotes.firstOrNull()?.firstUrl
            if (remoteUrl == null) {
                log.debug("No remote URL found for repository: ${repo.root.path}")
                return
            }

            if (!remoteUrl.contains("github.com")) {
                log.debug("Repository is not a GitHub repository: $remoteUrl")
                return
            }

            val github = GitHubIssue(remoteUrl, getGitHubToken())
            val latestIssues = getLatestIssues(github)
            
            latestIssues.forEach { issueId ->
                if (!processedIssues.contains(issueId)) {
                    processNewIssue(github, issueId)
                    processedIssues.add(issueId)
                }
            }
        } catch (e: Exception) {
            log.error("Error processing repository: ${repo.root.path}", e)
        }
    }
    
    private fun processNewIssue(github: GitHubIssue, issueId: String) {
        try {
            val story = github.getStoryById(issueId)
            log.info("Processing new issue: ${story.title}")
            notifyNewIssue(story)
        } catch (e: Exception) {
            log.error("Error processing issue $issueId", e)
        }
    }
    
    private fun getLatestIssues(github: GitHubIssue): List<String> {
        return try {
            // Get issues from the last 24 hours
            github.getRecentIssues(1)
        } catch (e: Exception) {
            log.error("Error fetching latest issues", e)
            emptyList()
        }
    }

    private fun getGitRepositories(): List<GitRepository> {
        val vcsManager = ProjectLevelVcsManager.getInstance(project)
        val repositoryManager = VcsRepositoryManager.getInstance(project)
        
        return vcsManager.allVcsRoots
            .mapNotNull { vcsRoot: VcsRoot ->
                repositoryManager.getRepositoryForRoot(vcsRoot.path) as? GitRepository
            }
    }

    private fun getGitHubToken(): String {
        // TODO: Implement proper token retrieval from settings or environment
        return System.getenv("GITHUB_TOKEN") ?: ""
    }

    private fun notifyNewIssue(story: SimpleStory) {
        val prompt = buildIssuePrompt(story)
        sendErrorNotification(project, prompt)
    }
    
    private fun buildIssuePrompt(story: SimpleStory): String {
        val prompts = devopsPromptsSettings.prompts
        val issuePrompt = prompts.find { it.title == "Issue" }?.content ?: "New issue: \${title}"
        
        return issuePrompt
            .replace("\${title}", story.title)
            .replace("\${description}", story.description)
    }

    private fun sendErrorNotification(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AutoDev.Issue")
            .createNotification("New Issue Detected", message, NotificationType.INFORMATION)
            .notify(project)
    }
}