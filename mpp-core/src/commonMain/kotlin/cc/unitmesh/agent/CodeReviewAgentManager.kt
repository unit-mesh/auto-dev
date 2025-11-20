package cc.unitmesh.agent

import cc.unitmesh.agent.logging.getLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Manager for orchestrating code review agent sessions with artifact generation
 */
class CodeReviewAgentManager {
    private val logger = getLogger("CodeReviewAgentManager")
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Active review sessions
    private val _sessions = MutableStateFlow<Map<String, ReviewSessionState>>(emptyMap())
    val sessions: StateFlow<Map<String, ReviewSessionState>> = _sessions.asStateFlow()
    
    // Completed reviews archive
    private val completedReviews = mutableMapOf<String, ReviewArtifactCollection>()
    
    /**
     * Submit a single review task
     */
    suspend fun submitReview(
        agent: CodeReviewAgent,
        task: ReviewTask,
        onProgress: (String) -> Unit = {}
    ): String {
        val sessionId = generateSessionId()
        
        logger.info { "Submitting review session: $sessionId" }
        
        // Initialize session state
        updateSessionState(sessionId, ReviewStatus.QUEUED)
        
        // Launch review in background
        scope.launch {
            try {
                updateSessionState(sessionId, ReviewStatus.RUNNING)
                
                // Generate review plan
                val reviewPlan = generateReviewPlan(task)
                addArtifact(sessionId, reviewPlan)
                onProgress("Review plan generated")
                
                // Execute review
                val result = agent.execute(task, onProgress)
                
                // Generate analysis summary
                val analysisSummary = generateAnalysisSummary(result, task)
                addArtifact(sessionId, analysisSummary)
                onProgress("Analysis complete")
                
                // Generate fix suggestions (if findings exist)
                if (analysisSummary.findings.isNotEmpty()) {
                    val fixSuggestions = generateFixSuggestions(analysisSummary.findings)
                    fixSuggestions.forEach { addArtifact(sessionId, it) }
                    onProgress("Fix suggestions generated")
                }
                
                // Generate metrics report
                val metricsReport = generateMetricsReport(task)
                addArtifact(sessionId, metricsReport)
                onProgress("Metrics report generated")
                
                updateSessionState(sessionId, ReviewStatus.COMPLETED)
                moveToCompleted(sessionId)
                
            } catch (e: CancellationException) {
                logger.warn { "Review session $sessionId cancelled" }
                updateSessionState(sessionId, ReviewStatus.CANCELLED)
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Review session $sessionId failed: ${e.message}" }
                updateSessionState(sessionId, ReviewStatus.FAILED, e.message)
            }
        }
        
        return sessionId
    }
    
    /**
     * Submit multiple reviews in parallel
     */
    suspend fun submitParallelReviews(
        reviews: List<Pair<CodeReviewAgent, ReviewTask>>,
        onProgress: (String, String) -> Unit = { _, _ -> }
    ): List<String> {
        logger.info { "Submitting ${reviews.size} parallel reviews" }
        
        return reviews.map { (agent, task) ->
            submitReview(agent, task) { progress ->
                onProgress(getSessionId(agent, task), progress)
            }
        }
    }
    
    /**
     * Get current state of a review session
     */
    fun getSessionState(sessionId: String): ReviewSessionState? {
        return _sessions.value[sessionId]
    }
    
    /**
     * Get all artifacts for a session
     */
    fun getSessionArtifacts(sessionId: String): ReviewArtifactCollection? {
        // Check active sessions first
        val session = _sessions.value[sessionId]
        if (session != null && session.artifacts.isNotEmpty()) {
            return ReviewArtifactCollection(
                sessionId = sessionId,
                artifacts = session.artifacts,
                createdAt = session.startedAt
            )
        }
        
        // Check completed reviews
        return completedReviews[sessionId]
    }
    
    /**
     * Cancel a running review
     */
    fun cancelReview(sessionId: String): Boolean {
        val session = _sessions.value[sessionId]
        if (session?.status == ReviewStatus.RUNNING || session?.status == ReviewStatus.QUEUED) {
            updateSessionState(sessionId, ReviewStatus.CANCELLED)
            logger.info { "Review session $sessionId cancelled" }
            return true
        }
        return false
    }
    
