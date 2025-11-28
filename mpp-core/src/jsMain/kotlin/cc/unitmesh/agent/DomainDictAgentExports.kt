package cc.unitmesh.agent

import cc.unitmesh.agent.subagent.DomainDictAgent
import cc.unitmesh.agent.subagent.DomainDictContext
import cc.unitmesh.devins.filesystem.JsFileSystemFactory
import cc.unitmesh.indexer.DomainDictService
import cc.unitmesh.llm.JsKoogLLMService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * JS-friendly version of DomainDictContext
 */
@JsExport
data class JsDomainDictContext(
    val userQuery: String,
    val currentDict: String? = null,
    val maxIterations: Int = 7,
    val focusArea: String? = null
) {
    fun toCommon(): DomainDictContext {
        return DomainDictContext(
            userQuery = userQuery,
            currentDict = currentDict,
            maxIterations = maxIterations,
            focusArea = focusArea
        )
    }
}

/**
 * JS-friendly version of DeepResearchResult
 */
@JsExport
data class JsDeepResearchResult(
    val success: Boolean,
    val message: String,
    val steps: Int,
    val newEntries: Int,
    val report: String
)

/**
 * JS-friendly Domain Dictionary Deep Research Agent for CLI usage
 * 
 * This agent implements a 7-step Deep Research methodology:
 * 1. Clarify - Problem Definition
 * 2. Decompose - Research Dimensions
 * 3. Information Map - Planning
 * 4. Iterative Deep Research Loop
 * 5. Second-Order Insights
 * 6. Synthesis - Research Narrative
 * 7. Actionization - Final Deliverables
 */
@JsExport
class JsDomainDictAgent(
    private val projectPath: String,
    private val llmService: JsKoogLLMService
) {
    private val fileSystem = JsFileSystemFactory.createFileSystem(projectPath)
    private val domainDictService = DomainDictService(fileSystem)
    
    private val agent: DomainDictAgent = DomainDictAgent(
        llmService = llmService.service,
        fileSystem = fileSystem,
        domainDictService = domainDictService,
        maxDefaultIterations = 7
    )

    /**
     * Execute deep research for domain dictionary optimization
     * 
     * @param userQuery The user's requirement or question about domain vocabulary
     * @param focusArea Optional focus area (e.g., "auth", "payment", "agent")
     * @param maxIterations Maximum research iterations (default: 7)
     * @param onProgress Callback for progress updates
     * @return Promise with research result
     */
    @JsName("executeDeepResearch")
    fun executeDeepResearch(
        userQuery: String,
        focusArea: String? = null,
        maxIterations: Int = 7,
        onProgress: ((String) -> Unit)? = null
    ): Promise<JsDeepResearchResult> {
        return GlobalScope.promise {
            val context = DomainDictContext(
                userQuery = userQuery,
                focusArea = focusArea,
                maxIterations = maxIterations
            )

            val progressCallback = onProgress ?: { msg: String -> 
                console.log(msg) 
            }

            val result = agent.execute(context, progressCallback)

            JsDeepResearchResult(
                success = result.success,
                message = if (result.success) "Deep research completed successfully" else result.content,
                steps = result.metadata["steps"]?.toIntOrNull() ?: 7,
                newEntries = result.metadata["newEntries"]?.toIntOrNull() ?: 0,
                report = result.content
            )
        }
    }

    /**
     * Quick research mode - faster but less thorough
     * Uses only 3 iterations and focuses on immediate improvements
     */
    @JsName("executeQuickResearch")
    fun executeQuickResearch(
        userQuery: String,
        focusArea: String? = null,
        onProgress: ((String) -> Unit)? = null
    ): Promise<JsDeepResearchResult> {
        return executeDeepResearch(
            userQuery = userQuery,
            focusArea = focusArea,
            maxIterations = 3,
            onProgress = onProgress
        )
    }

    /**
     * Check if domain dictionary exists
     */
    @JsName("dictionaryExists")
    fun dictionaryExists(): Promise<Boolean> {
        return GlobalScope.promise {
            domainDictService.loadContent() != null
        }
    }

    /**
     * Get current dictionary content
     */
    @JsName("getDictionaryContent")
    fun getDictionaryContent(): Promise<String?> {
        return GlobalScope.promise {
            domainDictService.loadContent()
        }
    }

    /**
     * Get agent state summary
     */
    @JsName("getStateSummary")
    fun getStateSummary(): dynamic {
        val summary = agent.getStateSummary()
        return js("({})").also { obj ->
            summary.forEach { (key, value) ->
                obj[key] = value
            }
        }
    }

    /**
     * Ask a question about the completed research
     */
    @JsName("askQuestion")
    fun askQuestion(question: String): Promise<String> {
        return GlobalScope.promise {
            val result = agent.handleQuestion(question, emptyMap())
            result.content
        }
    }

    companion object {
        /**
         * Create a new DomainDictAgent instance
         */
        @JsName("create")
        fun create(
            projectPath: String,
            llmService: JsKoogLLMService
        ): JsDomainDictAgent {
            return JsDomainDictAgent(projectPath, llmService)
        }
    }
}

