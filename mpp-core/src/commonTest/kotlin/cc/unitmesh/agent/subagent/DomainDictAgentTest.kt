package cc.unitmesh.agent.subagent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for DomainDictAgent data classes and Deep Research flow
 */
class DomainDictAgentTest {

    // ============= Context Tests =============

    @Test
    fun testDomainDictContextCreation() {
        val context = DomainDictContext(
            userQuery = "Add authentication related terms",
            maxIterations = 7,
            focusArea = "auth"
        )

        assertEquals("Add authentication related terms", context.userQuery)
        assertEquals(7, context.maxIterations)
        assertEquals("auth", context.focusArea)
    }

    @Test
    fun testDomainDictContextDefaults() {
        val context = DomainDictContext(
            userQuery = "Simple query"
        )

        assertEquals("Simple query", context.userQuery)
        assertEquals(7, context.maxIterations) // New default value for Deep Research
        assertEquals(null, context.focusArea)
        assertEquals(null, context.currentDict)
    }

    @Test
    fun testDomainDictContextToString() {
        val context = DomainDictContext(
            userQuery = "A very long query that should be truncated in the toString representation",
            maxIterations = 5,
            focusArea = "auth"
        )

        val str = context.toString()
        assertTrue(str.contains("DomainDictContext"))
        assertTrue(str.contains("focusArea=auth"))
        assertTrue(str.contains("maxIterations=5"))
    }

    // ============= Domain Entry Tests =============

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

    // ============= Deep Research Step 1: Problem Definition Tests =============

    @Test
    fun testProblemDefinitionCreation() {
        val problemDef = ProblemDefinition(
            goal = "Optimize domain dictionary for authentication module",
            scope = "Authentication and authorization related code",
            depth = "Comprehensive analysis",
            deliverableFormat = "CSV",
            constraints = listOf("Focus on security terms", "Exclude test files")
        )

        assertEquals("Optimize domain dictionary for authentication module", problemDef.goal)
        assertEquals(2, problemDef.constraints.size)
    }

    // ============= Deep Research Step 2: Research Dimensions Tests =============

    @Test
    fun testResearchDimensionCreation() {
        val dimension = ResearchDimension(
            name = "Core Domain",
            description = "Main business logic entities",
            priority = 5,
            queries = listOf("*Service*.kt", "*Repository*.kt")
        )

        assertEquals("Core Domain", dimension.name)
        assertEquals(5, dimension.priority)
        assertEquals(2, dimension.queries.size)
    }

    @Test
    fun testResearchDimensionPriority() {
        val dimensions = listOf(
            ResearchDimension("Core", "Core domain", 5),
            ResearchDimension("Infra", "Infrastructure", 3),
            ResearchDimension("API", "API layer", 4)
        )

        val sorted = dimensions.sortedByDescending { it.priority }
        assertEquals("Core", sorted[0].name)
        assertEquals("API", sorted[1].name)
        assertEquals("Infra", sorted[2].name)
    }

    // ============= Deep Research Step 3: Information Plan Tests =============

    @Test
    fun testInformationPlanCreation() {
        val plan = InformationPlan(
            searchPaths = listOf("src/main", "src/commonMain"),
            filePatterns = listOf("*Agent*.kt", "*Service*.kt"),
            knowledgeSources = listOf("source code", "README"),
            analysisStrategies = listOf("class analysis", "function analysis")
        )

        assertEquals(2, plan.searchPaths.size)
        assertEquals(2, plan.filePatterns.size)
        assertEquals(2, plan.analysisStrategies.size)
    }

    // ============= Deep Research Step 4: Dimension Result Tests =============

    @Test
    fun testDimensionResearchResult() {
        val result = DimensionResearchResult(
            dimension = "Core Domain",
            collected = listOf("class:UserService", "fun:authenticate"),
            organized = mapOf(
                "classes" to listOf("UserService", "AuthService"),
                "functions" to listOf("authenticate", "authorize")
            ),
            validated = true,
            conflicts = emptyList(),
            conclusion = "Found 2 classes and 2 functions",
            newEntries = listOf(
                DomainEntry("Auth", "AuthService", "Authentication service")
            )
        )

        assertEquals("Core Domain", result.dimension)
        assertTrue(result.validated)
        assertEquals(1, result.newEntries.size)
        assertEquals(2, result.collected.size)
    }

    // ============= Deep Research Step 5: Second-Order Insights Tests =============

