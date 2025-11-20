package cc.unitmesh.agent

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class CodeReviewAgentManagerTest {
    
    private lateinit var manager: CodeReviewAgentManager
    
    @BeforeTest
    fun setup() {
        manager = CodeReviewAgentManager()
    }
    
    @Test
    fun testSubmitReview() = runBlocking {
        // This is a basic structure test - actual review execution would require mock LLM
        val sessionId = "test-session-${System.currentTimeMillis()}"
        
        // Verify manager is initialized
        assertNotNull(manager)
        
        // Test that we can get session state (even if null before submission)
        val initialState = manager.getSessionState(sessionId)
        assertNull(initialState, "Session should not exist before submission")
    }
    
    @Test
    fun testGetActiveSummary() = runBlocking {
        val summary = manager.getActiveSummary()
        
        assertNotNull(summary)
        assertEquals(0, summary.totalSessions, "Should start with no sessions")
        assertEquals(0, summary.queued)
        assertEquals(0, summary.running)
        assertEquals(0, summary.completed)
        assertEquals(0, summary.failed)
        assertEquals(0, summary.cancelled)
    }
    
    @Test
    fun testCancelReviewNonExistent() {
        val result = manager.cancelReview("non-existent-session")
        assertFalse(result, "Should not be able to cancel non-existent session")
    }
    
    @Test
    fun testGetSessionArtifactsNonExistent() {
        val artifacts = manager.getSessionArtifacts("non-existent-session")
        assertNull(artifacts, "Should return null for non-existent session")
    }
    
    @Test
    fun testReviewStatusEnum() {
        // Test all status values exist
        val statuses = ReviewStatus.values()
        assertEquals(5, statuses.size)
        assertTrue(statuses.contains(ReviewStatus.QUEUED))
        assertTrue(statuses.contains(ReviewStatus.RUNNING))
        assertTrue(statuses.contains(ReviewStatus.COMPLETED))
        assertTrue(statuses.contains(ReviewStatus.FAILED))
        assertTrue(statuses.contains(ReviewStatus.CANCELLED))
    }
    
    @Test
    fun testReviewSessionStateCreation() {
        val sessionState = ReviewSessionState(
            sessionId = "test-123",
            status = ReviewStatus.QUEUED,
            startedAt = kotlinx.datetime.Clock.System.now()
        )
        
        assertEquals("test-123", sessionState.sessionId)
        assertEquals(ReviewStatus.QUEUED, sessionState.status)
        assertTrue(sessionState.artifacts.isEmpty())
        assertNull(sessionState.errorMessage)
    }
    
    @Test
    fun testReviewSummaryCreation() {
        val summary = ReviewSummary(
            totalSessions = 10,
            queued = 2,
            running = 3,
            completed = 4,
            failed = 1,
            cancelled = 0
        )
        
        assertEquals(10, summary.totalSessions)
        assertEquals(2, summary.queued)
        assertEquals(3, summary.running)
        assertEquals(4, summary.completed)
        assertEquals(1, summary.failed)
        assertEquals(0, summary.cancelled)
    }
    
    @Test
    fun testConfidenceLevelPercentages() {
        assertEquals(95, ConfidenceLevel.VERY_HIGH.percentage)
        assertEquals(80, ConfidenceLevel.HIGH.percentage)
        assertEquals(60, ConfidenceLevel.MEDIUM.percentage)
        assertEquals(40, ConfidenceLevel.LOW.percentage)
        assertEquals(20, ConfidenceLevel.VERY_LOW.percentage)
    }
    
    @Test
    fun testComparisonModeValues() {
        val modes = ComparisonMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(ComparisonMode.BEFORE_AFTER))
        assertTrue(modes.contains(ComparisonMode.DIAGRAM))
        assertTrue(modes.contains(ComparisonMode.HYBRID))
    }
}