    /**
     * Get active summary of all running reviews
     */
    fun getActiveSummary(): ReviewSummary {
        val sessions = _sessions.value.values
        return ReviewSummary(
            totalSessions = sessions.size,
            queued = sessions.count { it.status == ReviewStatus.QUEUED },
            running = sessions.count { it.status == ReviewStatus.RUNNING },
            completed = sessions.count { it.status == ReviewStatus.COMPLETED },
            failed = sessions.count { it.status == ReviewStatus.FAILED },
            cancelled = sessions.count { it.status == ReviewStatus.CANCELLED }
        )
    }
    
    // Private helper methods
    
    private fun updateSessionState(
        sessionId: String,
        status: ReviewStatus,
        errorMessage: String? = null
    ) {
        val current = _sessions.value[sessionId]
        val updated = if (current == null) {
            ReviewSessionState(
                sessionId = sessionId,
                status = status,
                startedAt = Clock.System.now(),
                errorMessage = errorMessage
            )
        } else {
            current.copy(
                status = status,
                updatedAt = Clock.System.now(),
                errorMessage = errorMessage
            )
        }
        
        _sessions.value = _sessions.value + (sessionId to updated)
    }
    
    private fun addArtifact(sessionId: String, artifact: CodeReviewArtifact) {
        val session = _sessions.value[sessionId] ?: return
        val updated = session.copy(
            artifacts = session.artifacts + artifact,
            updatedAt = Clock.System.now()
        )
        _sessions.value = _sessions.value + (sessionId to updated)
    }
    
    private fun moveToCompleted(sessionId: String) {
        val session = _sessions.value[sessionId] ?: return
        completedReviews[sessionId] = ReviewArtifactCollection(
            sessionId = sessionId,
            artifacts = session.artifacts,
            createdAt = session.startedAt
        )
        // Keep in active sessions for a while for status queries
        scope.launch {
            delay(60000) // Keep for 1 minute
            _sessions.value = _sessions.value - sessionId
        }
    }
    
    private fun getSessionId(agent: CodeReviewAgent, task: ReviewTask): String {
        // Generate a consistent ID for tracking
        return "${agent.name}-${task.projectPath.hashCode()}"
    }
    
    // Artifact generation methods
    
    private suspend fun generateReviewPlan(task: ReviewTask): ReviewPlanArtifact {
        val scope = ReviewScope(
            fileCount = task.filePaths.size,
            linesOfCode = 0, // Could be calculated if needed
            languages = detectLanguages(task.filePaths),
            description = "Review ${task.filePaths.size} files for ${task.reviewType}"
        )
        
        val focusAreas = when (task.reviewType) {
            ReviewType.COMPREHENSIVE -> listOf(
                "Code quality and maintainability",
                "Security vulnerabilities",
                "Performance issues",
                "Best practices compliance"
            )
            ReviewType.SECURITY -> listOf(
                "Security vulnerabilities",
                "Authentication and authorization",
                "Input validation",
                "Secure data handling"
            )
            ReviewType.PERFORMANCE -> listOf(
                "Algorithm efficiency",
                "Resource utilization",
                "Caching strategies",
                "Database query optimization"
            )
            ReviewType.STYLE -> listOf(
                "Code style consistency",
                "Naming conventions",
                "Documentation completeness",
                "Code organization"
            )
        }
        
        return ReviewPlanArtifact(
            scope = scope,
            focusAreas = focusAreas,
            estimatedDuration = estimateDuration(task.filePaths.size),
            reviewObjectives = listOf(
                "Identify critical issues",
                "Provide actionable recommendations",
                "Ensure code quality standards"
            )
        )
    }
    
    private fun generateAnalysisSummary(
        result: ToolResult.AgentResult,
        task: ReviewTask
    ): AnalysisSummaryArtifact {
        // Parse findings from result
        val findingsJson = result.metadata["findings"] as? String ?: "[]"
        val findings = try {
            kotlinx.serialization.json.Json.decodeFromString<List<ReviewFinding>>(findingsJson)
        } catch (e: Exception) {
            // Fallback: parse from content
            ReviewFinding.parseFindings(result.content)
        }
        
        val criticalCount = findings.count { it.severity == Severity.CRITICAL }
        val highCount = findings.count { it.severity == Severity.HIGH }
        val mediumCount = findings.count { it.severity == Severity.MEDIUM }
        val lowCount = findings.count { it.severity == Severity.LOW || it.severity == Severity.INFO }
        
        val score = calculateQualityScore(criticalCount, highCount, mediumCount, lowCount)
        
        return AnalysisSummaryArtifact(
            overallScore = score,
            findings = findings.take(10), // Top 10 findings
            criticalIssuesCount = criticalCount,
            highIssuesCount = highCount,
            mediumIssuesCount = mediumCount,
            lowIssuesCount = lowCount,
            summary = "Review completed for ${task.filePaths.size} files. Found ${findings.size} issues."
        )
    }
    
