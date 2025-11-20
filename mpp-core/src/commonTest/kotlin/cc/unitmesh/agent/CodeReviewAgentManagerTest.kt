package cc.unitmesh.agent

import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CodeReviewAgentManagerTest {

    @Test
    fun `submitReview should create session and return session ID`() = runTest {
        // Given
        val manager = CodeReviewAgentManager()
        val mockAgent = createMockAgent()
        val task = ReviewTask(
            projectPath = "/test/project",
            reviewType = ReviewType.SECURITY,
            filePaths = listOf("src/Main.kt")
        )

        // When
        val sessionId = manager.submitReview(mockAgent, task)

        // Then
        assertNotNull(sessionId)
        assertTrue(sessionId.startsWith("review-session-"))
        
        // Session should be active
        val session = manager.getSessionState(sessionId)
        assertNotNull(session)
        assertEquals(ReviewStatus.QUEUED, session.status)
    }

    @Test
    fun `submitParallelReviews should create multiple sessions`() = runTest {
        // Given
        val manager = CodeReviewAgentManager()
        val mockAgent1 = createMockAgent()
        val mockAgent2 = createMockAgent()
        
        val task1 = ReviewTask(
            projectPath = "/test/project",
            reviewType = ReviewType.SECURITY,
            filePaths = listOf("src/Security.kt")
        )
        val task2 = ReviewTask(
            projectPath = "/test/project",
            reviewType = ReviewType.PERFORMANCE,
            filePaths = listOf("src/Performance.kt")
        )

        // When
        val sessionIds = manager.submitParallelReviews(
            agents = listOf(mockAgent1, mockAgent2),
            tasks = listOf(task1, task2)
        )

        // Then
        assertEquals(2, sessionIds.size)
        
        // Both sessions should exist
        val session1 = manager.getSessionState(sessionIds[0])
        val session2 = manager.getSessionState(sessionIds[1])
        assertNotNull(session1)
        assertNotNull(session2)
    }

    @Test
    fun `getActiveSummary should return summary of active reviews`() = runTest {
        // Given
        val manager = CodeReviewAgentManager()
        val mockAgent = createMockAgent()
        val task = ReviewTask(
            projectPath = "/test/project",
            reviewType = ReviewType.COMPREHENSIVE,
            filePaths = listOf("src/File1.kt", "src/File2.kt")
        )

        // When
        val sessionId = manager.submitReview(mockAgent, task)
        delay(100) // Allow some processing
        val summary = manager.getActiveSummary()

        // Then
        assertTrue(summary.isNotEmpty())
        val reviewSummary = summary.find { it.sessionId == sessionId }
        assertNotNull(reviewSummary)
        assertEquals(ReviewType.COMPREHENSIVE, reviewSummary.reviewType)
        assertEquals(2, reviewSummary.filesCount)
    }

    @Test
    fun `cancelReview should move session to completed with cancelled status`() = runTest {
        // Given
        val manager = CodeReviewAgentManager()
        val mockAgent = createMockAgent()
        val task = ReviewTask(
            projectPath = "/test/project",
            reviewType = ReviewType.STYLE,
            filePaths = listOf("src/Style.kt")
        )

        // When
        val sessionId = manager.submitReview(mockAgent, task)
        delay(50) // Let it start
        manager.cancelReview(sessionId)
        delay(50) // Let cancellation process

        // Then
        val session = manager.getSessionState(sessionId)
        assertNotNull(session)
        assertEquals(ReviewStatus.CANCELLED, session.status)
        assertNotNull(session.endTime)
        
        // Should not be in active reviews anymore
        assertTrue(manager.activeReviews.value[sessionId] == null)
    }

    @Test
    fun `getSessionArtifacts should return artifacts after review completion`() = runTest {
        // Given
        val manager = CodeReviewAgentManager()
        val mockAgent = createMockAgent()
        val task = ReviewTask(
            projectPath = "/test/project",
            reviewType = ReviewType.SECURITY,
            filePaths = listOf("src/Auth.kt")
        )

        // When
        val sessionId = manager.submitReview(mockAgent, task)
        
        // Wait for review to complete (mock agent completes quickly)
        var attempts = 0
        while (attempts < 50) { // Max 5 seconds
            val session = manager.getSessionState(sessionId)
            if (session?.status?.isTerminal() == true) break
            delay(100)
            attempts++
        }

        val artifacts = manager.getSessionArtifacts(sessionId)

        // Then
        assertNotNull(artifacts)
        assertNotNull(artifacts.plan) // Plan should be generated
    }

    @Test
    fun `ReviewStatus terminal statuses should be identified correctly`() {
        // Terminal statuses
        assertTrue(ReviewStatus.COMPLETED.isTerminal())
        assertTrue(ReviewStatus.FAILED.isTerminal())
        assertTrue(ReviewStatus.CANCELLED.isTerminal())
        
        // Non-terminal statuses
        assertTrue(!ReviewStatus.QUEUED.isTerminal())
        assertTrue(!ReviewStatus.RUNNING.isTerminal())
    }

    // Helper to create a mock agent for testing
    private fun createMockAgent(): CodeReviewAgent {
        val mockLLMService = object : KoogLLMService {
            override suspend fun streamPrompt(
                prompt: String,
                compileDevIns: Boolean
            ) = kotlinx.coroutines.flow.flow {
                emit("Mock review response")
            }
        }
        
        val mockConfigService = object : McpToolConfigService {
            override fun getAllServerConfigs() = emptyList<Any>()
            override fun getServerConfig(serverId: String) = null
            override fun isServerEnabled(serverId: String) = false
        }

        return CodeReviewAgent(
            projectPath = "/test/project",
            llmService = mockLLMService,
            maxIterations = 1,
            mcpToolConfigService = mockConfigService,
            enableLLMStreaming = false
        )
    }
}
