package cc.unitmesh.devti.observer.agent

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.settings.devops.devopsPromptsSettings
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import git4idea.push.GitPushListener
import git4idea.push.GitPushRepoResult
import git4idea.repo.GitRepository
import org.kohsuke.github.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class PipelineStatusProcessor(private val project: Project) : AgentProcessor, GitPushListener {
    private val log = Logger.getInstance(PipelineStatusProcessor::class.java)
    private var monitoringJob: ScheduledFuture<*>? = null
    private val timeoutMinutes = 30

    override fun onCompleted(repository: GitRepository, pushResult: GitPushRepoResult) {
        // 检查 push 是否成功
        if (pushResult.type != GitPushRepoResult.Type.SUCCESS) {
            log.info("Push failed, skipping pipeline monitoring")
            return
        }

        // 获取最新的 commit SHA
        val latestCommit = repository.currentRevision
        if (latestCommit == null) {
            log.warn("Could not determine latest commit SHA")
            return
        }

        log.info("Push successful, starting pipeline monitoring for commit: $latestCommit")
        
        // 获取远程仓库信息
        val remoteUrl = getGitHubRemoteUrl(repository)
        if (remoteUrl == null) {
            log.warn("No GitHub remote URL found")
            return
        }

        // 开始监听流水线
        startMonitoring(repository, latestCommit, remoteUrl)
    }

    override fun process() {
        // AgentProcessor 接口要求的方法，这里可以为空
    }

    private fun getGitHubRemoteUrl(repository: GitRepository): String? {
        return repository.remotes.firstOrNull { remote ->
            remote.urls.any { url -> 
                url.contains("github.com") 
            }
        }?.urls?.firstOrNull { it.contains("github.com") }
    }

    private fun startMonitoring(repository: GitRepository, commitSha: String, remoteUrl: String) {
        log.info("Starting pipeline monitoring for commit: $commitSha")
        
        val startTime = System.currentTimeMillis()
        
        monitoringJob = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay({
            try {
                val elapsedMinutes = (System.currentTimeMillis() - startTime) / (1000 * 60)
                
                if (elapsedMinutes >= timeoutMinutes) {
                    log.info("Pipeline monitoring timeout reached for commit: $commitSha")
                    AutoDevNotifications.notify(
                        project,
                        "GitHub Action monitoring timeout (30 minutes) for commit: ${commitSha.take(7)}",
                        NotificationType.WARNING
                    )
                    stopMonitoring()
                    return@scheduleWithFixedDelay
                }
                
                val workflowRun = findWorkflowRunForCommit(remoteUrl, commitSha)
                if (workflowRun != null) {
                    val isComplete = checkWorkflowStatus(workflowRun, commitSha)
                    if (isComplete) {
                        stopMonitoring()
                    }
                }
            } catch (e: Exception) {
                log.error("Error monitoring pipeline for commit: $commitSha", e)
                AutoDevNotifications.notify(
                    project,
                    "Error monitoring GitHub Action: ${e.message}",
                    NotificationType.ERROR
                )
                stopMonitoring()
            }
        }, 30, 30, TimeUnit.SECONDS) // Check every 30 seconds
    }

    private fun findWorkflowRunForCommit(remoteUrl: String, commitSha: String): GHWorkflowRun? {
        try {
            val github = createGitHubConnection()
            val ghRepository = getGitHubRepository(github, remoteUrl) ?: return null
            
            // 获取所有 workflows
            val workflows = ghRepository.listWorkflows().toList()
            
            // 查找与指定 commit 相关的 workflow run
            for (workflow in workflows) {
                val runs = workflow.listRuns()
                    .iterator()
                    .asSequence()
                    .take(10) // 限制检查最近的 10 个运行
                    .find { it.headSha == commitSha }
                
                if (runs != null) {
                    return runs
                }
            }
            
            return null
        } catch (e: Exception) {
            log.error("Error finding workflow run for commit: $commitSha", e)
            return null
        }
    }

    private fun checkWorkflowStatus(workflowRun: GHWorkflowRun, commitSha: String): Boolean {
        return when (workflowRun.status) {
            GHWorkflowRun.Status.COMPLETED -> {
                when (workflowRun.conclusion) {
                    GHWorkflowRun.Conclusion.SUCCESS -> {
                        AutoDevNotifications.notify(
                            project,
                            "✅ GitHub Action completed successfully for commit: ${commitSha.take(7)}",
                            NotificationType.INFORMATION
                        )
                        true
                    }
                    GHWorkflowRun.Conclusion.FAILURE, 
                    GHWorkflowRun.Conclusion.CANCELLED,
                    GHWorkflowRun.Conclusion.TIMED_OUT -> {
                        AutoDevNotifications.notify(
                            project,
                            "❌ GitHub Action failed for commit: ${commitSha.take(7)} - ${workflowRun.conclusion}",
                            NotificationType.ERROR
                        )
                        true
                    }
                    else -> {
                        log.info("Workflow completed with conclusion: ${workflowRun.conclusion}")
                        false
                    }
                }
            }
            GHWorkflowRun.Status.IN_PROGRESS, GHWorkflowRun.Status.QUEUED -> {
                log.info("Workflow still running: ${workflowRun.status}")
                false
            }
            else -> {
                log.info("Unknown workflow status: ${workflowRun.status}")
                false
            }
        }
    }

    private fun createGitHubConnection(): GitHub {
        val token = project.devopsPromptsSettings?.githubToken
        return if (token.isNullOrBlank()) {
            GitHub.connectAnonymously()
        } else {
            GitHub.connectUsingOAuth(token)
        }
    }

    private fun getGitHubRepository(github: GitHub, remoteUrl: String): GHRepository? {
        try {
            val repoPath = extractRepositoryPath(remoteUrl) ?: return null
            return github.getRepository(repoPath)
        } catch (e: Exception) {
            log.error("Error getting GitHub repository from URL: $remoteUrl", e)
            return null
        }
    }

    private fun extractRepositoryPath(remoteUrl: String): String? {
        // Handle both HTTPS and SSH URLs
        val httpsPattern = Regex("https://github\\.com/([^/]+/[^/]+)(?:\\.git)?/?")
        val sshPattern = Regex("git@github\\.com:([^/]+/[^/]+)(?:\\.git)?/?")
        
        return httpsPattern.find(remoteUrl)?.groupValues?.get(1)
            ?: sshPattern.find(remoteUrl)?.groupValues?.get(1)
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel(false)
        monitoringJob = null
        log.info("Pipeline monitoring stopped")
    }
}