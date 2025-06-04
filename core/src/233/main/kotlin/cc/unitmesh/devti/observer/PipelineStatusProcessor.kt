package cc.unitmesh.devti.observer

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.provider.observer.AgentObserver
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.settings.devops.devopsPromptsSettings
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.messages.MessageBusConnection
import git4idea.push.GitPushListener
import git4idea.push.GitPushRepoResult
import git4idea.repo.GitRepository
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHWorkflowRun
import org.kohsuke.github.GitHub
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class PipelineStatusProcessor : AgentObserver, GitPushListener {
    private val log = Logger.getInstance(PipelineStatusProcessor::class.java)
    private var monitoringJob: ScheduledFuture<*>? = null
    private val timeoutMinutes = 30
    private var project: Project? = null

    override fun onCompleted(repository: GitRepository, pushResult: GitPushRepoResult) {
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        this.project = project
        if (project == null) {
            log.warn("Cannot get project from component: $this")
            return
        }
        if (!project.coderSetting.state.enableObserver) return

        if (pushResult.type != GitPushRepoResult.Type.SUCCESS) {
            log.info("Push failed, skipping pipeline monitoring")
            return
        }

        val latestCommit = repository.currentRevision
        if (latestCommit == null) {
            log.warn("Could not determine latest commit SHA")
            return
        }

        log.info("Push successful, starting pipeline monitoring for commit: $latestCommit")

        val remoteUrl = getGitHubRemoteUrl(repository)
        if (remoteUrl == null) {
            log.warn("No GitHub remote URL found")
            return
        }

        startMonitoring(repository, latestCommit, remoteUrl)
    }

    private var connection: MessageBusConnection? = null

    override fun onRegister(project: Project) {
        this.project = project;
        if (!project.coderSetting.state.enableObserver) return
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

        var workflowNotFoundCount = 0
        val maxWorkflowNotFoundAttempts = 3  // 如果连续3次找不到workflow，停止监控
        val startTime = System.currentTimeMillis()

        monitoringJob = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay({
            try {
                val elapsedMinutes = (System.currentTimeMillis() - startTime) / (1000 * 60)

                if (elapsedMinutes >= timeoutMinutes) {
                    log.info("Pipeline monitoring timeout reached for commit: $commitSha")
                    AutoDevNotifications.notify(
                        project!!,
                        "GitHub Action monitoring timeout (30 minutes) for commit: ${commitSha.take(7)}",
                        NotificationType.WARNING
                    )
                    stopMonitoring()
                    return@scheduleWithFixedDelay
                }

                val workflowRun = findWorkflowRunForCommit(remoteUrl, commitSha)
                if (workflowRun != null) {
                    workflowNotFoundCount = 0  // 重置计数器
                    val isComplete = checkWorkflowStatus(workflowRun, commitSha)
                    if (isComplete) {
                        stopMonitoring()
                    }
                } else {
                    workflowNotFoundCount++
                    log.info("Workflow not found for commit: $commitSha (attempt $workflowNotFoundCount/$maxWorkflowNotFoundAttempts)")
                    if (workflowNotFoundCount >= maxWorkflowNotFoundAttempts) {
                        log.info("No workflow found after $maxWorkflowNotFoundAttempts attempts, stopping monitoring for commit: $commitSha")
                        AutoDevNotifications.notify(
                            project!!,
                            "No GitHub Action workflow found for commit: ${commitSha.take(7)}",
                            NotificationType.INFORMATION
                        )
                        stopMonitoring()
                    }
                }
            } catch (e: Exception) {
                log.error("Error monitoring pipeline for commit: $commitSha", e)
                AutoDevNotifications.notify(
                    project!!,
                    "Error monitoring GitHub Action: ${e.message}",
                    NotificationType.ERROR
                )
                stopMonitoring()
            }
        }, 1, 5, TimeUnit.MINUTES)  // 1分钟后开始第一次检查，然后每5分钟检查一次
    }

    private fun findWorkflowRunForCommit(remoteUrl: String, commitSha: String): GHWorkflowRun? {
        try {
            val github = createGitHubConnection()
            val ghRepository = getGitHubRepository(github, remoteUrl) ?: return null

            // 使用 queryWorkflowRuns 查询workflow runs
            // 这样可以减少API调用次数
            val allRuns = ghRepository.queryWorkflowRuns()
                .list()
                .iterator()
                .asSequence()
                .take(50)
                .toList()
            
            // 查找匹配commit SHA的workflow run
            val matchingRun = allRuns.find { it.headSha == commitSha }
            
            if (matchingRun != null) {
                log.info("Found workflow run for commit $commitSha: ${matchingRun.name} (${matchingRun.status})")
                return matchingRun
            } else {
                log.debug("No workflow run found for commit $commitSha in recent ${allRuns.size} runs")
                return null
            }
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
                            project!!,
                            "✅ GitHub Action completed successfully for commit: ${commitSha.take(7)}",
                            NotificationType.INFORMATION
                        )
                        true
                    }
                    GHWorkflowRun.Conclusion.FAILURE,
                    GHWorkflowRun.Conclusion.CANCELLED,
                    GHWorkflowRun.Conclusion.TIMED_OUT -> {
                        AutoDevNotifications.notify(
                            project!!,
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
        val token = project?.devopsPromptsSettings?.githubToken
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