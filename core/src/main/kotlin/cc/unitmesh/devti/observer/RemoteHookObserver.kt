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
import java.util.concurrent.TimeUnit

/**
 * Remote Hook observer will receive the remote hook event and process it.
 * like:
 * - [ ] Jira issue
 * - [ ] GitHub/Gitlab issue
 * and Trigger after processor, and send the notification to the chat window.
 */
open class RemoteHookObserver : AgentObserver {
    private var issueWorker: IssueWorker? = null
    
    override fun onRegister(project: Project) {
//        issueWorker = IssueWorker(project)
//        // Schedule periodic checks for new issues
//        AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
//            { issueWorker?.process() },
//            0, 30, TimeUnit.MINUTES
//        )
    }
}

class IssueWorker(private val project: Project) : AgentProcessor {
    private val log = logger<IssueWorker>()
    private val processedIssues = mutableSetOf<String>()
    
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
    
    private fun processRepository(repository: GitRepository) {
        val remoteUrl = repository.remotes.firstOrNull()?.firstUrl ?: return
        
        if (remoteUrl.contains("github.com")) {
            processGitHubIssues(remoteUrl)
        } else if (remoteUrl.contains("gitlab")) {
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
            // In a real implementation, we'd fetch recent issues and filter for new ones
            // For now, let's assume we have a way to get the latest issue ID
            val latestIssueId = getLatestIssueId(github)
            
            if (latestIssueId != null && !processedIssues.contains(latestIssueId)) {
                val story = github.getStoryById(latestIssueId)
                if (story != null) {
                    processedIssues.add(latestIssueId)
                    notifyNewIssue(story)
                }
            }
        } catch (e: Exception) {
            log.warn("Error processing GitHub issues for repo: $repoUrl", e)
        }
    }
    
    private fun getLatestIssueId(github: GitHubIssue): String? {
        // This is a placeholder - in a complete implementation, 
        // we would query the API for the latest issues
        // For now, return null to indicate no new issues
        return null
    }
    
    private fun notifyNewIssue(story: SimpleStory) {
        val prompt = buildIssuePrompt(story)
//        sendErrorNotification(project, prompt)
    }
    
    private fun buildIssuePrompt(story: SimpleStory): String {
        return """
            New issue received: #${story.id} - ${story.title}
            
            ```
            ${story.description}
            ```
            
            How would you like to proceed with this issue?
        """.trimIndent()
    }
    
    private fun getGitRepositories(): List<GitRepository> {
        val vcsManager = ProjectLevelVcsManager.getInstance(project)
        val roots: Array<VcsRoot> = vcsManager.allVcsRoots
        val repositoryManager = VcsRepositoryManager.getInstance(project)
        
        return roots.mapNotNull { root ->
            val repo = repositoryManager.getRepositoryForRoot(root.path)
            if (repo is GitRepository) repo else null
        }
    }
}
