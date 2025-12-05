package cc.unitmesh.devti.observer

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.flow.kanban.impl.GitHubIssue
import cc.unitmesh.devti.provider.observer.AgentObserver
import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.concurrency.AppExecutorUtil
import git4idea.push.GitPushListener
import git4idea.push.GitPushRepoResult
import git4idea.repo.GitRepository
import org.kohsuke.github.GHWorkflowRun
import org.kohsuke.github.GHWorkflowJob
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class PipelineStatusProcessor : AgentObserver, GitPushListener {
    private val log = Logger.getInstance(PipelineStatusProcessor::class.java)
    private var monitoringJob: ScheduledFuture<*>? = null
    private val timeoutMinutes = 35
    private var project: Project? = null

    override fun onCompleted(repository: GitRepository, pushResult: GitPushRepoResult) {
        ProjectManager.getInstance().openProjects.firstOrNull()?.let { currentProject ->
            this.project = currentProject

            if (!currentProject.coderSetting.state.enableObserver) {
                return
            }

            if (pushResult.type != GitPushRepoResult.Type.SUCCESS) {
                log.info("Push failed, skipping pipeline monitoring")
                return
            }

            repository.currentRevision?.let { latestCommit ->
                log.info("Push successful, starting pipeline monitoring for commit: $latestCommit")

                GitHubIssue.parseGitHubRemoteUrl(repository)?.let { remoteUrl ->
                    startMonitoring(repository, latestCommit, remoteUrl)
                } ?: log.warn("No GitHub remote URL found")
            } ?: log.warn("Could not determine latest commit SHA")
        } ?: log.warn("Cannot get project from component: $this")
    }

    override fun onRegister(project: Project) {
        this.project = project
    }

    private fun startMonitoring(repository: GitRepository, commitSha: String, remoteUrl: String) {
        log.info("Starting pipeline monitoring for commit: $commitSha")

        var workflowNotFoundCount = 0
        val maxWorkflowNotFoundAttempts = 3
        val startTime = System.currentTimeMillis()

        monitoringJob = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay({
            try {
                val elapsedMinutes = (System.currentTimeMillis() - startTime) / (1000 * 60)

                if (elapsedMinutes >= timeoutMinutes) {
                    log.info("Pipeline monitoring timeout reached for commit: $commitSha")
                    stopMonitoring()
                    return@scheduleWithFixedDelay
                }

                findWorkflowRunForCommit(remoteUrl, commitSha)?.let { workflowRun ->
                    workflowNotFoundCount = 0

                    val isComplete = checkWorkflowStatus(workflowRun, commitSha)
                    if (isComplete) {
                        stopMonitoring()
                    }
                } ?: run {
                    workflowNotFoundCount++
                    log.info("Workflow not found for $remoteUrl commit: $commitSha (attempt $workflowNotFoundCount/$maxWorkflowNotFoundAttempts)")

                    if (workflowNotFoundCount >= maxWorkflowNotFoundAttempts) {
                        log.info("No workflow found after $maxWorkflowNotFoundAttempts attempts, stopping monitoring")
                        stopMonitoring()
                    }
                }
            } catch (e: Exception) {
                log.error("Error monitoring pipeline for commit: $commitSha", e)
                project?.let { currentProject ->
                    AutoDevNotifications.notify(
                        currentProject,
                        "Error monitoring GitHub Action: ${e.message}",
                        NotificationType.ERROR
                    )
                }
                stopMonitoring()
            }
        }, 4, 5, TimeUnit.MINUTES)  // 4分钟后开始第一次检查，然后每5分钟检查一次
    }

    private fun findWorkflowRunForCommit(remoteUrl: String, commitSha: String): GHWorkflowRun? {
        return try {
            GitHubIssue.getGitHubRepository(project!!, remoteUrl)?.let { ghRepository ->
                val allRuns = ghRepository.queryWorkflowRuns()
                    .list()
                    .iterator()
                    .asSequence()
                    .take(50)  // 检查最近50个runs
                    .toList()

                allRuns.find { it.headSha == commitSha }?.also { matchingRun ->
                    log.info("Found workflow run for commit $commitSha: ${matchingRun.name} (${matchingRun.status})")
                } ?: run {
                    log.debug("No workflow run found for commit $commitSha in recent ${allRuns.size} runs")
                    null
                }
            }
        } catch (e: Exception) {
            log.error("Error finding workflow run for commit: $commitSha", e)
            null
        }
    }

    private fun checkWorkflowStatus(workflowRun: GHWorkflowRun, commitSha: String): Boolean =
        when (workflowRun.status) {
            GHWorkflowRun.Status.COMPLETED -> {
                when (workflowRun.conclusion) {
                    GHWorkflowRun.Conclusion.SUCCESS -> true
                    GHWorkflowRun.Conclusion.FAILURE -> {
                        handleWorkflowFailure(workflowRun, commitSha)
                        true
                    }
                    GHWorkflowRun.Conclusion.CANCELLED,
                    GHWorkflowRun.Conclusion.TIMED_OUT -> true
                    else -> {
                        log.info("Workflow completed with conclusion: ${workflowRun.conclusion}")
                        false
                    }
                }
            }
            GHWorkflowRun.Status.IN_PROGRESS,
            GHWorkflowRun.Status.QUEUED -> false
            else -> false
        }

    private fun handleWorkflowFailure(workflowRun: GHWorkflowRun, commitSha: String) {
        try {
            val failureDetails = getWorkflowFailureDetails(workflowRun)
            val detailedMessage = buildDetailedFailureMessage(workflowRun, commitSha, failureDetails)
            project?.let { AutoDevNotifications.error(it, detailedMessage) }
        } catch (e: Exception) {
            val isPermissionError = e.message?.run {
                contains("admin rights", ignoreCase = true) || contains("403", ignoreCase = true)
            } ?: false

            val fallbackMessage = if (isPermissionError) {
                """
                |❌ GitHub Action failed for commit: ${commitSha.take(7)} - ${workflowRun.conclusion}
                |⚠️ Detailed logs unavailable - admin rights required
                |View details at: ${workflowRun.htmlUrl}
                """.trimMargin()
            } else {
                """
                |❌ GitHub Action failed for commit: ${commitSha.take(7)} - ${workflowRun.conclusion}
                |URL: ${workflowRun.htmlUrl}
                |Error getting details: ${e.message}
                """.trimMargin()
            }

            project?.let { AutoDevNotifications.error(it, fallbackMessage) }
        }
    }

    private fun getWorkflowFailureDetails(workflowRun: GHWorkflowRun): WorkflowFailureDetails {
        return try {
            val failedJobs = workflowRun
                .listJobs()
                .filter { it.conclusion == GHWorkflowRun.Conclusion.FAILURE }
                .map {
                    JobFailure(
                        jobName = it.name,
                        errorSteps = extractFailedSteps(it),
                        logs = getJobLogsFromAPI(it),
                        jobUrl = it.htmlUrl?.toString()
                    )
                }
            if (failedJobs.isNotEmpty()) {
                WorkflowFailureDetails(failedJobs = failedJobs.toMutableList())
            } else {
                WorkflowFailureDetails(workflowLogs = getWorkflowLogsFromAPI(workflowRun))
            }
        } catch (e: Exception) {
            log.error("Error getting workflow failure details", e)
            WorkflowFailureDetails(error = e.message)
        }
    }

    private fun extractFailedSteps(job: GHWorkflowJob): List<String> {
        return try {
            job.steps
                .filter { it.conclusion == GHWorkflowRun.Conclusion.FAILURE }
                .map { step -> "${step.name}: ${step.conclusion} (${step.number})" }
        } catch (e: Exception) {
            log.error("Error extracting failed steps", e)
            emptyList()
        }
    }

    private fun getJobLogsFromAPI(job: GHWorkflowJob): String? {
        return try {
            job.downloadLogs { logStream ->
                logStream.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                }
            }
        } catch (e: Exception) {
            if (isPermissionError(e.message)) {
                log.info("Admin rights required to download job logs. Falling back to alternative approach.")
                """
                |⚠️ Job logs unavailable - admin rights required to download logs from GitHub Actions.
                |Job URL: ${job.htmlUrl}
                |You can view the logs directly at: ${job.htmlUrl}
                """.trimMargin()
            } else {
                null
            }
        }
    }

    private fun getWorkflowLogsFromAPI(workflowRun: GHWorkflowRun): String? {
        return try {
            workflowRun.downloadLogs { logStream ->
                ZipInputStream(logStream).use { zipStream ->
                    extractLogsFromZipStream(zipStream)
                }
            }
        } catch (e: Exception) {
            log.warn("Cannot download workflow logs - ${e.message}")
            if (isPermissionError(e.message)) {
                log.info("Admin rights required to download workflow logs.")
                """
                |⚠️ Workflow logs unavailable - admin rights required to download logs from GitHub Actions.
                |Workflow URL: ${workflowRun.htmlUrl}
                |You can view the logs directly at: ${workflowRun.htmlUrl}
                """.trimMargin()
            } else {
                null
            }
        }
    }

    private fun extractLogsFromZipStream(zipStream: ZipInputStream): String {
        val logContents = StringBuilder()
        generateSequence { zipStream.nextEntry }
            .filter { it.name.endsWith(".txt") }
            .forEach { entry ->
                val content = zipStream.readBytes().toString(Charsets.UTF_8)
                logContents.append("=== ${entry.name} ===\n")
                    .append(content)
                    .append("\n\n")
            }

        return logContents.toString()
    }

    private fun isPermissionError(errorMessage: String?): Boolean {
        return errorMessage?.let {
            it.contains("admin rights", ignoreCase = true) || it.contains("403", ignoreCase = true)
        } ?: false
    }

    private fun buildDetailedFailureMessage(
        workflowRun: GHWorkflowRun,
        commitSha: String,
        failureDetails: WorkflowFailureDetails
    ): String = buildString {
        append("❌ GitHub Action failed for commit: ${commitSha.take(7)}\n")
        append("Workflow: ${workflowRun.name}\n")
        append("URL: ${workflowRun.htmlUrl}\n\n")

        when {
            failureDetails.error != null -> {
                append("Error getting details: ${failureDetails.error}\n")
            }
            failureDetails.failedJobs.isNotEmpty() -> {
                append("Failed Jobs:\n")
                failureDetails.failedJobs.forEach { jobFailure ->
                    append("• ${jobFailure.jobName}\n")

                    jobFailure.jobUrl?.let {
                        append("  URL: $it\n")
                    }

                    if (jobFailure.errorSteps.isNotEmpty()) {
                        append("  Failed steps:\n")
                        jobFailure.errorSteps.forEach { step ->
                            append("    - $step\n")
                        }
                    }

                    jobFailure.logs?.takeIf { it.isNotBlank() }?.let { logs ->
                        append("  Logs:\n")
                        logs.lineSequence()
                            .filter { it.isNotBlank() }
                            .forEach { line ->
                                append("    $line\n")
                            }
                    }

                    append("\n")
                }
            }
            failureDetails.workflowLogs != null -> {
                append("Workflow Logs:\n")
                failureDetails.workflowLogs.lineSequence()
                    .filter { it.isNotBlank() }
                    .forEach { line ->
                        append("  $line\n")
                    }
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel(false)
        monitoringJob = null
        log.info("Pipeline monitoring stopped")
    }

    private data class WorkflowFailureDetails(
        val failedJobs: MutableList<JobFailure> = mutableListOf(),
        val workflowLogs: String? = null,
        val error: String? = null
    )

    private data class JobFailure(
        val jobName: String,
        val errorSteps: List<String>,
        val logs: String?,
        val jobUrl: String? = null
    )
}