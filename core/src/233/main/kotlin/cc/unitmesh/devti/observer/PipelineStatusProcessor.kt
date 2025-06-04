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
import com.intellij.util.messages.MessageBusConnection
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
        }, 0, 1, TimeUnit.MINUTES)  // 1分钟后开始第一次检查，然后每5分钟检查一次
    }

    private fun findWorkflowRunForCommit(remoteUrl: String, commitSha: String): GHWorkflowRun? {
        try {
            val github = createGitHubConnection()
            val ghRepository = getGitHubRepository(github, remoteUrl) ?: return null

            // 使用 queryWorkflowRuns 查询workflow runs
            val allRuns = ghRepository.queryWorkflowRuns()
                .list()
                .iterator()
                .asSequence()
                .take(50)  // 检查最近50个runs
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
                    GHWorkflowRun.Conclusion.FAILURE -> {
                        handleWorkflowFailure(workflowRun, commitSha)
                        true
                    }
                    GHWorkflowRun.Conclusion.CANCELLED,
                    GHWorkflowRun.Conclusion.TIMED_OUT -> {
                        AutoDevNotifications.notify(
                            project!!,
                            "❌ GitHub Action ${workflowRun.conclusion.toString().lowercase()} for commit: ${commitSha.take(7)}",
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

    private fun handleWorkflowFailure(workflowRun: GHWorkflowRun, commitSha: String) {
        try {
            log.info("Analyzing workflow failure for commit: $commitSha")
            
            // 获取失败的构建详情
            val failureDetails = getWorkflowFailureDetails(workflowRun)
            
            // 构建详细的错误通知
            val detailedMessage = buildDetailedFailureMessage(workflowRun, commitSha, failureDetails)
            
            AutoDevNotifications.notify(
                project!!,
                detailedMessage,
                NotificationType.ERROR
            )
            
            // 记录详细日志
            log.info("Workflow failure details for commit $commitSha: $failureDetails")
            
        } catch (e: Exception) {
            log.error("Error analyzing workflow failure for commit: $commitSha", e)
            // 回退到简单通知
            AutoDevNotifications.notify(
                project!!,
                "❌ GitHub Action failed for commit: ${commitSha.take(7)} - ${workflowRun.conclusion}\nURL: ${workflowRun.htmlUrl}",
                NotificationType.ERROR
            )
        }
    }

    private fun getWorkflowFailureDetails(workflowRun: GHWorkflowRun): WorkflowFailureDetails {
        val failureDetails = WorkflowFailureDetails()
        
        try {
            // 获取所有jobs
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
            
            // 如果没有具体的job失败信息，尝试获取整个workflow的日志
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
            // 尝试使用 GitHub Java API 获取日志
            job.downloadLogs { logStream ->
                logStream.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        val logs = reader.readText()
                        // 提取关键错误信息（最后2000个字符，通常包含错误信息）
                        if (logs.length > 2000) {
                            "...\n" + logs.takeLast(2000)
                        } else logs
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Error downloading job logs for job: ${job.name}", e)
            // 如果GitHub Java API失败，尝试直接API调用
            getLogsViaDirectAPI(workflowRun, job.id)
        }
    }

    private fun getWorkflowLogsFromAPI(workflowRun: GHWorkflowRun): String? {
        return try {
            // 使用GitHub Java API获取整个workflow的日志
            workflowRun.downloadLogs { logStream ->
                // 处理ZIP格式的日志文件
                ZipInputStream(logStream).use { zipStream ->
                    val logContents = StringBuilder()
                    var entry = zipStream.nextEntry
                    while (entry != null && logContents.length < 5000) { // 限制日志长度
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
            log.error("Error downloading workflow logs", e)
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
                // 处理重定向到日志下载链接
                val redirectUrl = connection.getHeaderField("Location")
                if (redirectUrl != null) {
                    val logConnection = URL(redirectUrl).openConnection() as HttpURLConnection
                    logConnection.inputStream.use { stream ->
                        BufferedReader(InputStreamReader(stream)).use { reader ->
                            val logs = reader.readText()
                            if (logs.length > 2000) {
                                "...\n" + logs.takeLast(2000)
                            } else {
                                logs
                            }
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
            
            for (jobFailure in failureDetails.failedJobs.take(2)) { // 最多显示2个失败的job
                message.append("• ${jobFailure.jobName}\n")
                if (jobFailure.jobUrl != null) {
                    message.append("  URL: ${jobFailure.jobUrl}\n")
                }
                
                if (jobFailure.errorSteps.isNotEmpty()) {
                    message.append("  Failed steps:\n")
                    for (step in jobFailure.errorSteps.take(3)) { // 最多显示3个失败步骤
                        message.append("    - $step\n")
                    }
                }
                
                // 添加关键错误信息
                jobFailure.logs?.let { logs ->
                    val errorLines = extractKeyErrorLines(logs)
                    if (errorLines.isNotEmpty()) {
                        message.append("  Key errors:\n")
                        for (errorLine in errorLines.take(3)) { // 最多显示3行关键错误
                            message.append("    ${errorLine.take(100)}\n") // 限制每行长度
                        }
                    }
                }
                message.append("\n")
            }
        } else if (failureDetails.workflowLogs != null) {
            val errorLines = extractKeyErrorLines(failureDetails.workflowLogs!!)
            if (errorLines.isNotEmpty()) {
                message.append("Key errors:\n")
                for (errorLine in errorLines.take(5)) {
                    message.append("  ${errorLine.take(100)}\n")
                }
            }
        }
        
        return message.toString()
    }

    private fun extractKeyErrorLines(logs: String): List<String> {
        val errorPatterns = listOf(
            "Error:",
            "Exception:",
            "Failed:",
            "FAILED:",
            "BUILD FAILED",
            "npm ERR!",
            "fatal:",
            "Traceback",
            "AssertionError",
            "SyntaxError",
            "CompileError",
            "✗",
            "FAIL:",
            "stderr:"
        )
        
        return logs.lines()
            .filter { line ->
                errorPatterns.any { pattern ->
                    line.contains(pattern, ignoreCase = true)
                }
            }
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 10 } // 过滤太短的行
            .distinct()
            .take(10) // 最多返回10行
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