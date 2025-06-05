package cc.unitmesh.devti.observer

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.provider.observer.AgentObserver
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.settings.devops.devopsPromptsSettings
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.concurrency.AppExecutorUtil
import git4idea.push.GitPushListener
import git4idea.push.GitPushRepoResult
import git4idea.repo.GitRepository
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHWorkflowRun
import org.kohsuke.github.GHWorkflowJob
import org.kohsuke.github.GitHub
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class PipelineStatusProcessor : AgentObserver, GitPushListener {
    private val log = Logger.getInstance(PipelineStatusProcessor::class.java)
    private var monitoringJob: ScheduledFuture<*>? = null
    /// we limit for 4mins delay + 1mins networks (maybe) request + 30mins
    private val timeoutMinutes = 35
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
        }, 4, 5, TimeUnit.MINUTES)  // 1分钟后开始第一次检查，然后每5分钟检查一次
    }

    private fun findWorkflowRunForCommit(remoteUrl: String, commitSha: String): GHWorkflowRun? {
        try {
            val github = createGitHubConnection()
            val ghRepository = getGitHubRepository(github, remoteUrl) ?: return null

            val allRuns = ghRepository.queryWorkflowRuns()
                .list()
                .iterator()
                .asSequence()
                .take(50)  // 检查最近50个runs
                .toList()
            
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
                    GHWorkflowRun.Conclusion.SUCCESS -> true
                    GHWorkflowRun.Conclusion.FAILURE -> {
                        handleWorkflowFailure(workflowRun, commitSha)
                        true
                    }
                    GHWorkflowRun.Conclusion.CANCELLED,
                    GHWorkflowRun.Conclusion.TIMED_OUT -> {
                        true
                    }
                    else -> {
                        log.info("Workflow completed with conclusion: ${workflowRun.conclusion}")
                        false
                    }
                }
            }
            GHWorkflowRun.Status.IN_PROGRESS, GHWorkflowRun.Status.QUEUED -> false
            else -> false
        }
    }

    private fun handleWorkflowFailure(workflowRun: GHWorkflowRun, commitSha: String) {
        try {
            val failureDetails = getWorkflowFailureDetails(workflowRun)
            val detailedMessage = buildDetailedFailureMessage(workflowRun, commitSha, failureDetails)

            AutoDevNotifications.error(project!!, detailedMessage)
        } catch (e: Exception) {
            val fallbackMessage = if (e.message?.contains("admin rights", ignoreCase = true) == true ||
                                     e.message?.contains("403", ignoreCase = true) == true) {
                "❌ GitHub Action failed for commit: ${commitSha.take(7)} - ${workflowRun.conclusion}\n" +
                "⚠️ Detailed logs unavailable - admin rights required\n" +
                "View details at: ${workflowRun.htmlUrl}"
            } else {
                "❌ GitHub Action failed for commit: ${commitSha.take(7)} - ${workflowRun.conclusion}\n" +
                "URL: ${workflowRun.htmlUrl}\n" +
                "Error getting details: ${e.message}"
            }
            AutoDevNotifications.error(project!!, fallbackMessage)
        }
    }

    private fun getWorkflowFailureDetails(workflowRun: GHWorkflowRun): WorkflowFailureDetails {
        val failureDetails = WorkflowFailureDetails()
        try {
            val jobs = workflowRun.listJobs().toList()
            
            for (job in jobs) {
                if (job.conclusion == GHWorkflowRun.Conclusion.FAILURE) {
                    val jobFailure = JobFailure(
                        jobName = job.name,
                        errorSteps = extractFailedSteps(job),
                        logs = getJobLogsFromAPI(workflowRun, job),
                        jobUrl = job.htmlUrl?.toString()
                    )
                    failureDetails.failedJobs.add(jobFailure)
                }
            }
            
            if (failureDetails.failedJobs.isEmpty()) {
                failureDetails.workflowLogs = getWorkflowLogsFromAPI(workflowRun)
            }
            
        } catch (e: Exception) {
            log.error("Error getting workflow failure details", e)
            failureDetails.error = e.message
        }
        
        return failureDetails
    }

    private fun extractFailedSteps(job: GHWorkflowJob): List<String> {
        val failedSteps = mutableListOf<String>()
        
        try {
            val steps = job.steps
            for (step in steps) {
                if (step.conclusion == GHWorkflowRun.Conclusion.FAILURE) {
                    failedSteps.add("${step.name}: ${step.conclusion} (${step.number})")
                }
            }
        } catch (e: Exception) {
            log.error("Error extracting failed steps", e)
        }
        
        return failedSteps
    }

    private fun getJobLogsFromAPI(workflowRun: GHWorkflowRun, job: GHWorkflowJob): String? {
        return try {
            job.downloadLogs { logStream ->
                logStream.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        reader.readText()
                    }
                }
            }
        } catch (e: Exception) {
            log.warn("Cannot download job logs for job: ${job.name} - ${e.message}")
            if (e.message?.contains("admin rights", ignoreCase = true) == true ||
                e.message?.contains("403", ignoreCase = true) == true) {
                log.info("Admin rights required to download job logs. Falling back to alternative approach.")
                return "⚠️ Job logs unavailable - admin rights required to download logs from GitHub Actions.\n" +
                       "Job URL: ${job.htmlUrl}\n" +
                       "You can view the logs directly at: ${job.htmlUrl}"
            }

            // Try alternative API approach for other errors
            getLogsViaDirectAPI(workflowRun, job.id)
        }
    }

    private fun getWorkflowLogsFromAPI(workflowRun: GHWorkflowRun): String? {
        return try {
            workflowRun.downloadLogs { logStream ->
                ZipInputStream(logStream).use { zipStream ->
                    val logContents = StringBuilder()
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".txt")) {
                            val content = zipStream.readBytes().toString(Charsets.UTF_8)
                            logContents.append("=== ${entry.name} ===\n")
                            logContents.append(content)
                            logContents.append("\n\n")
                        }
                        entry = zipStream.nextEntry
                    }
                    logContents.toString()
                }
            }
        } catch (e: Exception) {
            log.warn("Cannot download workflow logs - ${e.message}")
            if (e.message?.contains("admin rights", ignoreCase = true) == true ||
                e.message?.contains("403", ignoreCase = true) == true) {
                log.info("Admin rights required to download workflow logs.")
                return "⚠️ Workflow logs unavailable - admin rights required to download logs from GitHub Actions.\n" +
                       "Workflow URL: ${workflowRun.htmlUrl}\n" +
                       "You can view the logs directly at: ${workflowRun.htmlUrl}"
            }

            null
        }
    }

    private fun getLogsViaDirectAPI(workflowRun: GHWorkflowRun, jobId: Long): String? {
        return try {
            val token = project?.devopsPromptsSettings?.githubToken
            val repoPath = extractRepositoryPath(workflowRun.repository.htmlUrl.toString())
            val apiUrl = "https://api.github.com/repos/$repoPath/actions/jobs/$jobId/logs"
            
            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            
            if (!token.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            
            if (connection.responseCode == 302) {
                val redirectUrl = connection.getHeaderField("Location")
                if (redirectUrl != null) {
                    val logConnection = URL(redirectUrl).openConnection() as HttpURLConnection
                    logConnection.inputStream.use { stream ->
                        BufferedReader(InputStreamReader(stream)).use { reader ->
                            reader.readText()
                        }
                    }
                } else null
            } else null
        } catch (e: Exception) {
            log.error("Error getting logs via direct API", e)
            null
        }
    }

    private fun buildDetailedFailureMessage(
        workflowRun: GHWorkflowRun,
        commitSha: String,
        failureDetails: WorkflowFailureDetails
    ): String {
        val message = StringBuilder()
        message.append("❌ GitHub Action failed for commit: ${commitSha.take(7)}\n")
        message.append("Workflow: ${workflowRun.name}\n")
        message.append("URL: ${workflowRun.htmlUrl}\n\n")

        if (failureDetails.error != null) {
            message.append("Error getting details: ${failureDetails.error}\n")
        } else if (failureDetails.failedJobs.isNotEmpty()) {
            message.append("Failed Jobs:\n")

            for (jobFailure in failureDetails.failedJobs) { // 显示所有失败的job
                message.append("• ${jobFailure.jobName}\n")
                if (jobFailure.jobUrl != null) {
                    message.append("  URL: ${jobFailure.jobUrl}\n")
                }

                if (jobFailure.errorSteps.isNotEmpty()) {
                    message.append("  Failed steps:\n")
                    for (step in jobFailure.errorSteps) { // 显示所有失败步骤
                        message.append("    - $step\n")
                    }
                }

                jobFailure.logs?.let { logs ->
                    if (logs.isNotBlank()) {
                        message.append("  Logs:\n")
                        logs.lines().forEach { line ->
                            if (line.isNotBlank()) {
                                message.append("    $line\n")
                            }
                        }
                    }
                }
                message.append("\n")
            }
        } else if (failureDetails.workflowLogs != null) {
            message.append("Workflow Logs:\n")
            failureDetails.workflowLogs!!.lines().forEach { line ->
                if (line.isNotBlank()) {
                    message.append("  $line\n")
                }
            }
        }

        return message.toString()
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

    // 数据类用于存储失败详情
    private data class WorkflowFailureDetails(
        val failedJobs: MutableList<JobFailure> = mutableListOf(),
        var workflowLogs: String? = null,
        var error: String? = null
    )

    private data class JobFailure(
        val jobName: String,
        val errorSteps: List<String>,
        val logs: String?,
        val jobUrl: String? = null
    )
}