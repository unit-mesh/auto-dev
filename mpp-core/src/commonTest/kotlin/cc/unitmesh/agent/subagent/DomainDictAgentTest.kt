package cc.unitmesh.agent.subagent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for DomainDictAgent data classes and schema
 * 
 * Note: Full agent execution tests require integration test setup with LLM mocking
 */
class DomainDictAgentTest {

    @Test
    fun testDomainDictContextCreation() {
        val context = DomainDictContext(
            userQuery = "Add authentication related terms",
            maxIterations = 3,
            focusArea = "auth"
        )

        assertEquals("Add authentication related terms", context.userQuery)
        assertEquals(3, context.maxIterations)
        assertEquals("auth", context.focusArea)
    }

    @Test
    fun testDomainDictContextDefaults() {
        val context = DomainDictContext(
            userQuery = "Simple query"
        )

        assertEquals("Simple query", context.userQuery)
        assertEquals(5, context.maxIterations) // Default value
        assertEquals(null, context.focusArea)
        assertEquals(null, context.currentDict)
    }

    @Test
    fun testDomainDictContextToString() {
        val context = DomainDictContext(
            userQuery = "A very long query that should be truncated in the toString representation",
            maxIterations = 3,
            focusArea = "auth"
        )

        val str = context.toString()
        assertTrue(str.contains("DomainDictContext"))
        assertTrue(str.contains("focusArea=auth"))
        assertTrue(str.contains("maxIterations=3"))
    }

    @Test
    fun testDomainEntryToCsvRow() {
        val entry = DomainEntry(
            chinese = "User Auth",
            codeTranslation = "UserAuth | Authentication",
            description = "User auth feature"
        )

        val csvRow = entry.toCsvRow()
        assertEquals("User Auth,UserAuth | Authentication,User auth feature", csvRow)
    }

    @Test
    fun testDomainEntryWithSpecialCharacters() {
        val entry = DomainEntry(
            chinese = "API Gateway",
            codeTranslation = "Gateway | API | GatewayService",
            description = "API endpoint (proxy) for services"
        )

        val csvRow = entry.toCsvRow()
        assertTrue(csvRow.contains("API Gateway"))
        assertTrue(csvRow.contains("Gateway | API | GatewayService"))
    }

    @Test
    fun testDictAssessmentCreation() {
        val assessment = DictAssessment(
            satisfiesRequirement = false,
            completenessScore = 0.6f,
            relevanceScore = 0.8f,
            gaps = listOf("Missing authentication terms", "Missing payment terms"),
            reasoning = "Current dictionary lacks auth-related vocabulary"
        )

        assertEquals(false, assessment.satisfiesRequirement)
        assertEquals(0.6f, assessment.completenessScore)
        assertEquals(0.8f, assessment.relevanceScore)
        assertEquals(2, assessment.gaps.size)
        assertTrue(assessment.gaps.contains("Missing authentication terms"))
    }

    @Test
    fun testDictAssessmentSatisfied() {
        val assessment = DictAssessment(
            satisfiesRequirement = true,
            completenessScore = 0.95f,
            relevanceScore = 0.92f,
            gaps = emptyList(),
            reasoning = "Dictionary covers all required terms"
        )

        assertTrue(assessment.satisfiesRequirement)
        assertTrue(assessment.completenessScore > 0.9f)
        assertTrue(assessment.gaps.isEmpty())
    }

    @Test
    fun testDictSuggestionTypes() {
        val suggestion = DictSuggestion(
            type = SuggestionType.QUERY_MORE_FILES,
            description = "Need to query authentication files",
            filesToQuery = listOf("src/**/*Auth*.kt", "src/**/*Login*.kt")
        )

        assertEquals(SuggestionType.QUERY_MORE_FILES, suggestion.type)
        assertEquals(2, suggestion.filesToQuery.size)
    }

    @Test
    fun testAllSuggestionTypes() {
        val types = SuggestionType.entries
        assertTrue(types.contains(SuggestionType.ADD_TERMS))
        assertTrue(types.contains(SuggestionType.REFINE_TRANSLATION))
        assertTrue(types.contains(SuggestionType.ADD_DESCRIPTION))
        assertTrue(types.contains(SuggestionType.QUERY_MORE_FILES))
        assertTrue(types.contains(SuggestionType.CLUSTER_ANALYSIS))
        assertTrue(types.contains(SuggestionType.COMPLETE))
    }

    @Test
    fun testDomainDictReviewResult() {
        val reviewResult = DomainDictReviewResult(
            iteration = 1,
            assessment = DictAssessment(
                satisfiesRequirement = false,
                completenessScore = 0.5f,
                relevanceScore = 0.7f,
                reasoning = "Needs more terms"
            ),
            suggestions = listOf(
                DictSuggestion(
                    type = SuggestionType.ADD_TERMS,
                    description = "Add authentication terms"
                )
            ),
            queriesNeeded = listOf("$.code.class(*Auth*)"),
            newEntries = listOf(
                DomainEntry("Auth", "Auth", "Authentication module")
            )
        )

        assertEquals(1, reviewResult.iteration)
        assertEquals(false, reviewResult.assessment.satisfiesRequirement)
        assertEquals(1, reviewResult.suggestions.size)
        assertEquals(1, reviewResult.queriesNeeded.size)
        assertEquals(1, reviewResult.newEntries.size)
    }

    @Test
    fun testDomainDictReviewResultWithNoChanges() {
        val reviewResult = DomainDictReviewResult(
            iteration = 3,
            assessment = DictAssessment(
                satisfiesRequirement = true,
                completenessScore = 0.95f,
                relevanceScore = 0.92f,
                reasoning = "Dictionary is complete"
            ),
            suggestions = listOf(
                DictSuggestion(
                    type = SuggestionType.COMPLETE,
                    description = "No more changes needed"
                )
            )
        )

        assertEquals(3, reviewResult.iteration)
        assertTrue(reviewResult.assessment.satisfiesRequirement)
        assertTrue(reviewResult.queriesNeeded.isEmpty())
        assertTrue(reviewResult.newEntries.isEmpty())
    }

    @Test
    fun testDomainDictAgentSchemaExampleUsage() {
        val example = DomainDictAgentSchema.getExampleUsage("domain-dict-agent")
        assertTrue(example.contains("/domain-dict-agent"))
        assertTrue(example.contains("userQuery"))
    }

    @Test
    fun testMultipleEntriesCsv() {
        val entries = listOf(
            DomainEntry("User", "User | UserEntity", "User entity"),
            DomainEntry("Auth", "Auth | Authentication", "Auth module"),
            DomainEntry("Payment", "Payment | PaymentService", "Payment processing")
        )

        val csvContent = entries.joinToString("\n") { it.toCsvRow() }

        assertTrue(csvContent.lines().size == 3)
        assertTrue(csvContent.contains("User,User | UserEntity,User entity"))
        assertTrue(csvContent.contains("Auth,Auth | Authentication,Auth module"))
        assertTrue(csvContent.contains("Payment,Payment | PaymentService,Payment processing"))
    }
}