    @Test
    fun testSecondOrderInsights() {
        val insights = SecondOrderInsights(
            principles = listOf("Domain terms reflect business concepts"),
            patterns = listOf("*Service suffix for business logic"),
            frameworks = listOf("Clean Architecture"),
            unifiedModel = "Domain vocabulary mirrors the ubiquitous language"
        )

        assertEquals(1, insights.principles.size)
        assertEquals(1, insights.patterns.size)
        assertTrue(insights.unifiedModel.contains("ubiquitous"))
    }

    // ============= Deep Research Step 6: Research Narrative Tests =============

    @Test
    fun testResearchNarrative() {
        val narrative = ResearchNarrative(
            summary = "Comprehensive analysis of domain vocabulary",
            keyFindings = listOf("Found 50 domain terms", "Identified 3 modules"),
            implications = listOf("Need to add authentication terms"),
            recommendations = listOf("Review generated entries", "Add more payment terms")
        )

        assertEquals(2, narrative.keyFindings.size)
        assertEquals(2, narrative.recommendations.size)
    }

    // ============= Deep Research Step 7: Final Deliverables Tests =============

    @Test
    fun testFinalDeliverables() {
        val deliverables = FinalDeliverables(
            updatedDictionary = "Chinese,Code Translation,Description\nAuth,AuthService,Auth module",
            changeLog = listOf("Added: Auth -> AuthService"),
            qualityMetrics = mapOf("completeness" to 0.85f, "relevance" to 0.9f),
            nextSteps = listOf("Review dictionary", "Test enhancement")
        )

        assertTrue(deliverables.updatedDictionary.contains("Chinese,Code Translation"))
        assertEquals(1, deliverables.changeLog.size)
        assertEquals(0.85f, deliverables.qualityMetrics["completeness"])
    }

    // ============= Deep Research State Tests =============

    @Test
    fun testDeepResearchStateInitial() {
        val state = DeepResearchState()

        assertEquals(0, state.step)
        assertEquals("", state.stepName)
        assertEquals(false, state.isComplete)
        assertTrue(state.dimensions.isEmpty())
    }

    @Test
    fun testDeepResearchStateProgression() {
        var state = DeepResearchState()

        // Step 1
        state = state.copy(
            step = 1,
            stepName = "Clarify",
            problemDefinition = ProblemDefinition("goal", "scope", "depth", "format")
        )
        assertEquals(1, state.step)
        assertNotNull(state.problemDefinition)

        // Step 2
        state = state.copy(
            step = 2,
            stepName = "Decompose",
            dimensions = listOf(
                ResearchDimension("Dim1", "desc", 5),
                ResearchDimension("Dim2", "desc", 3)
            )
        )
        assertEquals(2, state.step)
        assertEquals(2, state.dimensions.size)

        // Complete
        state = state.copy(
            step = 7,
            stepName = "Actionization",
            isComplete = true
        )
        assertTrue(state.isComplete)
    }

    // ============= Legacy Compatibility Tests =============

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
    }

    @Test
    fun testSuggestionTypes() {
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

    // ============= Schema Tests =============

    @Test
    fun testDomainDictAgentSchemaExampleUsage() {
        val example = DomainDictAgentSchema.getExampleUsage("domain-dict-agent")
        assertTrue(example.contains("/domain-dict-agent"))
        assertTrue(example.contains("userQuery"))
    }

    // ============= CSV Generation Tests =============

    @Test
    fun testMultipleEntriesCsv() {
        val entries = listOf(
            DomainEntry("User", "User | UserEntity", "User entity"),
            DomainEntry("Auth", "Auth | Authentication", "Auth module"),
            DomainEntry("Payment", "Payment | PaymentService", "Payment processing")
        )

        val csvContent = entries.joinToString("\n") { it.toCsvRow() }

        assertEquals(3, csvContent.lines().size)
        assertTrue(csvContent.contains("User,User | UserEntity,User entity"))
        assertTrue(csvContent.contains("Auth,Auth | Authentication,Auth module"))
        assertTrue(csvContent.contains("Payment,Payment | PaymentService,Payment processing"))
    }

    @Test
    fun testCsvWithHeader() {
        val header = "Chinese,Code Translation,Description"
        val entries = listOf(
            DomainEntry("Agent", "Agent | CodingAgent", "AI coding agent")
        )

        val fullCsv = listOf(header) + entries.map { it.toCsvRow() }
        val csvContent = fullCsv.joinToString("\n")

        assertTrue(csvContent.startsWith("Chinese,Code Translation,Description"))
        assertTrue(csvContent.contains("Agent,Agent | CodingAgent,AI coding agent"))
    }
}
