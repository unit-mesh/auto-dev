package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.scoring.HeuristicScoringModel
import cc.unitmesh.agent.scoring.TextSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeuristicScoringModelTest {

    private val model = HeuristicScoringModel()

    @Test
    fun `test scoring prioritizes class over toc`() {
        val classSegment = TextSegment(
            text = "AuthService",
            metadata = mapOf("type" to "class", "name" to "AuthService")
        )
        val tocSegment = TextSegment(
            text = "Authentication Guide",
            metadata = mapOf("type" to "toc", "name" to "Authentication Guide")
        )

        val query = "AuthService"
        
        val classScore = model.score(classSegment, query)
        val tocScore = model.score(tocSegment, query)

        println("Class Score: $classScore")
        println("TOC Score: $tocScore")

        assertTrue(classScore > tocScore, "Class should be scored higher than TOC")
    }

    @Test
    fun `test exact match bonus`() {
        val exactMatch = TextSegment(
            text = "AuthService",
            metadata = mapOf("type" to "class", "name" to "AuthService")
        )
        val partialMatch = TextSegment(
            text = "AuthServiceImpl",
            metadata = mapOf("type" to "class", "name" to "AuthServiceImpl")
        )

        val query = "AuthService"

        val exactScore = model.score(exactMatch, query)
        val partialScore = model.score(partialMatch, query)

        println("Exact Score: $exactScore")
        println("Partial Score: $partialScore")

        assertTrue(exactScore > partialScore, "Exact match should be scored higher than partial match")
    }
}
