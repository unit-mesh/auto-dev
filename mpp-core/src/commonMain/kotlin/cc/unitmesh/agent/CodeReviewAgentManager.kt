package cc.unitmesh.agent

import cc.unitmesh.agent.logging.getLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Manager for Code Review Agents with support for parallel execution
 * Inspired by Google Antigravity's Manager View concept
 * 
 * This manager provides:
 * - Parallel review task execution
 * - Artifact collection and management
 * - Progress tracking
 * - Async fire-and-forget task submission
 */
class CodeReviewAgentManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val logger = getLogger("CodeReviewAgentManager")
    
    private val _activeReviews = MutableStateFlow<Map<String, ReviewSessionState>>(emptyMap())
    val activeReviews: StateFlow<Map<String, ReviewSessionState>> = _activeReviews.asStateFlow()
    
    private val _completedReviews = MutableStateFlow<Map<String, ReviewSessionState>>(emptyMap())
    val completedReviews: StateFlow<Map<String, ReviewSessionState>> = _completedReviews.asStateFlow()
    
    /**
     * Submit a review task for async execution (fire-and-forget)
     * Returns immediately with a session ID for tracking
     */
    fun submitReview(
        agent: CodeReviewAgent,
        task: ReviewTask,
        onProgress: (String) -> Unit = {}
    ): String {
        val sessionId = generateSessionId()
        val session = ReviewSessionState(
            sessionId = sessionId,
            task = task,
            status = ReviewStatus.QUEUED,
            startTime = Clock.System.now()
        )
        
        _activeReviews.value = _activeReviews.value + (sessionId to session)
        
        scope.launch {
            executeReview(sessionId, agent, task, onProgress)
        }
        
        logger.info { "Submitted review session: $sessionId" }
        return sessionId
    }
    
    /**
     * Submit multiple review tasks in parallel
     * Returns a list of session IDs
     */
    fun submitParallelReviews(
        agents: List<CodeReviewAgent>,
        tasks: List<ReviewTask>,
        onProgress: (String, String) -> Unit = { _, _ -> }
    ): List<String> {
        require(agents.size == tasks.size) {
            "Number of agents (${agents.size}) must match number of tasks (${tasks.size})"
        }
        
        return agents.zip(tasks).map { (agent, task) ->
            submitReview(agent, task) { progress ->
                onProgress(task.reviewType.name, progress)
            }
        }
    }
    
    /**
     * Get the current state of a review session
     */
    fun getSessionState(sessionId: String): ReviewSessionState? {
        return _activeReviews.value[sessionId] ?: _completedReviews.value[sessionId]
    }
    
    /**
     * Get all artifacts for a completed review session
     */
    fun getSessionArtifacts(sessionId: String): ReviewArtifactCollection? {
        return getSessionState(sessionId)?.artifacts
    }
    
    /**
     * Cancel an active review session
     */
    fun cancelReview(sessionId: String) {
        _activeReviews.value[sessionId]?.let { session ->
            val canceledSession = session.copy(
                status = ReviewStatus.CANCELLED,
                endTime = Clock.System.now()
            )
            _activeReviews.value = _activeReviews.value - sessionId
            _completedReviews.value = _completedReviews.value + (sessionId to canceledSession)
            logger.info { "Cancelled review session: $sessionId" }
        }
    }
    
    /**
     * Get summary of all active reviews
     */
    fun getActiveSummary(): List<ReviewSummary> {
        return _activeReviews.value.values.map { session ->
            ReviewSummary(
                sessionId = session.sessionId,
                reviewType = session.task.reviewType,
                status = session.status,
                filesCount = session.task.filePaths.size,
                currentStep = session.currentStep,
                totalSteps = session.artifacts?.plan?.approachSteps?.size ?: 0,
                progress = calculateProgress(session)
            )
        }
    }
    
    private suspend fun executeReview(
        sessionId: String,
        agent: CodeReviewAgent,
        task: ReviewTask,
        onProgress: (String) -> Unit
    ) {
        try {
            // Update status to running
            updateSessionStatus(sessionId, ReviewStatus.RUNNING)
            
            // Generate review plan first (Antigravity approach)
            val plan = generateReviewPlan(task)
            updateSessionWithArtifact(sessionId) { artifacts ->
                artifacts.copy(plan = plan)
            }
            onProgress("📋 Review plan generated: ${plan.approachSteps.size} steps")
            
            // Execute the actual review
            val result = agent.execute(task) { progress ->
                onProgress(progress)
            }
            
            // Generate artifacts from result
            val summary = generateSummaryArtifact(task, result)
            val fixSuggestions = generateFixSuggestions(result)
            
            updateSessionWithArtifact(sessionId) { artifacts ->
                artifacts.copy(
                    summary = summary,
                    fixSuggestions = fixSuggestions,
                    endTime = Clock.System.now()
                )
            }
            
            // Mark as completed
            updateSessionStatus(sessionId, ReviewStatus.COMPLETED)
            moveToCompleted(sessionId)
            
            onProgress("✅ Review completed successfully")
            
        } catch (e: CancellationException) {
            logger.info { "Review cancelled: $sessionId" }
            updateSessionStatus(sessionId, ReviewStatus.CANCELLED)
            moveToCompleted(sessionId)
        } catch (e: Exception) {
            logger.error(e) { "Review failed: $sessionId - ${e.message}" }
            updateSessionStatus(sessionId, ReviewStatus.FAILED, e.message)
            moveToCompleted(sessionId)
            onProgress("❌ Review failed: ${e.message}")
        }
    }
    
    private fun generateReviewPlan(task: ReviewTask): ReviewPlanArtifact {
        val complexity = when {
            task.filePaths.size > 50 -> ReviewPlanArtifact.ComplexityLevel.CRITICAL
            task.filePaths.size > 20 -> ReviewPlanArtifact.ComplexityLevel.HIGH
            task.filePaths.size > 5 -> ReviewPlanArtifact.ComplexityLevel.MEDIUM
            else -> ReviewPlanArtifact.ComplexityLevel.LOW
        }
        
        val steps = mutableListOf<ReviewPlanArtifact.ReviewStep>()
        
        // Step 1: Read files and gather context
        if (task.filePaths.isNotEmpty()) {
            steps.add(
                ReviewPlanArtifact.ReviewStep(
                    order = 1,
                    description = "Analyze ${task.filePaths.size} files for ${task.reviewType} review",
                    expectedOutput = "File contents and structure analysis",
                    tools = listOf("read-file", "list-files")
                )
            )
        }
        
        // Step 2: Run linters
        steps.add(
            ReviewPlanArtifact.ReviewStep(
                order = 2,
                description = "Execute static analysis and linters",
                expectedOutput = "Linter findings and code quality metrics",
                tools = listOf("linter-tools")
            )
        )
        
        // Step 3: Analyze findings
        steps.add(
            ReviewPlanArtifact.ReviewStep(
                order = 3,
                description = "Review code for ${task.reviewType.name.lowercase()} issues",
                expectedOutput = "Prioritized list of findings",
                tools = emptyList()
            )
        )
        
        // Step 4: Generate fixes
        steps.add(
            ReviewPlanArtifact.ReviewStep(
                order = 4,
                description = "Generate actionable fix suggestions",
                expectedOutput = "Unified diff patches for critical issues",
                tools = emptyList()
            )
        )
        
        val focusAreas = when (task.reviewType) {
            ReviewType.SECURITY -> listOf(
                "SQL injection vulnerabilities",
                "Authentication/authorization issues",
                "Data exposure risks",
                "Cryptography usage",
                "Input validation"
            )
            ReviewType.PERFORMANCE -> listOf(
                "Algorithm complexity",
                "Memory usage",
                "Database query optimization",
                "Caching opportunities",
                "Resource leaks"
            )
            ReviewType.STYLE -> listOf(
                "Code formatting",
                "Naming conventions",
                "Documentation completeness",
                "Code organization",
                "Best practices adherence"
            )
            ReviewType.COMPREHENSIVE -> listOf(
                "Code correctness",
                "Security vulnerabilities",
                "Performance issues",
                "Maintainability",
                "Test coverage"
            )
        }
        
        return ReviewPlanArtifact(
            title = "Review Plan: ${task.reviewType} Analysis",
            scope = ReviewPlanArtifact.ReviewScope(
                filesCount = task.filePaths.size,
                linesOfCode = 0, // Will be updated after reading files
                reviewType = task.reviewType.name,
                complexity = complexity
            ),
            estimatedDuration = when (complexity) {
                ReviewPlanArtifact.ComplexityLevel.LOW -> "2-5 minutes"
                ReviewPlanArtifact.ComplexityLevel.MEDIUM -> "5-10 minutes"
                ReviewPlanArtifact.ComplexityLevel.HIGH -> "10-20 minutes"
                ReviewPlanArtifact.ComplexityLevel.CRITICAL -> "20+ minutes"
            },
            approachSteps = steps,
            focusAreas = focusAreas,
            toolsToUse = listOf("read-file", "list-files", "linter-tools")
        )
    }
    
    private fun generateSummaryArtifact(
        task: ReviewTask,
        result: ToolResult.AgentResult
    ): AnalysisSummaryArtifact {
        val findings = ReviewFinding.parseFindings(result.content)
        
        val criticalCount = findings.count { it.severity == Severity.CRITICAL }
        val highCount = findings.count { it.severity == Severity.HIGH }
        val mediumCount = findings.count { it.severity == Severity.MEDIUM }
        val lowCount = findings.count { it.severity == Severity.LOW }
        
        // Calculate quality scores based on findings
        val securityScore = 100 - (criticalCount * 20 + highCount * 10 + mediumCount * 5)
        val overallScore = maxOf(0, 100 - (criticalCount * 15 + highCount * 8 + mediumCount * 3 + lowCount * 1))
        
        return AnalysisSummaryArtifact(
            title = "Code Review Summary: ${task.reviewType}",
            overallQuality = AnalysisSummaryArtifact.QualityScore(
                overall = overallScore,
                maintainability = maxOf(0, 100 - (findings.size * 5)),
                security = maxOf(0, securityScore),
                performance = 85, // Placeholder
                testability = 80  // Placeholder
            ),
            findings = findings,
            metrics = AnalysisSummaryArtifact.CodeMetrics(
                filesAnalyzed = task.filePaths.size,
                linesAnalyzed = 0, // Placeholder
                issuesFound = findings.size,
                criticalIssues = criticalCount,
                highIssues = highCount,
                mediumIssues = mediumCount,
                lowIssues = lowCount
            ),
            recommendations = extractRecommendations(result.content)
        )
    }
    
    private fun generateFixSuggestions(result: ToolResult.AgentResult): List<FixSuggestionArtifact> {
        // Parse fix suggestions from result content
        // This is a simplified implementation
        return emptyList()
    }
    
    private fun extractRecommendations(content: String): List<String> {
        val recommendations = mutableListOf<String>()
        val lines = content.lines()
        var inRecommendationSection = false
        
        for (line in lines) {
            when {
                line.contains("recommendation", ignoreCase = true) ||
                line.contains("suggestion", ignoreCase = true) -> {
                    inRecommendationSection = true
                }
                inRecommendationSection && (line.startsWith("-") || line.startsWith("*")) -> {
                    recommendations.add(line.trimStart('-', '*', ' '))
                }
                line.isBlank() && inRecommendationSection -> {
                    inRecommendationSection = false
                }
            }
        }
        
        return recommendations.take(5)
    }
    
    private fun updateSessionStatus(
        sessionId: String,
        status: ReviewStatus,
        error: String? = null
    ) {
        _activeReviews.value[sessionId]?.let { session ->
            val updated = session.copy(
                status = status,
                error = error,
                endTime = if (status.isTerminal()) Clock.System.now() else null
            )
            _activeReviews.value = _activeReviews.value + (sessionId to updated)
        }
    }
    
    private fun updateSessionWithArtifact(
        sessionId: String,
        update: (ReviewArtifactCollection) -> ReviewArtifactCollection
    ) {
        _activeReviews.value[sessionId]?.let { session ->
            val currentArtifacts = session.artifacts ?: ReviewArtifactCollection(
                sessionId = sessionId,
                startTime = session.startTime
            )
            val updatedArtifacts = update(currentArtifacts)
            val updatedSession = session.copy(artifacts = updatedArtifacts)
            _activeReviews.value = _activeReviews.value + (sessionId to updatedSession)
        }
    }
    
    private fun moveToCompleted(sessionId: String) {
        _activeReviews.value[sessionId]?.let { session ->
            _activeReviews.value = _activeReviews.value - sessionId
            _completedReviews.value = _completedReviews.value + (sessionId to session)
        }
    }
    
    private fun calculateProgress(session: ReviewSessionState): Int {
        val plan = session.artifacts?.plan ?: return 0
        val totalSteps = plan.approachSteps.size
        if (totalSteps == 0) return 0
        
        return when (session.status) {
            ReviewStatus.QUEUED -> 0
            ReviewStatus.RUNNING -> (session.currentStep * 100) / totalSteps
            ReviewStatus.COMPLETED -> 100
            ReviewStatus.FAILED, ReviewStatus.CANCELLED -> 0
        }
    }
    
    private fun generateSessionId(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "review-session-$timestamp"
    }
}

/**
 * State of a review session
 */
@Serializable
data class ReviewSessionState(
    val sessionId: String,
    val task: ReviewTask,
    val status: ReviewStatus,
    val startTime: kotlinx.datetime.Instant,
    val endTime: kotlinx.datetime.Instant? = null,
    val currentStep: Int = 0,
    val artifacts: ReviewArtifactCollection? = null,
    val error: String? = null
)

/**
 * Status of a review session
 */
enum class ReviewStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;
    
    fun isTerminal(): Boolean = this in setOf(COMPLETED, FAILED, CANCELLED)
}

/**
 * Summary of a review session for dashboard display
 */
@Serializable
data class ReviewSummary(
    val sessionId: String,
    val reviewType: ReviewType,
    val status: ReviewStatus,
    val filesCount: Int,
    val currentStep: Int,
    val totalSteps: Int,
    val progress: Int
)