    private fun generateFixSuggestions(findings: List<ReviewFinding>): List<FixSuggestionArtifact> {
        return findings
            .filter { it.severity in listOf(Severity.CRITICAL, Severity.HIGH) }
            .take(5) // Top 5 critical/high issues
            .mapIndexed { index, finding ->
                FixSuggestionArtifact(
                    findingId = "FINDING-${index + 1}",
                    severity = finding.severity,
                    filePath = finding.filePath ?: "unknown",
                    lineNumber = finding.lineNumber,
                    problem = finding.description,
                    fixDescription = finding.suggestion ?: "Review and address this issue",
                    diffPatch = null, // Would be generated by actual fix generation
                    confidence = when (finding.severity) {
                        Severity.CRITICAL -> ConfidenceLevel.HIGH
                        Severity.HIGH -> ConfidenceLevel.MEDIUM
                        else -> ConfidenceLevel.LOW
                    }
                )
            }
    }
    
    private fun generateMetricsReport(task: ReviewTask): MetricsReportArtifact {
        val metrics = CodeMetrics(
            linesOfCode = 0, // Would be calculated from actual files
            cyclomaticComplexity = 0.0,
            testCoverage = 0.0,
            maintainabilityIndex = 0.0,
            technicalDebtHours = 0.0
        )
        
        return MetricsReportArtifact(
            metrics = metrics,
            trends = emptyMap(),
            benchmarks = emptyMap()
        )
    }
    
    private fun detectLanguages(filePaths: List<String>): List<String> {
        return filePaths
            .mapNotNull { path ->
                when {
                    path.endsWith(".kt") -> "Kotlin"
                    path.endsWith(".java") -> "Java"
                    path.endsWith(".js") || path.endsWith(".ts") -> "JavaScript/TypeScript"
                    path.endsWith(".py") -> "Python"
                    else -> null
                }
            }
            .distinct()
    }
    
    private fun estimateDuration(fileCount: Int): String {
        return when {
            fileCount <= 5 -> "5-10 minutes"
            fileCount <= 20 -> "10-30 minutes"
            fileCount <= 50 -> "30-60 minutes"
            else -> "1-2 hours"
        }
    }
    
    private fun calculateQualityScore(
        critical: Int,
        high: Int,
        medium: Int,
        low: Int
    ): QualityScore {
        // Simple scoring algorithm
        val totalIssues = critical + high + medium + low
        val weightedScore = when {
            critical > 0 -> 2.0
            high > 3 -> 4.0
            high > 0 -> 5.0
            medium > 5 -> 6.0
            medium > 0 -> 7.0
            low > 10 -> 8.0
            low > 0 -> 8.5
            else -> 9.5
        }
        
        val rating = when {
            weightedScore >= 9.0 -> "Excellent"
            weightedScore >= 7.0 -> "Good"
            weightedScore >= 5.0 -> "Fair"
            else -> "Poor"
        }
        
        return QualityScore(weightedScore, rating)
    }
    
    private fun generateSessionId(): String {
        // Generate a simple session ID using timestamp and random number
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val random = Random.nextInt(10000, 99999)
        return "review-$timestamp-$random"
    }
}

/**
 * State of a review session
 */
@Serializable
data class ReviewSessionState(
    val sessionId: String,
    val status: ReviewStatus,
    val startedAt: Instant,
    val updatedAt: Instant = startedAt,
    val artifacts: List<CodeReviewArtifact> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Review status enum
 */
@Serializable
enum class ReviewStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Summary of review sessions
 */
@Serializable
data class ReviewSummary(
    val totalSessions: Int,
    val queued: Int,
    val running: Int,
    val completed: Int,
    val failed: Int,
    val cancelled: Int
)
